package ru.sbrf.dab2c.executor.it.support.metrics

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Утилитный класс для парсинга формата Prometheus (text/plain).
 * Предназначен для использования в интеграционных тестах.
 */
internal class PrometheusMetricsParser(private val rawBody: String) {

    private val logger = KotlinLogging.logger { }

    data class MetricLine(
        val name: String,
        val tags: Map<String, String>,
        val value: Double
    )

    private val parsedLines: List<MetricLine> by lazy { parseAll() }

    private fun parseAll(): List<MetricLine> {
        return rawBody.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { parseLine(it) }
            .toList()
    }

    private fun parseLine(line: String): MetricLine? {
        return line.lastIndexOf(' ').takeIf { it != -1 }?.let { lastSpaceIndex ->
            val value = line.substring(lastSpaceIndex + 1).trim().toDoubleOrNull()
            val nameAndTags = line.substring(0, lastSpaceIndex).trim()

            value?.let { v ->
                parseMetricNameAndTags(nameAndTags)?.let { (name, tags) ->
                    MetricLine(name, tags, v)
                }
            }
        }
    }

    private fun parseMetricNameAndTags(nameAndTags: String): Pair<String, Map<String, String>>? {
        return if (!nameAndTags.contains('{')) {
            nameAndTags to emptyMap()
        } else {
            val openBrace = nameAndTags.indexOf('{')
            val closeBrace = nameAndTags.indexOf('}')
            if (openBrace != -1 && closeBrace != -1) {
                val name = nameAndTags.substring(0, openBrace)
                val tagsContent = nameAndTags.substring(openBrace + 1, closeBrace)
                name to parseTags(tagsContent)
            } else {
                null
            }
        }
    }

    private fun parseTags(tagsContent: String): Map<String, String> {
        return if (tagsContent.isBlank()) {
            emptyMap()
        } else {
            tagsContent.split(",").associate { tagPart ->
                val eqIndex = tagPart.indexOf('=')
                if (eqIndex == -1) {
                    "" to ""
                } else {
                    val key = tagPart.substring(0, eqIndex).trim()
                    val rawValue = tagPart.substring(eqIndex + 1).trim()
                    key to rawValue.removeSurrounding("\"")
                }
            }
        }
    }

    fun findMetric(
        name: String,
        requiredTags: Map<String, String> = emptyMap()
    ): MetricLine? {
        return parsedLines.find { metric ->
            metric.name == name && containsAllTags(metric.tags, requiredTags)
        }
    }

    fun findAllMetricsByName(name: String): List<MetricLine> {
        return parsedLines.filter { it.name == name }
    }

    private fun containsAllTags(actualTags: Map<String, String>, expectedTags: Map<String, String>): Boolean {
        return expectedTags.all { (key, value) -> actualTags[key] == value }
    }

    fun debugPrint() {
        logger.debug { "Parsed ${parsedLines.size} metric lines from response." }
    }
}
