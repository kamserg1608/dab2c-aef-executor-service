package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.util.extension

import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService
import ru.sbrf.ufs.platform.config.v2.RequestTemplateBuilder

/**
 * Extension allows to get list of strings by [parameter] name.
 */
fun ExtendedConfigService.getStringList(parameter: String): List<String> = RequestTemplateBuilder
    .builder().attributeNames().build().buildRequest(parameter)
    .let {
        getParameters(it).getOne(it).string.let { optionalValue ->
            if (optionalValue.isPresent) {
                optionalValue.list
            } else {
                emptyList()
            }
        }
    }
