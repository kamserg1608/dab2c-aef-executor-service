plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Shared Jackson ObjectMapper configuration"

dependencies {
    api(libs.jackson.module.kotlin)
    api(libs.jackson.datatype.jsr310)
}
