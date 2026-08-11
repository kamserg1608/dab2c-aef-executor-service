package ru.sbrf.dab2c.executor.it.support

import kotlinx.coroutines.runBlocking

/**
 * Runs an integration test with real I/O operations.
 * This is a wrapper over runBlocking designed for integration tests
 * that perform actual network calls (gRPC, HTTP, etc.).
 *
 * Unlike runTest from kotlinx-coroutines-test, this uses real time
 * and real dispatchers, which is required for integration tests.
 */
fun runItTest(block: suspend () -> Unit) {
    runBlocking { block() }
}
