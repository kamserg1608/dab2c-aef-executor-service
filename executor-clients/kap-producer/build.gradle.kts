plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    implementation(libs.spring.kafka)
}
