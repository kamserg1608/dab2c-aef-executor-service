package ru.sbrf.dab2c.executor.domain.voice

/**
 * Управление скоростью ответа модели.
 */
@Suppress("MagicNumber")
enum class Speed(
    val value: Int
) {
    /** не выбрано. */
    SPEED_UNSPECIFIED(0),
    /** очень медленно. */
    EXTRA_SLOW(1),
    /** медленно. */
    SLOW(2),
    /** средний темп. */
    MEDIUM(3),
    /** быстро. */
    FAST(4),
    /** очень быстро. */
    EXTRA_FAST(5)
}
