plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "MDC context propagation library for reactive applications with Kotlin coroutines"

dependencies {
    // Coroutine context bridges
    api(libs.kotlinx.coroutines.reactor)
    api(libs.kotlinx.coroutines.slf4j)

    // Micrometer context propagation (Reactor <-> ThreadLocal bridge)
    api(libs.micrometer.context.propagation)
}
