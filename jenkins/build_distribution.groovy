def onDistrib(app, distr) {

    def skipTests = env.SKIP_TEST?.toBoolean() ? " -x test " : ""
    def skipStaticChecks =  env.SKIP_STATIC_CHECKS?.toBoolean()?:false
    gradlew("clean build", skipTests)

    /*
     * Что здесь происходит:
     * - мы вычитываем deployment unit-s из свойств Maven-проекта
     * - при формировании bh-компонента дистрибутива создаём подпапку с именем deploymentUnit, чтобы скопировать джарки именно из этой папки.
     * Так хитрить приходится именно потому, что из BonJour EFS генерируется проект, и жёстко DU прописывать нельзя.
     *
     * Проекты у себя могут внести следующие изменения:
     * - указать вместо *-application-* свой модуль.
     * - вместо deploymentUnit* указать конкретную строку, а mavenwEvaluate() удалить.
     *
     * Должно соблюдаться правило: если количество deployment == N, то должно быть вызвано 2N distr.addBH()
     */
    String deploymentUnit1 = "router-executor"
    distr.addConf("./executor-application/configs/build/resources/main/*")
    distr.addBH("./executor-application/deployments/${deploymentUnit1}/build/zero-compressed-main-jar/*.jar", "${deploymentUnit1}-main")
    distr.addBH("./executor-application/deployments/${deploymentUnit1}/build/zero-compressed-dependencies-jars/*.jar", "${deploymentUnit1}-dependencies")
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
