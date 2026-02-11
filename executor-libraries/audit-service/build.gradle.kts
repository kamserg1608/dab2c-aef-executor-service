plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-clients:efs-adapter"))
    api(project(":executor-libraries:logging"))
}
