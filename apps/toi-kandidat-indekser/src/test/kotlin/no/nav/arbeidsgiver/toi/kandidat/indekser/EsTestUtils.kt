package no.nav.arbeidsgiver.toi.kandidat.indekser

import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch.core.DeleteByQueryRequest
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.testcontainers.elasticsearch.ElasticsearchContainer
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv

/**
 * Felles testverktøy for ES-relaterte integrasjonstester.
 */
object EsTestUtils {
    /** Oppretter en standard ElasticsearchContainer for tester. */
    fun defaultElasticsearchContainer(): ElasticsearchContainer =
        ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.3")
            .withExposedPorts(9200)
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")

    /** Lager en OpenSearchClient koblet til gitt container. */
    fun openSearchClient(container: ElasticsearchContainer): OpenSearchClient =
        OpenSearchClient(
            ApacheHttpClient5TransportBuilder.builder(
                HttpHost.create(container.httpHostAddress)
            ).build()
        )

    /** Lager en ESClient mot gitt container og indeks. */
    fun esClient(container: ElasticsearchContainer, index: String): ESClient =
        ESClient(container.httpHostAddress, index, "kandidat", "kandidat")

    /** Sørger for at indeksen finnes og er tom. */
    fun ensureIndexClean(client: OpenSearchClient, index: String) {
        val indexExists = client.indices().exists { it.index(index) }.value()
        if (indexExists) {
            do {
                client.indices().refresh { it.index(index) }
                client.deleteByQuery(
                    DeleteByQueryRequest.Builder()
                        .index(index)
                        .query { it.matchAll { it } }
                        .refresh(Refresh.True)
                        .build()
                )
                flush(client, index)
            } while (client.count { it.index(index) }.count() != 0L)
        } else {
            client.indices().create { it.index(index) }
            flush(client, index)
        }
    }

    fun flush(client: OpenSearchClient, index: String) {
        client.indices().flush { it.index(index) }
    }

    fun waitForCount(client: OpenSearchClient, index: String, expected: Long, maxAttempts: Int = 50, sleepMs: Long = 100) {
        var attempts = 0
        while (attempts < maxAttempts) {
            val c = client.count { it.index(index) }.count()
            if (c == expected) return
            Thread.sleep(sleepMs)
            attempts++
        }
        throw AssertionError("Timed out waiting for count=$expected in index=$index")
    }

    fun sleepForAsyncES(ms: Long = 1000) = Thread.sleep(ms)

    /** Lagrer CV via ESClient og henter den ut igjen via OpenSearchClient. */
    fun lagreOgHent(cv: EsCv, esClient: ESClient, osClient: OpenSearchClient, index: String) =
        run {
            esClient.lagreEsCv(cv)
            waitForCount(osClient, index, 1)
            osClient.get({ req ->
                req.index(index).id(cv.indekseringsnøkkel())
            }, EsCv::class.java)
        }
}
