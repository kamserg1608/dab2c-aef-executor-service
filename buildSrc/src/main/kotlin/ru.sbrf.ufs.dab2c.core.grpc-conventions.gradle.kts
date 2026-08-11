import com.google.protobuf.gradle.id

plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
    id("com.google.protobuf")
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    api(libs.findBundle("grpc").get())
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.findVersion("protobuf").get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.findVersion("grpc").get()}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.findVersion("grpc-kotlin").get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc")
                id("grpckt")
            }
            task.builtins {
                id("kotlin")
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
    }
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
