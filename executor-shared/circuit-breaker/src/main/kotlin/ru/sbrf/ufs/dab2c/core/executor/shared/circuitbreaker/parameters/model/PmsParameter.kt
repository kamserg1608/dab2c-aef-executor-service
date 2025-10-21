package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model

import ru.sbrf.ufs.platform.config.v2.OptionalValue
import ru.sbrf.ufs.platform.config.v2.ParameterValue

/**
 * Abstract SUP-parameter
 * that describes how to extract itself from [ParameterValue]
 * and how to map itself to [MappedClass].
 */
data class PmsParameter<Type, MappedClass>(

    /** Name of the SUP parameter. */
    val name: String,

    /** Class-reference of the SUP parameter. */
    val type: Class<Type>,

    /** Method-extractor from [ParameterValue]. */
    val extractor: (ParameterValue) -> OptionalValue<Type>,

    /** Method that maps parameter on corresponding property of DTO class. */
    val enricher: (MappedClass, Type) -> Unit

)
