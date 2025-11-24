pluginManagement {
    repositories {
        maven {
            credentials {
                username = System.getProperty("gradle.wrapperUser")
                password = System.getProperty("gradle.wrapperPassword")
            }
            url = uri("https://nexus-ci.delta.sbrf.ru/repository/public/")
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
