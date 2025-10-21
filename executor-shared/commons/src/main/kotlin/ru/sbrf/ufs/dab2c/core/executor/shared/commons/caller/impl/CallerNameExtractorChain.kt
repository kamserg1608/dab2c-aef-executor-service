package ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractorPart
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.util.ClassUtil

/**
 * Detects caller name from given object through delegating it to predefined set of [CallerNameExtractorPart].
 */
class CallerNameExtractorChain(extractors: List<CallerNameExtractorPart<Any>>) : CallerNameExtractor {

    private val extractors = extractors.associateBy { it.supportedClass }

    override fun extract(target: Any?): String? {
        if (target == null) return null

        return ClassUtil.getJavaClassHierarchy(target.javaClass)
            .firstNotNullOfOrNull { extractors[it]?.extract(target) }
    }
}
