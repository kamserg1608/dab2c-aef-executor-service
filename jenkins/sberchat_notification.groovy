def getDefaultConfig() {
    return [
            stage      : 'finally',
            phase      : 'after',
            description: 'Нотификация в групповой чат'
    ]
}

def run(extension, extensionAPI) {
    def isEnabled =  env.REPORT_TO_CHAT?.toBoolean()?:false
    def version = extensionAPI.getDistrib().getVersion()

    try {
        if (isEnabled) {
            def purpose = (env.BUILD_PURPOSE == null || env.BUILD_PURPOSE == "") ? env.CHOSEN_DEFAULT_PURPOSE : env.BUILD_PURPOSE
            reportBuildResult(version, purpose, "${currentBuild.result}")
            println "Успешно произведено оповещение о сборке для версии ${version}"
        }
    } catch (e) {
        unstable("Не удалось произвести оповещение о сборке для версии ${version}  - ${e.getMessage()}")
    }
}

void reportBuildResult(version, purpose, status) {

    def statusDescription = ""
    if (status == 'FAILURE') statusDescription = "Данный статус означает неудачную сборку, или то, что сборка была отменена."
    else if (status == 'UNSTABLE') statusDescription = "Данный статус, скорее всего, означает, что не запускался JOB CHECK DISTRIB."

    def ticketsInBuild = sh(script: '(cd ./tmp/config ; bash ./jenkins/tickets_list.sh)', returnStdout: true).trim()

    def data = [
            'peer': '8eed4d76b0b811f0',
            'status': status,
            'text':
"""
Цель сборки: ${purpose}.
Ветка: ${CONFIG_BRANCH}.
Версия: ${version}.
Статус: ${status}. ${statusDescription}
${ticketsInBuild}

Ссылка на DBP Mediators: TODO
""".stripIndent().bytes.encodeBase64().toString(),
            'url': "${currentBuild.absoluteUrl}"
    ]
    String json = writeJSON returnText: true, json: data
    httpRequest httpMode: 'POST',  url: "https://sbt-qa-jenkins.delta.sbrf.ru/sberchatbot", requestBody:  json
}

return this
