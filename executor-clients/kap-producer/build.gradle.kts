plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-libraries:monitoring"))
    api(project(":executor-libraries:context"))
    implementation(project(":executor-libraries:common"))
    implementation(project(":executor-libraries:jackson"))
    implementation(libs.spring.kafka)
    testImplementation(project(":executor-libraries:test-support"))
}
