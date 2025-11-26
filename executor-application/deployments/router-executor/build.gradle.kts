plugins {
    id("ru.sbrf.ufs.dab2c.core.deployment-conventions")
}

description = "router-executor"

springBoot {
    mainClass.set("ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app.RouterExecutorApplicationEntryPointKt")
}

dependencies {
    implementation(project(":executor-shared:health-check"))
    implementation(project(":executor-shared:commons"))
    implementation(project(":executor-shared:monitoring"))
    implementation(project(":executor-shared:circuit-breaker"))
    implementation(project(":executor-shared:logging"))
    implementation(libs.sber.acl)

    testImplementation(project(":executor-shared:test-utils"))
}

if (project.property("build-distr")?.toString().toBoolean()) {

//    configurations.all {
//        exclude(group = "com.h2database", module = "h2")
//        exclude(group = "org.junit.platform")
//        exclude(group = "org.junit.jupiter")
//        exclude(group = "junit")
//    }

    tasks.register<Copy>("copyToLib") {
        from(configurations.runtimeClasspath) {
            include("*.jar")
        }
        into(layout.buildDirectory.dir("libs/dependencies"))
    }

    tasks.register<Exec>("recompressMain") {
        group = "build"
        commandLine("sh", "jar_compressor.sh")
        args(
            layout.buildDirectory.dir("libs").get(),
            layout.buildDirectory.dir("zero-compressed-main-jar").get()
        )
    }

    tasks.register<Exec>("recompressDeps") {
        group = "build"
        commandLine("sh", "jar_compressor.sh")
        args(
            layout.buildDirectory.dir("libs/dependencies").get(),
            layout.buildDirectory.dir("zero-compressed-dependencies-jars").get()
        )
    }

    tasks.named("recompressMain") {
        dependsOn(tasks.named("jar"))
    }
    tasks.named("recompressDeps") {
        dependsOn(tasks.named("copyToLib"))
    }
    tasks.named("build") {
        dependsOn(tasks.named("recompressMain"))
        dependsOn(tasks.named("recompressDeps"))
    }
}
