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
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder


class ESClient(
    esUrl: String,
    esUsername: String,
    esPassword: String
) {
    private val alias = "kandidater"
    private val hovedIndex = "kandidater-1"
    private val ekstraIndekser = listOf<String>()
    private val indexer = listOf(hovedIndex) + ekstraIndekser

    private val openSearchClient: OpenSearchClient

    fun lagreEsCv(giveMeEsCv: EsCv) {
        indexer.forEach { index ->
            openSearchClient.index { req ->
                req.index(index)
                    .id(giveMeEsCv.indekseringsnøkkel())
                    .document(giveMeEsCv)
                    .refresh(Refresh.True)
            }
        }
    }

    fun slettCv(aktørId: String) {
        indexer.forEach { index ->
            openSearchClient.deleteByQuery{ req ->
                req.index(index).query { q ->
                    q.term { t -> t.field("aktorId").value(FieldValue.of(aktørId)) }
                }.refresh(Refresh.True)
            }
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

        indexer.filterNot (::finnesIndex).forEach(::opprettIndex)

        if(aliasIndex() != hovedIndex) {
            oppdaterAlias(hovedIndex)
        }
    }

    fun finnesIndex(index: String) = openSearchClient.indices()
        .exists { req ->
            req.index(index)
        }.value()

    private fun opprettIndex(index: String) {
        openSearchClient.indices().create { req -> req.index(index) }
    }

    private fun aliasIndex() = try {
        openSearchClient
            .indices()
            .getAlias { req -> req.name(alias) }
            .result()
            .keys
            .firstOrNull()
    } catch (_: Exception) {
        null
    }

    private fun oppdaterAlias(hovedIndex: String) {
        val currentIndices = try {
            openSearchClient.indices()
                .getAlias { req -> req.name(alias) }
                .result()
                .keys
        } catch (_: Exception) {
            emptyList()
        }
        openSearchClient.indices().updateAliases { req ->
            req.actions { action ->
                currentIndices.forEach { index ->
                    action.remove { remove ->
                        remove.index(index).alias(alias)
                    }
                }
                action.add { add ->
                    add.index(hovedIndex).alias(alias)
                }
            }
        }
    }
}