plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
    id("com.google.devtools.ksp")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(libs.findLibrary("konvert-api").get())
    add("ksp", libs.findLibrary("konvert").get())
}

ksp {
    arg("konvert.enforce-not-null", "true")
}
