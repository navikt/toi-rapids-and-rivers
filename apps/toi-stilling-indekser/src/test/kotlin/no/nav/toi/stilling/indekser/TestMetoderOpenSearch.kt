package no.nav.toi.stilling.indekser

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.query_dsl.IdsQuery
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.CountRequest
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.opensearch.indices.RefreshRequest

class TestMetoderOpenSearch(private val client: OpenSearchClient) {

    fun finnRekrutteringsbistandStilling(uuid: String, indeks: String): RekrutteringsbistandStilling? {
        val idQuery = IdsQuery.Builder().values(uuid).build()
        val query = Query.Builder().ids(idQuery).build()
        val search = SearchRequest.Builder().index(indeks).query(query).build()

        val response = client.search(search, RekrutteringsbistandStilling::class.java)
        return response.hits().hits().first().source()
    }

    fun hentAntallDokumenter(indeks: String): Long {
        val request = CountRequest.Builder().index(indeks).build()

        val response = client.count(request).count()
        return response
    }

    fun refreshIndex() {
        val request = RefreshRequest.Builder().index("$stillingAlias*").build()
        client.indices().refresh(request)
    }
}
