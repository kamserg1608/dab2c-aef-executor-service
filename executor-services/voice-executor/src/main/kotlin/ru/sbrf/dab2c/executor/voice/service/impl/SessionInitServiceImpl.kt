package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ParametersClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ProfileClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.SdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.tracing.TracingParentElement
import ru.sbrf.dab2c.executor.library.tracing.aef.AefRequestContextElement
import ru.sbrf.dab2c.executor.library.tracing.grpc.OtelGrpcBridge
import ru.sbrf.dab2c.executor.logging.MaskingCollector
import ru.sbrf.dab2c.executor.voice.logging.VoiceMdcInitializer
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureTogglesElement
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitResult
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService

/** Initializes voice session by loading session metadata from SDS and EFS. */
@Service
class SessionInitServiceImpl(
    private val sdsClient: SdsClient,
    private val configuratorClient: ConfiguratorClient,
    private val profileClient: ProfileClient,
    private val parametersClient: ParametersClient,
) : SessionInitService {

    private val logger = KotlinLogging.logger {}

    @Suppress("LongMethod")
    override suspend fun initialize(): SessionInitResult {
        val headers = currentHeaders()
        val channel = headers.getHeader(RequestHeader.CHANNEL)
        logger.info { "Session initialization started for channel=$channel" }

        val daSessionMeta = sdsClient.readDaSessionMeta(channel)
        logger.debug { "Fetched DaSessionMeta: sessionId=${daSessionMeta.sessionId}, ucpId=${daSessionMeta.ucpId}" }

        val daSessionCommon = configuratorClient.getDaSessionCommon()
        logger.debug { "Fetched DaSessionCommon. $daSessionCommon" }

        val daSessionUserInfo = profileClient.getPersonInfo()
        daSessionUserInfo.firstName?.let { MaskingCollector.register(it) }
        daSessionUserInfo.patrName?.let { MaskingCollector.register(it) }
        logger.debug { "Fetched DaSessionUserInfo. $daSessionUserInfo" }

        val toggleParams = parametersClient.getParameters(VoiceSessionFeatureToggles.PARAMETER_NAMES)
        val featureToggles = VoiceSessionFeatureToggles(toggleParams)
        logger.debug { "Fetched feature toggles: kapSendExtra=${featureToggles.kapSendExtra}" }

        val daSessionInfo = DaSessionInfo(daSessionMeta, daSessionCommon, daSessionUserInfo)

        VoiceMdcInitializer.updateWithSessionInfo(
            sessionId = daSessionInfo.meta.sessionId,
            ucpId = daSessionInfo.meta.ucpId
        )
        logger.info { "Session initialization completed, MDC updated: sessionId=${daSessionInfo.meta.sessionId}" }

        return SessionInitResult(daSessionInfo, featureToggles)
    }

    @Suppress("LabeledExpression")
    override fun <T> Flow<T>.withSessionContext(headers: Headers): Flow<T> {
        val upstream = this
        return flow {
            val baseContext = HeadersElement(headers) +
                MDCContext() +
                OtelGrpcBridge.coroutineContextElement() +
                AefRequestContextElement() +
                TracingParentElement()
            val (result, mdc) = withContext(baseContext) { initialize() to MDC.getCopyOfContextMap() }
            val fullContext = baseContext +
                SessionInfoElement(result.sessionInfo) +
                VoiceSessionFeatureTogglesElement(result.featureToggles) +
                MDCContext(mdc)
            emitAll(upstream.flowOn(fullContext))
        }
    }
}
