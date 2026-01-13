plugins {
    `java-library`
    id("com.github.spotbugs")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}

spotbugs {
    toolVersion.set(libs.findVersion("spotbugs-tool").get().toString())
    ignoreFailures.set(true)
    excludeFilter.set(file("${project.rootDir}/config/spotbugs/excludeFilters.xml"))
}

dependencies {
    spotbugs(libs.findLibrary("spotbugs").get())
    compileOnly(libs.findLibrary("spotbugs-annotations").get())
}
