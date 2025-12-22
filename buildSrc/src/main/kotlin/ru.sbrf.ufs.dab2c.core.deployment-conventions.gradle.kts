plugins {
    id("org.springframework.boot")
    id("ru.sbrf.ufs.dab2c.core.process-resources-conventions")
}

val excludedSignatures = listOf(
    "**/*.SF",
    "**/*.DSA",
    "**/*.RSA",
    "**/META-INF/*.SF",
    "**/META-INF/*.DSA",
    "**/META-INF/*.RSA",
    "**/META-INF/*.EC",
    "**/META-INF/SIG-*"
)

tasks {
    named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
        enabled = false
        exclude(excludedSignatures)
    }

    named<Jar>("jar") {
        enabled = true
        exclude(excludedSignatures)
    }
}

afterEvaluate {
    apply(plugin = "ru.sbrf.ufs.zero-compress-plugin")

    the<ru.sbrf.ufs.platform.plugin.dto.ZeroCompressExtension>().apply {
        enabled = project.findProperty("build.distr")?.toString()?.toBoolean() ?: false
    }
}
