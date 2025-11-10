package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.http.impl


import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.protocol.HttpContext
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.http.api.PooledHttpClient

/**
 * Pooled decorator of [HttpClient] that clear a pool of HTTP connections on demand.
 */
@Suppress("MethodOverloading")
//@SuppressFBWarnings("HCP_HTTP_REQUEST_RESOURCES_NOT_FREED_LOCAL", "Resources are released in releaseConnections()")
open class PooledHttpClientImpl(
    private val delegate: HttpClient
) : HttpClient by delegate, PooledHttpClient {

    private val connectionsPool = mutableListOf<CloseableHttpResponse>()

    override fun releaseConnections() {
        connectionsPool.forEach { it.close() }
        connectionsPool.clear()
    }

    override fun execute(request: HttpUriRequest?): HttpResponse = delegate.execute(request).addToPool()

    override fun execute(request: HttpUriRequest?, context: HttpContext?): HttpResponse =
        delegate.execute(request, context).addToPool()

    override fun execute(target: HttpHost?, request: HttpRequest?): HttpResponse =
        delegate.execute(target, request).addToPool()

    override fun execute(target: HttpHost?, request: HttpRequest?, context: HttpContext?): HttpResponse =
        delegate.execute(target, request, context).addToPool()

    private fun HttpResponse.addToPool(): HttpResponse {
        connectionsPool.add(this as CloseableHttpResponse)
        return this
    }

    companion object {
        private const val MAX_CONN_TOTAL = 50
        private const val MAX_CONN_PER_ROUTE = 50

        /**
         * Creates and configures a pooled HTTP client.
         *
         * @param httpClientBuilder the builder used to configure HTTP client
         * @param totalConnections the max total number of connections (default is [MAX_CONN_TOTAL])
         * @param maxPerConnectionRoute max number of connections per route (default is [MAX_CONN_PER_ROUTE])
         * @return a configured instance of [PooledHttpClientImpl]
         */
        fun create(
            httpClientBuilder: HttpClientBuilder,
            totalConnections: Int = MAX_CONN_TOTAL,
            maxPerConnectionRoute: Int = MAX_CONN_PER_ROUTE
        ): PooledHttpClient = PooledHttpClientImpl(
            httpClientBuilder
                .setMaxConnTotal(totalConnections)
                .setMaxConnPerRoute(maxPerConnectionRoute)
                .build()
        )
    }
}
