def onDistrib(app, distr) {

    def skipTests = env.SKIP_TEST?.toBoolean() ? " -x test " : ""
    def version = "-Pdistrib.version=${distr.version}"
    gradlew("clean build", "${version} ${skipTests}")

    distr.addConf("./dab2c-core-integration-service-configs/build/resources/main/distr/*")

    distr.addBH("./dab2c-core-integration-service-application/build/zero-compressed-main-jar/*.jar", "SYBSYSTEM_CODE_LOWER_CASE_PLACEHOLDER-main")
    distr.addBH("./dab2c-core-integration-service-application/build/zero-compressed-dependencies-jars/*.jar", "SYBSYSTEM_CODE_LOWER_CASE_PLACEHOLDER-dependencies")

    distr.addDB("./dab2c-core-migrations/build/libs/db_archive.zip")
}

void gradlew(String task, String options) {
    withPreparedEnv("openjdk-21") {
        withCredentials([usernamePassword(
                credentialsId: 'nexus_ci_cred',
                passwordVariable: 'wrappedPassword',
                usernameVariable: 'wrappedUser')]) {
            String wrapperOptions = getWrapperOptions(wrappedUser, wrappedPassword)
            sh "./gradlew ${wrapperOptions} ${task} ${options}"
        }
    }
}

void withPreparedEnv(String jdk, Closure closure) {
    withEnv([
            "JAVA_HOME=${tool name: jdk, type: 'jdk'}",
    ]) {
        closure.call()
    }
}

String getWrapperOptions(String wrappedUser, String wrappedPassword) {
    return [
            "-Dgradle.wrapperUser=${wrappedUser}",
            "-Dgradle.wrapperPassword='${wrappedPassword}'",
            "-Dgradle.wrappedUser=${wrappedUser}",
            "-Dgradle.wrappedPassword='${wrappedPassword}'",
    ].join(" ")
}

return wrapJenkinsfile(this)
