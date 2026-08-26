pipeline {
    agent {
        label 'clearAgent&&rhel8&&!static'
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
    sonar(env.CHANGE_BRANCH)
}

void sonar(String appBranch) {
    if (!appBranch?.trim()) {
        error("env.CHANGE_BRANCH is not set for pull request analysis")
    }

    gradlew("jacocoAggregateReport", "")
    verifyJacocoReport()

    def baseCommit = sh(
            script: 'git rev-parse HEAD',
            returnStdout: true
    ).trim()

    withEnv([
            "SONAR_BRANCH_NAME=${appBranch}",
            "SONAR_SCM_REVISION=${baseCommit}",
    ]) {
        withCredentials([string(
                credentialsId: 'sonar_token_executor-java:master_447002',
                variable: 'sonarToken'
        )]) {
            gradlew("sonar", getSonarOptions())
        }
    }
}

void verifyJacocoReport() {
    sh '''
        REPORT="build/reports/jacoco/aggregate/jacoco.xml"

        test -s "$REPORT" || {
            echo "JaCoCo aggregate XML was not created or is empty: $REPORT"
            exit 1
        }

        echo "JaCoCo aggregate XML:"
        ls -lh "$REPORT"
        grep -o '<counter type="LINE"[^>]*/>' "$REPORT" | tail -1 || true
    '''
}

void gradlew(String task, String options) {
    withPreparedEnv("sberjdk-21") {
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

String getSonarOptions() {
    return [
            '-Dsonar.login="$sonarToken"',
            '-Dsonar.branch.name="$SONAR_BRANCH_NAME"',
            "-Dsonar.projectName=executor-java",
            "-Dsonar.projectKey=executor-java",
            '-Dsonar.scm.revision="$SONAR_SCM_REVISION"',
            "-Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/aggregate/jacoco.xml",
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
