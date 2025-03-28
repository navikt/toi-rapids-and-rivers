package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.ObjectMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.IndexResponse
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.opensearch.client.opensearch.indices.*
import java.io.StringReader


const val stillingAlias: String = "stilling"

class IndexClient(private val client: OpenSearchClient, private val objectMapper: ObjectMapper) {

    companion object {
        private val osSettings = IndexClient::class.java
            .getResource("/stilling-common.json")?.readText()
        private val stillingMapping = IndexClient::class.java
            .getResource("/stilling-mapping.json")?.readText()
    }

    fun indekser(stillinger: List<RekrutteringsbistandStilling>, indeks: String): BulkResponse {
        val bulkRequest = BulkRequest.Builder()

        stillinger.forEach { stilling ->
            val operation = BulkOperation.Builder()
                .index { idx ->
                    idx.index(indeks)
                        .id(stilling.stilling.uuid.toString())
                        .document(stilling)
                }
                .build()

            bulkRequest.operations(operation)
        }
        val uuider = stillinger.map { it.stilling.uuid }
        log.info("Indekserte ${uuider.size} stillinger i indeks '$indeks'. UUIDer: $uuider")

        return client.bulk(bulkRequest.build())
    }

    fun indekserStilling(stilling: RekrutteringsbistandStilling, indeks: String): IndexResponse {
        val uuid = stilling.stilling.uuid
        val request = IndexRequest.Builder<RekrutteringsbistandStilling>()
            .index(indeks)
            .id(uuid.toString())
            .document(stilling)
            .build()

        val response = client.index(request)

        log.info("Indeksert stilling med UUID $uuid i indeks '$indeks', response: ${response.result()}")
        return response
    }


    fun finnesIndeks(indeksnavn: String): Boolean {
        val request: ExistsRequest = ExistsRequest.Builder().index(indeksnavn).build()
        return client.indices().exists(request).value()
    }

    fun opprettIndeks(indeksNavn: String) {
        val mapper = client._transport().jsonpMapper()

        val createIndexRequest = CreateIndexRequest.Builder().index(indeksNavn)
        val settingsParser = mapper.jsonProvider().createParser(osSettings?.let { StringReader(it) })
        val indexSettings = IndexSettings._DESERIALIZER.deserialize(settingsParser, mapper)
        createIndexRequest.settings(indexSettings)
        val mappingsParser = mapper.jsonProvider().createParser(stillingMapping?.let { StringReader(it) })
        val typeMapping = TypeMapping._DESERIALIZER.deserialize(mappingsParser, mapper)
        createIndexRequest.mappings(typeMapping)

        val indeksOpprettet = client.indices().create(createIndexRequest.build()).acknowledged()

        log.info("Indeks '$indeksNavn' ble opprettet: $indeksOpprettet")
    }

    fun oppdaterAlias(indeksNavn: String) {
        val request = UpdateAliasesRequest.Builder().actions { actions ->
            // Remove action
            actions.remove { remove ->
                remove.index("$stillingAlias*")
                    .alias(stillingAlias)
            }
            // Add action
            actions.add { add ->
                add.index(indeksNavn)
                    .alias(stillingAlias)
            }
        }.build()

        val aliasOppdatert = client.indices().updateAliases(request).acknowledged()

        if (!aliasOppdatert) {
            throw Exception("Klarte ikke oppdatere alias $stillingAlias til å peke på $indeksNavn")
        }
        log.info("Oppdatere alias $stillingAlias til å peke på $indeksNavn")
    }

    fun fjernAlias() {
        log.info("Prøver å fjerne aliaser")
        val request = UpdateAliasesRequest.Builder().actions { actions ->
            // Remove action
            actions.remove { remove ->
                remove.index("$stillingAlias*")
                    .alias(stillingAlias)
            }
        }.build()

        val aliasOppdatert = client.indices().updateAliases(request).acknowledged()

        val alias = hentIndeksAliasPekerPå()

        log.info("Alias peker på noe etter fosøk av fjerning: $alias")
        if (!aliasOppdatert) {
            throw Exception("Klarte ikke fjerne alias")
        }
    }



    fun hentIndeksAliasPekerPå(): String? {
        val request = GetAliasRequest.Builder().index("$stillingAlias*").build()

        val response = client.indices().getAlias(request)

        val indekser = response.result()

        log.info("Størrelse på aliaser ${indekser}")

        val antallAlias = indekser.filter { it.value.aliases().size > 0 }

        return when (antallAlias.size) {
            0 -> null
            1 -> antallAlias.keys.first()
            else -> throw Exception(
                "Klarte ikke hente indeks for alias, fikk mer enn én indeks. " +
                        "Antall indekser: ${response.result().size}"
            )
        }
    }
}
