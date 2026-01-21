plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-clients:ivr-voice"))
    api(project(":executor-clients:giga-voice"))
    api(project(":executor-clients:giga-agent"))
    api(project(":executor-clients:efs-adapter"))
    api(project(":executor-clients:kap-producer"))
    implementation(libs.grpc.server.spring.boot.starter)
    implementation(libs.grpc.client.spring.boot.starter)
    implementation(libs.grpc.services)
    implementation(libs.kotlin.logging.jvm)
}
