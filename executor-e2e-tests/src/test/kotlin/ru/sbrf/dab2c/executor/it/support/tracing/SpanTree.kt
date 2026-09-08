package ru.sbrf.dab2c.executor.it.support.tracing

import ru.sbrf.dab2c.executor.library.testing.tracing.ParsedSpan

/** The `voice_session` span of the call [voiceCallId]. */
fun List<ParsedSpan>.voiceSessionOf(voiceCallId: String): ParsedSpan =
    checkNotNull(
        firstOrNull {
            it.kind() == "voice_session" &&
                it.attributes["aef.settings"]?.contains("\"voiceCallId\":\"$voiceCallId\"") == true
        }
    ) { "Voice session span for $voiceCallId was not closed and exported" }

/** The downstream stream span carrying [voiceSession]. */
fun List<ParsedSpan>.downstreamOf(voiceSession: ParsedSpan): ParsedSpan =
    checkNotNull(firstOrNull { it.spanId == voiceSession.parentSpanId }) {
        "Downstream stream span was not closed and exported"
    }

/** [root] together with every span nested under it. */
fun List<ParsedSpan>.subtreeOf(root: ParsedSpan): List<ParsedSpan> {
    val byParent = groupBy { it.parentSpanId }
    val collected = mutableListOf(root)
    var frontier = listOf(root)
    while (frontier.isNotEmpty()) {
        frontier = frontier.flatMap { byParent[it.spanId].orEmpty() }
        collected += frontier
    }
    return collected
}

/**
 * Every span of the call [voiceCallId]. The tracing topic carries all concurrent calls in a single
 * trace, so assertions on a call must be scoped to its own tree.
 */
fun List<ParsedSpan>.callTreeOf(voiceCallId: String): List<ParsedSpan> =
    subtreeOf(rootOf(voiceSessionOf(voiceCallId)))

private fun List<ParsedSpan>.rootOf(span: ParsedSpan): ParsedSpan {
    var current = span
    while (true) {
        current = firstOrNull { it.spanId == current.parentSpanId } ?: return current
    }
}
