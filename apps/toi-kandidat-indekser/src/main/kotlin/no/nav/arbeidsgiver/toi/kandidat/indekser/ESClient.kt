package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder


class ESClient(
    esUrl: String,
    private val esIndex: String,
    esUsername: String,
    esPassword: String
) {
    private val openSearchClient: OpenSearchClient

    fun lagreEsCv(giveMeEsCv: EsCv) {
        openSearchClient.index { req ->
            req.index(esIndex)
                .id(giveMeEsCv.indekseringsnøkkel())
                .document(giveMeEsCv)
        }
    }

    fun slettCv(indekseringsnøkkel: String) {
        openSearchClient.delete { req ->
            req.index(esIndex).id(indekseringsnøkkel)
        }
    }

    init {
        val host = HttpHost.create(esUrl)
        val credentialsProvider = BasicCredentialsProvider().apply {
            setCredentials(
                AuthScope(host),
                UsernamePasswordCredentials(esUsername, esPassword.toCharArray())
            )
        }
        val sslcontext = SSLContextBuilder.create().build()

        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        val transport = ApacheHttpClient5TransportBuilder.builder(host)
            .setHttpClientConfigCallback { builder ->
                val tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslcontext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .buildAsync()
                val connectionManager = PoolingAsyncClientConnectionManagerBuilder.create().setTlsStrategy(tlsStrategy).build()
                builder.setDefaultCredentialsProvider(credentialsProvider).setConnectionManager(connectionManager)
            }
            .setMapper(JacksonJsonpMapper(objectMapper))
            .build()
        openSearchClient = OpenSearchClient(transport)
    }
}