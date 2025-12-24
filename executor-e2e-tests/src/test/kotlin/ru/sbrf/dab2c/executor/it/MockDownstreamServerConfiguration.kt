package ru.sbrf.dab2c.executor.it

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Server
import io.grpc.ServerBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Configuration
import java.net.URI

private val logger = KotlinLogging.logger {}

@TestConfiguration
class MockDownstreamServerConfiguration(
    private val mockService: MockGigaVoiceDownstreamService,
    @Value("\${grpc.client.downstream.address}") private val downstreamAddress: String
) : SmartLifecycle {

    private var server: Server? = null
    private var running = false

    private val port: Int by lazy {
        parsePortFromAddress(downstreamAddress)
    }

    override fun start() {
        logger.info { "Starting mock downstream gRPC server on port $port" }
        server = ServerBuilder
            .forPort(port)
            .addService(mockService)
            .build()
            .start()
        running = true
        logger.info { "Mock downstream gRPC server started on port $port" }
    }

    override fun stop() {
        logger.info { "Stopping mock downstream gRPC server" }
        server?.shutdown()
        server?.awaitTermination()
        running = false
        logger.info { "Mock downstream gRPC server stopped" }
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Int.MIN_VALUE + 1000

    private fun parsePortFromAddress(address: String): Int {
        // Address format: static://localhost:29091
        val withoutScheme = address.substringAfter("://")
        return URI("http://$withoutScheme").port
    }
}
