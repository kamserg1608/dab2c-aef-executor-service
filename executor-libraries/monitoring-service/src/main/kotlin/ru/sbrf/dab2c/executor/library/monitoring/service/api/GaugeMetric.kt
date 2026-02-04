package ru.sbrf.dab2c.executor.library.monitoring.service.api

fun interface GaugeMetric {
    fun set(value: Double)
}
