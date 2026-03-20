package ru.sbrf.dab2c.executor.clients.efs.adapter.api

/**
 * EFS configuration parameter with typed accessors.
 */
data class Parameter(val name: String, val value: String?) {

    /** Raw string value. */
    fun getString(default: () -> String = { noValue() }): String =
        value ?: default()

    /** Boolean value. */
    fun getBool(default: () -> Boolean = { noValue() }): Boolean =
        value?.toBoolean() ?: default()

    /** Long value. */
    fun getLong(default: () -> Long = { noValue() }): Long =
        value?.toLong() ?: default()

    /** Double value. */
    fun getDouble(default: () -> Double = { noValue() }): Double =
        value?.toDouble() ?: default()

    private fun noValue(): Nothing =
        error("Parameter '$name': value not present and no default provided")
}
