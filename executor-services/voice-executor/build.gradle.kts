plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-libraries:monitoring-service"))
    api(project(":executor-libraries:audit-service"))
    api(project(":executor-clients:ivr-voice"))
    api(project(":executor-clients:giga-voice"))
    api(project(":executor-clients:giga-agent"))
    api(project(":executor-clients:efs-adapter"))
    api(project(":executor-clients:kap-producer"))
    api(project(":executor-libraries:logging"))
    implementation(libs.grpc.server.spring.boot.starter)
    implementation(libs.grpc.client.spring.boot.starter)
    implementation(libs.grpc.services)
    implementation(libs.kotlin.logging.jvm)
}
