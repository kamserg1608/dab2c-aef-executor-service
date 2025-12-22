plugins {
    `java-library`
    id("com.github.spotbugs")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}

spotbugs {
    toolVersion.set("4.9.3")
    // TODO После полной миграции на Gradle отдельной задачей починить все проблемы SpotBugs
    ignoreFailures.set(true)
    excludeFilter.set(file("${project.rootDir}/config/spotbugs/excludeFilters.xml"))
}

dependencies {
    spotbugs("com.github.spotbugs:spotbugs:4.9.3")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.3")
}
