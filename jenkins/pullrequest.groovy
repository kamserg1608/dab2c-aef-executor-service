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

void check() {
    gradlew("clean build", "")
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

def notifyAboutFailConditionally() {
    if (!env.CHANGE_ID) {
        emailext(to: 'DAlekZhuravlev@sberbank.ru',
                subject: 'Падение ПР сборки',
                body: "Упала сборка. См. ${env.BUILD_URL}"
        )
    }
}
