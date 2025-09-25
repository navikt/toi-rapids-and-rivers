package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.ObjectMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.core.BulkRequest
import org.opensearch.client.opensearch.core.BulkResponse
import org.opensearch.client.opensearch.core.IndexRequest
import org.opensearch.client.opensearch.core.IndexResponse
import org.opensearch.client.opensearch.core.UpdateRequest
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

        stillinger.forEach { rekrutteringsbistandStilling ->
            val operation = BulkOperation.Builder()
                .index { idx ->
                    idx.index(indeks)
                        .id(rekrutteringsbistandStilling.stilling.uuid.toString())
                        .document(rekrutteringsbistandStilling)
                }
                .build()

            bulkRequest.operations(operation)
        }

        val response = client.bulk(bulkRequest.build())
        if(response.errors()) {
            val feil = response.items().filter { it.error() != null }

            feil.forEach {
                log.error("Greide ikke å indeksere ekstern stilling med UUID ${it.id()}: ${it.error()?.reason()} i indeks $indeks")
            }
        }
        return response
    }

    fun indekserStilling(stilling: RekrutteringsbistandStilling, indeks: String): IndexResponse {
        val uuid = stilling.stilling.uuid
        val request = IndexRequest.Builder<RekrutteringsbistandStilling>()
            .index(indeks)
            .id(uuid.toString())
            .document(stilling)
            .build()

        val response = client.index(request)

        log.info("Indeksert direktemeldt stilling med UUID $uuid i indeks '$indeks', response: ${response.result()}")
        return response
    }

    fun oppdaterStillingsinfo(stillingsId: String, indeks: String, stillingsinfo: Stillingsinfo) {
        val felter = mapOf<String, Any>(
            "stillingsinfo" to stillingsinfo
        )

        try {
            val request = UpdateRequest.Builder<RekrutteringsbistandStilling, Map<String, Any>>()
                .index(indeks)
                .id(stillingsId)
                .doc(felter)
                .build()

            val response = client.update(request, RekrutteringsbistandStilling::class.java)
            log.info("Oppdaterte dokument $stillingsId i indeks $indeks med felter $felter, result=${response.result()}")

        } catch (e: Exception) {
            log.error("Greide ikke å oppdatere dokument med id $stillingsId i indeks $indeks", e)
            throw e
        }
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

    fun oppdaterAlias(indeksNavn: String, fjernAlias: Boolean = false) {
        if(fjernAlias) fjernAlias()
        lagAlias(indeksNavn)
        log.info("Oppdaterte alias $stillingAlias til å peke på $indeksNavn")
    }

    private fun fjernAlias() {
        val request = UpdateAliasesRequest.Builder().actions { actions ->
            // Remove action
            actions.remove { remove ->
                remove.index("$stillingAlias*")
                    .alias(stillingAlias)
            }
        }.build()
        val aliasOppdatert = client.indices().updateAliases(request).acknowledged()

        if (!aliasOppdatert) {
            throw Exception("Klarte ikke fjerne alias $stillingAlias")
        }
        log.info("Fjernet alias $stillingAlias")
    }

    private fun lagAlias(indeksNavn: String) {
        val request = UpdateAliasesRequest.Builder().actions { actions ->
            // Add action
            actions.add { add ->
                add.index(indeksNavn)
                    .alias(stillingAlias)
            }
        }.build()
        val aliasOppdatert = client.indices().updateAliases(request).acknowledged()

        if (!aliasOppdatert) {
            throw Exception("Klarte ikke opprette alias $stillingAlias")
        }
        log.info("Opprettet alias $stillingAlias")
    }


    fun hentIndeksAliasPekerPå(): String? {
        val request = GetAliasRequest.Builder().index("$stillingAlias*").build()

        val response = client.indices().getAlias(request)
        val indekser = response.result()

        val antallAlias = indekser.filter { it.value.aliases().isNotEmpty() }

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
