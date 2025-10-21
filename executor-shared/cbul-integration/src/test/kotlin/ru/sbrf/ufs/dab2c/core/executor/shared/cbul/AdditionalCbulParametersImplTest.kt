package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.AdditionalCbulParametersImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.config.ExtendedConfigServiceFactory

class AdditionalCbulParametersImplTest {

    private val actualService = AdditionalCbulParametersImpl(
        ExtendedConfigServiceFactory.buildFileBased("sup/cbul_add_params.json")
    )

    private val emptyService = AdditionalCbulParametersImpl(
        ExtendedConfigServiceFactory.buildFileBased("sup/empty.json")
    )

    @Test
    fun testBathSizeAbsent() {
        assertThat(emptyService.getCbulBatchSize()).isEqualTo(10)
    }

    @Test
    fun testBatchSizeOk() {
        assertThat(actualService.getCbulBatchSize()).isEqualTo(103)
    }
}
