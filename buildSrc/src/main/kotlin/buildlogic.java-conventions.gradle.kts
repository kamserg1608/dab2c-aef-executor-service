plugins {
    java
    `java-library`
    `maven-publish`
}

repositories {
    val nexusUsername = System.getProperty("gradle.wrapperUser")
    val nexusPassword = System.getProperty("gradle.wrapperPassword")
    val protectedRepo = { repoUrl: String ->
        maven {
            credentials {
                username = nexusUsername
                password = nexusPassword
            }
            url = uri(repoUrl)
        }
    }

    mavenLocal()
    protectedRepo("https://nexus-ci.delta.sbrf.ru/repository/public/")
    protectedRepo("https://nexus-ci.delta.sbrf.ru/repository/maven-lib-int/")
    protectedRepo("https://nexus-ci.delta.sbrf.ru/repository/maven-lib-release/")
}

group = "ru.sbrf.ufs.dab2c.core.executor"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}
