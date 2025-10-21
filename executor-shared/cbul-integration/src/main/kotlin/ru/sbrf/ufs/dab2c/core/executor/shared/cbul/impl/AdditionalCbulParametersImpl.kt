package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.cbul.starter.CbulConfigService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.AdditionalCbulParameters
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService
import ru.sbrf.ufs.platform.config.v2.RequestTemplateBuilder

private const val DEFAULT_BATCH_SIZE = 10L

/**
 * Default implementation of [AdditionalCbulParameters].
 */
class AdditionalCbulParametersImpl(
    private val configService: ExtendedConfigService
) : AdditionalCbulParameters {

    private val requestBuilder = RequestTemplateBuilder.builder().attributeNames().build()

    override fun getCbulBatchSize(): Int =
        with(requestBuilder.buildRequest(CbulConfigService.CBUL_BATCH_SIZE)) {
            configService.getParameters(this).getOne(this).long
        }.or(DEFAULT_BATCH_SIZE).toInt()
}
