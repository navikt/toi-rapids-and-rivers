package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import java.security.cert.X509Certificate

class OpenSearchConfig(env: MutableMap<String, String>, private val objectMapper: ObjectMapper) {
    private val url = env["OPEN_SEARCH_URI"]!!
    private val username = env["OPEN_SEARCH_USERNAME"]!!
    private val password = env["OPEN_SEARCH_PASSWORD"]!!

    fun openSearchClient() : OpenSearchClient {
        val host = HttpHost.create(url)
        val credentialsProvider = BasicCredentialsProvider()
        credentialsProvider.setCredentials(
            AuthScope(host),
            UsernamePasswordCredentials(username, password.toCharArray()))

        val sslcontext = SSLContextBuilder
                .create()
                .loadTrustMaterial(
                    null
                ) { chains: Array<X509Certificate?>?, authType: String? -> true }
                .build()

        val builder = ApacheHttpClient5TransportBuilder.builder(host)
            .setMapper(JacksonJsonpMapper(objectMapper))
            .setHttpClientConfigCallback { httpClientBuilder: HttpAsyncClientBuilder ->
                val tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslcontext)
                    .build()
                val connectionManager = PoolingAsyncClientConnectionManagerBuilder
                    .create()
                    .setTlsStrategy(tlsStrategy)
                    .build()
                httpClientBuilder
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setConnectionManager(connectionManager)
            }

        val transport: OpenSearchTransport = builder.build()

        val client = OpenSearchClient(transport)
        return client
    }
}
