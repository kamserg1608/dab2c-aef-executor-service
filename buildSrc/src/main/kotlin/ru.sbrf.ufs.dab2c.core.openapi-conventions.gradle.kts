plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
    id("org.openapi.generator")
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/openapi/src/main/kotlin")
        }
    }
}
