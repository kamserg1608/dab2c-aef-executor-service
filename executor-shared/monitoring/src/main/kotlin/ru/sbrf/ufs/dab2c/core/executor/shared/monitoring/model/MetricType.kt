package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model

/** Metric type to be reported. */
enum class MetricType(
    /**
     * Metric title.
     */
    val title: String
) {

    /** Successful operation event type. */
    SUCCESS("Успешные обращения к сервису"),

    /** Successful operation metric type (milliseconds). */
    SUCCESS_TIME("Неуспешные обращения к сервису"),

    /** Unsuccessful operation event type. */
    ERROR("Время успешных обращений к сервису"),

    /** Unsuccessful operation metric type (milliseconds). */
    ERROR_TIME("Время неуспешных обращений к сервису"),

    /** Value event type. */
    VALUE("Некоторое бизнес-значение")
}
