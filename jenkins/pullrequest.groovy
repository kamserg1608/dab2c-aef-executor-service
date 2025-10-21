pipeline {
    agent {
        label 'clearAgent&&!static'
    }

    options {
        timestamps()
    }

    stages {
        stage("Полная проверка") {
            steps {
                check()
            }
        }
    }

    post {
        failure {
            notifyAboutFailConditionally()
        }
    }
}

/**
 * Запускает проверки Maven со следующими опциями:
 * <ul>
 *     <li>settings.xml с актуальными Nexus-репозиториями и кредами ТУЗа BonJour EFS.</li>
 *     <li>все проверки включены.</li>
 *     <li>актуальная для контекста запуска ветка (для пулл-реквеста — ветка с изменениями, для регулярных прогонов — сама ветка, по которой ведётся прогон).</li>
 *     <li>рандомный порт для работы Spring Boot (во избежание конфликтов при прогоне параллельных ПРов).</li>
 *     <li>явное включение профиля Spring Boot для прогона тестов на стабе.</li>
 *     <li>активация профиля для включения СУПового плагина.</li>
 *     <li>активация профиля для работы переупаковщика jar-файло</li>
 * </ul>
 */
void check() {
    mavenw("clean install sonar:sonar", getBuildOptions(), "openjdk-21")
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
        credentialsId: 'cab-sa-dvo08125',
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

String getBuildOptions() {
    return "-Dskip.all.checks=false ${getSonarOptions()} ${getLocalServerOptions()} -Dbuild-config -Dbuild-distr -Dmaven.build.cache.enabled=false"
}

String getSonarOptions() {
    if (env.CHANGE_ID) {
        return ["-Dsonar.pullrequest.key=${env.CHANGE_ID}",
                "-Dsonar.pullrequest.branch=${env.CHANGE_BRANCH}",
                "-Dsonar.pullrequest.base=${env.CHANGE_TARGET}",
        ].join(" ")
    } else {
        return "-Dsonar.branch.name=${env.BRANCH_NAME}"
    }
}

String getLocalServerOptions() {
    def random = new Random()
    String portSuffix = sprintf("%03d", random.nextInt(1000))
    return [
        "-Dserver.port=8${portSuffix}",
        "-Dkafka.port=29${portSuffix}"
    ].join(" ")
}

def notifyAboutFailConditionally() {
    if (!env.CHANGE_ID) {
        emailext(to: 'DAlekZhuravlev@sberbank.ru',
                subject: 'Падение ПР сборки',
                body: "Упала сборка. См. ${env.BUILD_URL}"
        )
    }
}
