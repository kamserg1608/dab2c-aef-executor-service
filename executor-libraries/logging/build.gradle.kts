plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Structured logging utilities"

dependencies {
    api(libs.kotlin.logging.jvm)
    api(libs.kotlinx.coroutines.slf4j)
}
