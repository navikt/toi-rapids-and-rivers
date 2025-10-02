package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.Refresh
import org.opensearch.client.opensearch.core.DeleteByQueryRequest
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.apache.hc.core5.http.HttpHost

@Testcontainers
class EsCvObjectMotherVariantIntegrationTest {
    companion object {
        private val esIndex = "kandidatfeed-variants"
        @Container
        private var elasticsearch: ElasticsearchContainer =
            EsTestUtils.defaultElasticsearchContainer()
        private lateinit var testEsClient: ESClient
        private lateinit var client: OpenSearchClient
    }

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @BeforeEach
    fun setUp() {
        testEsClient = EsTestUtils.esClient(elasticsearch, esIndex)
        client = EsTestUtils.openSearchClient(elasticsearch)
        EsTestUtils.ensureIndexClean(client, esIndex)
        EsTestUtils.sleepForAsyncES()
    }

    private fun lagreOgHent(cv: EsCv) =
        EsTestUtils.lagreOgHent(cv, testEsClient, client, esIndex)

    @Test
    fun `giveMeEsCv - basis CV med forventet innhold (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["utdanning"].size()).isGreaterThan(0)
        assertThat(cvJson["yrkeserfaring"].size()).isGreaterThan(0)
        assertThat(cvJson["kompetanseObj"].size()).isGreaterThan(0)
        assertThat(cvJson["sprak"].size()).isGreaterThan(0)
    }

    @Test
    fun `giveMeEsCv2 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv2()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["utdanning"].size()).isGreaterThan(0)
        assertThat(cvJson["yrkeserfaring"].size()).isGreaterThan(0)
    }

    @Test
    fun `giveMeEsCv3 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv3()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["utdanning"].size()).isGreaterThan(0)
    }

    @Test
    fun `giveMeEsCv4 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv4()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["yrkeserfaring"].size()).isGreaterThan(0)
    }

    @Test
    fun `giveMeEsCv5 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv5()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["kompetanseObj"].size()).isGreaterThan(0)
    }

    @Test
    fun `giveMeEsCv6 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv6()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["kursObj"].isArray).isTrue
    }

    @Test
    fun `giveMeCvUtenKompetanse - kompetanse skal være tom (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvUtenKompetanse()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["kompetanseObj"].size()).isEqualTo(0)
    }

    @Test
    fun `giveMeCvForDoedPerson - doed true (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvForDoedPerson()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["doed"].asBoolean()).isTrue
    }

    @Test
    fun `giveMeCvForKode6 - skjerming frKode=6 (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvForKode6()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["frKode"].asText()).isEqualTo("6")
    }

    @Test
    fun `giveMeCvForKode7 - skjerming frKode=7 (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvForKode7()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["frKode"].asText()).isEqualTo("7")
    }

    @Test
    fun `giveMeCvFritattForKandidatsok - flagg satt (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvFritattForKandidatsok()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["fritattKandidatsok"].asBoolean()).isTrue
    }

    @Test
    fun `giveMeCvFritattForAgKandidatsok - flagg satt (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvFritattForAgKandidatsok()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["fritattAgKandidatsok"].asBoolean()).isTrue
    }
}
