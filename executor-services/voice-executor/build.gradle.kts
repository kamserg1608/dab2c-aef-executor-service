plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-libraries:context"))
    api(project(":executor-libraries:monitoring"))
    api(project(":executor-libraries:audit"))
    api(project(":executor-clients:giga-voice"))
    api(project(":executor-clients:giga-agent"))
    api(project(":executor-clients:efs-adapter"))
    api(project(":executor-clients:kap-producer"))
    api(project(":executor-clients:configurator"))
    api(project(":executor-clients:iag"))
    api(project(":executor-libraries:logging"))
    api(project(":executor-libraries:time"))
    implementation(project(":executor-libraries:jackson"))
    implementation(libs.grpc.server.spring.boot.starter)
    implementation(libs.grpc.client.spring.boot.starter)
    implementation(libs.grpc.services)
    implementation(libs.kotlin.logging.jvm)
    testImplementation(project(":executor-libraries:test-support"))
}
