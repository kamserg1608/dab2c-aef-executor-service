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
        // Формат: metric_name{tag1="val1",tag2="val2"} value
        if (!line.contains(' ')) return null

        val lastSpaceIndex = line.lastIndexOf(' ')
        if (lastSpaceIndex == -1) return null

        val valueStr = line.substring(lastSpaceIndex + 1).trim()
        val value = valueStr.toDoubleOrNull() ?: return null

        val nameAndTags = line.substring(0, lastSpaceIndex).trim()

        if (!nameAndTags.contains('{')) {
            // Метрика без тегов: metric_name value
            return MetricLine(nameAndTags, emptyMap(), value)
        }

        val openBrace = nameAndTags.indexOf('{')
        val closeBrace = nameAndTags.indexOf('}')

        if (closeBrace == -1 || openBrace == -1) return null

        val name = nameAndTags.substring(0, openBrace)
        val tagsContent = nameAndTags.substring(openBrace + 1, closeBrace)

        val tags = if (tagsContent.isBlank()) {
            emptyMap()
        } else {
            tagsContent.split(",").associate { tagPart ->
                val eqIndex = tagPart.indexOf('=')
                if (eqIndex == -1) return@associate "" to ""

                val key = tagPart.substring(0, eqIndex).trim()
                val rawValue = tagPart.substring(eqIndex + 1).trim()
                val valueClean = rawValue.removeSurrounding("\"")
                key to valueClean
            }
        }

        return MetricLine(name, tags, value)
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
