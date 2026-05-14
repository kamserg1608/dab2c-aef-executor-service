package ru.sbrf.dab2c.executor.library.testing.golden

import com.fasterxml.jackson.databind.ObjectMapper
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Serializes [actual] via [mapper] and compares the result tree-wise against a JSON file on
 * the test classpath. Whitespace and key-order insensitive: both sides are parsed and
 * compared via `JsonNode.equals`.
 *
 * Output is canonicalised through `JsonNode` before write/compare, so raw-value JSON
 * embedded by custom serializers (notably proto3 JSON produced by `ProtobufModule` via
 * `JsonFormat`) is re-emitted with consistent pretty-print formatting.
 *
 * When `UPDATE_GOLDENS=1` is set in the environment, the golden file is rewritten with
 * the pretty-printed serialization of [actual] instead of asserting. A loud warning is
 * printed to stderr so accidental check-ins of regenerated goldens are obvious in review.
 *
 * [goldenResource] is a classpath path relative to the test resources root, e.g.
 * `"golden/audit/settings-rq.json"`.
 */
fun assertMatchesGolden(
    actual: Any,
    goldenResource: String,
    mapper: ObjectMapper = ObjectMappers.MAPPER
) {
    val actualJson = canonicalJson(actual, mapper)

    if (System.getenv("UPDATE_GOLDENS") == "1") {
        writeGolden(goldenResource, actualJson)
        return
    }

    val expectedTree = readGoldenOrFail(goldenResource, actualJson, mapper)
    val actualTree = mapper.readTree(actualJson)

    if (expectedTree != actualTree) {
        throw AssertionError(formatGoldenMismatch(goldenResource, expectedTree, actualTree, mapper))
    }
}

private fun canonicalJson(actual: Any, mapper: ObjectMapper): String =
    mapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(mapper.readTree(mapper.writeValueAsString(actual)))

private fun writeGolden(goldenResource: String, content: String) {
    val target = resolveGoldenSourcePath(goldenResource)
    Files.createDirectories(target.parent)
    Files.writeString(target, content + "\n")
    System.err.println(
        "!!! GOLDEN UPDATED: $goldenResource -> $target — review before committing !!!"
    )
}

private fun readGoldenOrFail(goldenResource: String, actualJson: String, mapper: ObjectMapper) =
    Thread.currentThread().contextClassLoader.getResourceAsStream(goldenResource)
        ?.use { mapper.readTree(it) }
        ?: throw AssertionError(
            "Golden resource not found on test classpath: $goldenResource\n" +
                "Set UPDATE_GOLDENS=1 and re-run to create it.\n" +
                "Actual:\n$actualJson"
        )

private fun formatGoldenMismatch(
    goldenResource: String,
    expectedTree: com.fasterxml.jackson.databind.JsonNode,
    actualTree: com.fasterxml.jackson.databind.JsonNode,
    mapper: ObjectMapper
): String {
    val expectedPretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(expectedTree)
    val actualPretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actualTree)
    return "Golden mismatch: $goldenResource\n" +
        "--- expected ---\n$expectedPretty\n" +
        "--- actual ---\n$actualPretty\n" +
        "If the divergence is intentional, set UPDATE_GOLDENS=1 to regenerate."
}

/**
 * Recursively replaces values of [fields] anywhere in [payload] with [placeholder]
 * so non-deterministic content (UUIDs, wall-clock timestamps, random ports) doesn't
 * break golden-file comparison. Returns a new map; does not mutate [payload].
 *
 * - Field match is by exact key name at any depth.
 * - Recurses into nested `Map<String, Any?>` values.
 * - Recurses into `List<*>` values, preserving order; if list elements are maps they
 *   are masked element-wise.
 * - Non-map / non-list values that share a name with a masked field at the SAME level
 *   are replaced. Primitive lookups inside lists/non-map nested structures aren't
 *   touched (we don't try to invent field names for list elements that aren't maps).
 */
fun maskNonDeterministic(
    payload: Map<String, Any?>,
    fields: Set<String>,
    placeholder: String = "<MASKED>"
): Map<String, Any?> =
    payload.mapValues { (key, value) ->
        if (key in fields) placeholder else maskValue(value, fields, placeholder)
    }

@Suppress("UNCHECKED_CAST")
private fun maskValue(value: Any?, fields: Set<String>, placeholder: String): Any? = when (value) {
    is Map<*, *> -> maskNonDeterministic(value as Map<String, Any?>, fields, placeholder)
    is List<*> -> value.map { maskValue(it, fields, placeholder) }
    else -> value
}

private fun resolveGoldenSourcePath(goldenResource: String): Path {
    val classpath = Thread.currentThread().contextClassLoader.getResource(".")
        ?: error("Cannot resolve test classpath root for golden update")
    val moduleRoot = generateSequence(Paths.get(classpath.toURI())) { it.parent }
        .take(MAX_PARENT_WALK)
        .firstOrNull { Files.exists(it.resolve("build.gradle.kts")) }
        ?: error(
            "Cannot locate module root (looking for build.gradle.kts) for goldenResource=$goldenResource " +
                "starting from $classpath"
        )
    return moduleRoot.resolve("src/test/resources").resolve(goldenResource)
}

private const val MAX_PARENT_WALK = 10
