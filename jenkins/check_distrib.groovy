def getDefaultConfig() {
    return [
            stage      : 'upload',
            phase      : 'after',
            description: 'Проверки check_disrib'
    ]
}

def run(extension, extensionAPI) {
    def isEnabled =  env.CHECK_DISTRIB?.toBoolean()?:false
    def version = extensionAPI.getDistrib().getVersion()

    try {
        if (isEnabled) {
            launchCheckDistrib(version)
            println "Результат проверки check_disrib для версии ${version}  - Успех"
        } else {
            unstable("Результат проверки check_disrib для версии ${version}  - Пропущен")
        }
    } catch (e) {
        unstable("Результат проверки check_disrib для версии ${version}  - ${e.getMessage()}")
    }
}

void launchCheckDistrib(version) {
    println "Будет проверен дистрибутив с версией ${version}"

    List<String> params = [
            "TENANT=DAB2C",
            "SUBSYSTEM=ROUTER_EXECUTOR_JAVA_DAB2C",
            "ARTIFACT_VERSION=${version}"
    ]

    triggerRemoteJob(
            abortTriggeredJob: true,
            auth: CredentialsAuth(credentials: "aef_dab2c_ift_cab-sa-dvo08817_ad_domain"),
            job: 'https://api.sbt-jenkins.sigma.sbrf.ru/marsh/job/dosug/job/lib-check/job/check_disrib',
            parameters: params.join("\n"),
            pollInterval: 30)
}

return this
