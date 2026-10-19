package no.nav.arbeidsgiver.toi.kandidat.indekser

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import no.nav.arbeidsgiver.toi.logging.log
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.opensearch.client.json.jackson3.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.IndexSettings
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import java.io.StringReader

fun esObjectMapper(): JsonMapper = JsonMapper.builder()
    .addModule(offsetTidsmodul())
    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    // Felt som starter med æ, ø eller å forsvinner stille uten denne
    .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
    // OpenSearch-klienten leser dokumenter midt i en større respons, så det kommer tokens etter dokumentet
    .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    .build()

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

        val transport = ApacheHttpClient5TransportBuilder.builder(host)
            .setHttpClientConfigCallback { builder ->
                val tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslcontext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .buildAsync()
                val connectionManager = PoolingAsyncClientConnectionManagerBuilder.create().setTlsStrategy(tlsStrategy).build()
                builder.setDefaultCredentialsProvider(credentialsProvider).setConnectionManager(connectionManager)
            }
            .setMapper(JacksonJsonpMapper(esObjectMapper()))
            .build()
        openSearchClient = OpenSearchClient(transport)

        indexer.filterNot (::finnesIndex).forEach(::opprettIndeks)

        if(aliasIndex() != hovedIndex) {
            oppdaterAlias(hovedIndex)
        }
    }

    fun finnesIndex(index: String) = openSearchClient.indices()
        .exists { req ->
            req.index(index)
        }.value()

    companion object {
        private val osSettings = this::class.java
            .getResource("/cv_settings.json")?.readText()
        private val stillingMapping = this::class.java
            .getResource("/cv_mapping.json")?.readText()
    }

    private fun opprettIndeks(indeksNavn: String) {
        val mapper = openSearchClient._transport().jsonpMapper()

        val createIndexRequest = CreateIndexRequest.Builder().index(indeksNavn)
        val settingsParser = mapper.jsonProvider().createParser(osSettings?.let { StringReader(it) })
        val indexSettings = IndexSettings._DESERIALIZER.deserialize(settingsParser, mapper)
        createIndexRequest.settings(indexSettings)
        val mappingsParser = mapper.jsonProvider().createParser(stillingMapping?.let { StringReader(it) })
        val typeMapping = TypeMapping._DESERIALIZER.deserialize(mappingsParser, mapper)
        createIndexRequest.mappings(typeMapping)

        val indeksOpprettet = openSearchClient.indices().create(createIndexRequest.build()).acknowledged()

        log.info("Indeks '$indeksNavn' ble opprettet: $indeksOpprettet")
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
