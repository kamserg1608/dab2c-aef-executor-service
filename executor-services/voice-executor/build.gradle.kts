plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(project(":executor-clients:giga-voice"))
    implementation(libs.grpc.server.spring.boot.starter)
    implementation(libs.grpc.client.spring.boot.starter)
}
