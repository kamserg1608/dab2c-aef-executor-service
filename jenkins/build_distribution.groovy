def onDistrib(app, distr) {

    def skipTests =  env.SKIP_TEST?.toBoolean()?:false
    def skipStaticChecks =  env.SKIP_STATIC_CHECKS?.toBoolean()?:false
    mavenw("clean verify sonar:sonar", getBuildOptions(app, distr, skipTests, skipStaticChecks), "openjdk-21")

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
    String deploymentUnit1 = mavenwEvaluate("deploymentUnit1", "openjdk-21")
    distr.addBH("./executor-application/deployments/router-executor/target/zero-compressed-main-jar/*.jar", "${deploymentUnit1}-main")
    distr.addBH("./executor-application/deployments/router-executor/target/zero-compressed-dependencies-jars/*.jar", "${deploymentUnit1}-dependencies")
    distr.addConf("./executor-application/deployments/router-executor/target/conf/*")
    distr.addConf("./executor-application/configs/target/classes/distr/*")
}

/**
 * @param goal Maven-цель для запуска
 * @param options Опции Maven
 * @param jdk Идентификатор JDK-инструмента в Jenkins
 */
void mavenw(String goal, String options, String jdk) {
    withPreparedEnv(jdk) {
        configFileProvider([configFile(
            fileId: 'dab2c_core_java_integration_service',
            variable: 'MAVEN_SETTINGS_XML')]) {
            sh "./mvnw ${goal} -s $MAVEN_SETTINGS_XML $options"
        }
    }
}

/**
 * Запуск блока кода closure с подготовленным окружением
 *
 * @param jdk Идентификатор JDK-инструмента в Jenkins
 */
void withPreparedEnv(String jdk, Closure closure) {
    withCredentials([usernamePassword(
//       credentialsId: 'cab-sa-dvo08816_AD_Path',
            credentialsId: 'aef_dab2c_ift_cab-sa-dvo08817_ad_domain',
            usernameVariable: 'wrappedUser', passwordVariable: 'wrappedPassword'
    )]) {
        withEnv([
                // Указываем версию Java
                "JAVA_HOME=${tool name: jdk, type: 'jdk'}",
                // Проставляем параметры для аутентификации Maven Wrapper в Nexus 3
                "MVNW_USERNAME=${wrappedUser}",
                "MVNW_PASSWORD=${wrappedPassword}"
        ]) {
            closure.call()
        }
    }
}

String mavenwEvaluate(String projectProperty, String jdk) {
    String result = null;
    withPreparedEnv(jdk) {
        configFileProvider([configFile(
            fileId: 'dab2c_core_java_integration_service',
            variable: 'MAVEN_SETTINGS_XML')]) {
            result = sh(returnStdout: true, script: "./mvnw help:evaluate -Dexpression=$projectProperty -q -DforceStdout -s $MAVEN_SETTINGS_XML")
        }
    }
    return result;
}

/**
 *
 Формирование опций запуска Maven
 - включаются все проверки статического анализа, в том числе, SonarQube.
 - для SonarQube передаётся ветка, по которой идёт сборка.
 - активируется профиль для работы СУПового плагина (build-config).
 - активируется профиль для работы переупаковщика jar-файлов.
 - подставляется актуальная переменная FP_VERSION, которая в дальнейшем инжектится в файлы окружения Spring.
 - включается стабовый профиль Spring Boot для тестов и генерируется случайный порт.
 */
String getBuildOptions(app, distr, skipTests, skipStaticChecks) {
    return [
            "-Dmaven.test.skip=$skipTests",
            "-Dskip.all.checks=$skipStaticChecks",
            "-Dsonar.branch.name=${app.branch}",
            "-Dbuild-config",
            "-Dbuild-distr",
            "-Dmaven.build.cache.enabled=false",
            "-DFP_VERSION=${distr.version}",
            "${getLocalServerOptions()}"
    ].join(" ")
}

String getLocalServerOptions() {
    def random = new Random()
    String portSuffix = sprintf("%03d", random.nextInt(1000))
    return [
            "-Dserver.port=8${portSuffix}",
            "-Dkafka.port=29${portSuffix}"
    ].join(" ")
}

return wrapJenkinsfile(this)
