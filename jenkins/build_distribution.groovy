def onDistrib(app, distr) {
    build(distr)
    sonar(app)

    distr.addConf("./executor-distribution/build/resources/main/distr/*")

    distr.addBH("./executor-application/build/zero-compressed-main-jar/*.jar", "executor-main")
    distr.addBH("./executor-application/build/zero-compressed-dependencies-jars/*.jar", "executor-dependencies")
}

void build(distr) {
    def skipTests = env.SKIP_TEST?.toBoolean() ? " -x test " : ""
    def version = "-Pdistrib.version=${distr.version}"
    gradlew("clean build", "${version} ${skipTests}")
}

void sonar(app) {
    def appBranch = app.branch
    if (!appBranch?.trim()) {
        error("app.branch is not set for Sonar analysis")
    }

    gradlew("jacocoAggregateReport", "--info")
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
            gradlew("sonar", "${getSonarOptions()} --info")
        }
    }
}

void verifyJacocoReport() {
    sh '''
        REPORT="build/reports/jacoco/aggregate/jacoco.xml"

        echo "=== JaCoCo aggregate report ==="
        ls -lh "$REPORT"
        test -s "$REPORT"

        echo "=== JaCoCo counters ==="
        grep -o '<counter type="LINE"[^>]*/>' "$REPORT" | tail -1

        echo "=== E2E-covered production classes ==="
        grep -o '<class name="[^"]*IntegrationLogger[^"]*"' "$REPORT" | head
        grep -o '<class name="[^"]*SessionInitServiceImpl[^"]*"' "$REPORT" | head
    '''
}

void gradlew(String task, String options) {
    withPreparedEnv("sberjdk-21") {
        withCredentials([usernamePassword(
                credentialsId: 'aef_dab2c_ift_cab-sa-dvo08817_ad_domain',
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
            "-Dsonar.coverage.jacoco.xmlReportPaths=${pwd()}/build/reports/jacoco/aggregate/jacoco.xml",
    ].join(" ")
}

return wrapJenkinsfile(this)
