package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.security.aspect

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidated

internal open class SpNameValidatedService {

    @SecurityValidated(SERVICE_NAME)
    open fun validatedMethod(
        @SecurityValidated.SpNameHolder
        spName: String,
    ) = spName

    companion object {
        const val SERVICE_NAME = "test"
    }
}
