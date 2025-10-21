package ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractorPart
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.impl.CallerNameExtractorChain

/** [CallerNameExtractorChain] bean configuration. */
@Configuration
class CallerNameExtractorChainConfiguration {

    @Bean
    @Suppress("UNCHECKED_CAST")
    internal fun callerNameDetector(extractors: List<CallerNameExtractorPart<*>>) =
        CallerNameExtractorChain(extractors as List<CallerNameExtractorPart<Any>>)
}
