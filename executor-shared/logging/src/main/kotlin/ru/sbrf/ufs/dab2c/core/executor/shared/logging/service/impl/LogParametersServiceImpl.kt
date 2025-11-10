package ru.sbrf.ufs.dab2c.core.executor.shared.logging.service.impl


import org.slf4j.MDC
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.PropagateLogParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.extractor.api.LogParameterExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.mdc.MdcFormatter
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.service.api.LogParametersService
import ru.sbrf.ufs.platform.logger.LoggerContext
import ru.sbrf.ufs.platform.logger.LoggerFactory
import ru.sbrf.ufs.platform.logger.Params.RQ_UID
import ru.sbrf.ufs.platform.logger.Params.TRACE_ID
import java.util.UUID

/**
 * Base implementation for LogParametersService.
 */
@Suppress("LateinitUsage")
class LogParametersServiceImpl(extractors: List<LogParameterExtractor<Any>>) : LogParametersService {

    private val extractors: Map<Class<Any>, LogParameterExtractor<Any>> =
        extractors.associateBy { it.supportedType }

    private val context: LoggerContext = LoggerFactory.getLoggerContext()

    private lateinit var contextBackup: MutableMap<String, String>
    private var mdcBackup: MutableMap<String, String>? = null

    override fun processParameters(args: Array<Any>, parameters: PropagateLogParameters) {
        contextBackup = context.asMap()
        mdcBackup = MDC.getCopyOfContextMap()
        return addContextParameters(args)
    }

    private fun addContextParameters(args: Array<Any>) {
        args.asSequence()
            .filter { extractors[it.javaClass] != null }
            .map { arg -> arg to extractors[arg.javaClass]!! }
            .forEach { (arg, logParameterExtractor) ->
                val castedArg = logParameterExtractor.supportedType.cast(arg)

                val logPlatformParameters = logParameterExtractor.extractLoggingParams(castedArg)

                context.put(logPlatformParameters)
                MDC.setContextMap(MdcFormatter.toMdcParamsMap(logPlatformParameters))
            }

        addRqUidToContext()
        addTraceIdToContext()
    }

    //@SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    override fun restoreContext() {
        context.clear()
        context.put(contextBackup)
        MDC.clear()
        if (mdcBackup != null) {
            MDC.setContextMap(mdcBackup)
        }
    }

    private fun addRqUidToContext() {
        var rqUid: String? = LoggerFactory.getLoggerContext().get(RQ_UID)
        if (rqUid == null) rqUid = UUID.randomUUID().toString()

        context.put(RQ_UID, rqUid)
        MDC.put(RQ_UID, rqUid)
    }

    private fun addTraceIdToContext() {
        MDC.put(TRACE_ID, MdcFormatter.formatMdc(TRACE_ID, context.traceId))
    }
}
