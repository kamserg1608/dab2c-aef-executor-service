plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
    id("org.openapi.generator")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(libs.findBundle("jackson").get())
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/openapi/src/main/kotlin")
        }
    }
}