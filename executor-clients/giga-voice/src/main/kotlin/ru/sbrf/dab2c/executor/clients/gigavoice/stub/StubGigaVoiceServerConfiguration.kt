package ru.sbrf.dab2c.executor.clients.gigavoice.stub

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.net.URI

private val logger = KotlinLogging.logger {}

private const val LIFECYCLE_PHASE_OFFSET = 1000

/**
 * Spring configuration for stub GigaVoice gRPC server.
 * Active only when STUB-CLIENTS profile is enabled.
 */
@Configuration
@Profile("STUB-CLIENTS")
class StubGigaVoiceServerConfiguration(
    private val stubGigaVoiceService: StubGigaVoiceService,
    @Value("\${grpc.client.downstream.address}") private val downstreamAddress: String
) : SmartLifecycle {

    private var server: Server? = null
    private var running = false

    private val port: Int by lazy {
        parsePortFromAddress(downstreamAddress)
    }

    override fun start() {
        logger.info { "Starting stub GigaVoice gRPC server on port $port" }
        server = ServerBuilder
            .forPort(port)
            .addService(stubGigaVoiceService)
            .build()
            .start()
        running = true
        logger.info { "Stub GigaVoice gRPC server started on port $port" }
    }

    override fun stop() {
        logger.info { "Stopping stub GigaVoice gRPC server" }
        server?.shutdown()
        server?.awaitTermination()
        running = false
        logger.info { "Stub GigaVoice gRPC server stopped" }
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Int.MIN_VALUE + LIFECYCLE_PHASE_OFFSET

    private fun parsePortFromAddress(address: String): Int {
        val withoutScheme = address.substringAfter("://")
        return URI("http://$withoutScheme").port
    }
}
