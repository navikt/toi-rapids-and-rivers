package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import org.opensearch.client.opensearch.OpenSearchClient
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class EsCvObjectMotherVariantIntegrationTest {
    companion object {
        private val esIndex = "kandidater"

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
        testEsClient = EsTestUtils.esClient(elasticsearch)
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
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Toppnivå felter (kun felt vi vet finnes i ES)
        assertThat(cvJson["kommunenummerstring"].asText()).isEqualTo(expectedJson["kommunenummerstring"].asText())
        assertThat(cvJson["fylkeNavn"].asText()).isEqualTo(expectedJson["fylkeNavn"].asText())
        assertThat(cvJson["kommuneNavn"].asText()).isEqualTo(expectedJson["kommuneNavn"].asText())
        assertThat(cvJson["oppstartKode"].asText()).isEqualTo(expectedJson["oppstartKode"].asText())
        assertThat(cvJson["veilederIdent"].asText()).isEqualTo(expectedJson["veilederIdent"].asText())
        assertThat(cvJson["veilederVisningsnavn"].asText()).isEqualTo(expectedJson["veilederVisningsnavn"].asText())
        assertThat(cvJson["veilederEpost"].asText()).isEqualTo(expectedJson["veilederEpost"].asText())

        val harKontakt = cvJson.get("harKontaktinformasjon") ?: cvJson.get("isHarKontaktinformasjon")
        assertThat(harKontakt).isNotNull
        assertThat(harKontakt!!.asBoolean()).isTrue
        assertThat(cvJson.has("totalLengdeYrkeserfaring")).isTrue
        assertThat(cvJson["totalLengdeYrkeserfaring"].asInt()).isGreaterThan(0)

        // Jobbønsker
        assertThat(cvJson["geografiJobbonsker"].size()).isGreaterThan(0)
        assertThat(cvJson["yrkeJobbonskerObj"].size()).isGreaterThan(0)
        assertThat(cvJson["omfangJobbonskerObj"].size()).isGreaterThan(0)
        assertThat(cvJson["ansettelsesformJobbonskerObj"].size()).isGreaterThan(0)
        assertThat(cvJson["arbeidstidsordningJobbonskerObj"].size()).isGreaterThan(0)
        assertThat(cvJson["arbeidsdagerJobbonskerObj"].size()).isEqualTo(2)
        assertThat(cvJson["arbeidstidJobbonskerObj"].size()).isGreaterThan(0)

        // Samlet kompetanse (kombinert fra kompetanse/sertifikat/godkjenninger)
        assertThat(cvJson["samletKompetanseObj"].isArray).isTrue
        assertThat(cvJson["samletKompetanseObj"].size()).isGreaterThan(0)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(2)
        assertThat(cvJson["utdanning"][0]["fraDato"].asText()).isEqualTo(expectedJson["utdanning"][0]["fraDato"].asText())
        assertThat(cvJson["utdanning"][0]["tilDato"].asText()).isEqualTo(expectedJson["utdanning"][0]["tilDato"].asText())
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asText()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asText())
        assertThat(cvJson["utdanning"][0]["nusKode"].asText()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asText())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asText()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asText())
        assertThat(cvJson["utdanning"][1]["utdannelsessted"].asText()).isEqualTo(expectedJson["utdanning"][1]["utdannelsessted"].asText())
        assertThat(cvJson["utdanning"][1]["nusKode"].asText()).isEqualTo(expectedJson["utdanning"][1]["nusKode"].asText())
        assertThat(cvJson["utdanning"][1]["alternativGrad"].asText()).isEqualTo(expectedJson["utdanning"][1]["alternativGrad"].asText())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["fraDato"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["fraDato"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["tilDato"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["tilDato"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["alternativStillingstittel"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["sted"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["sted"].asText())

        // Kompetanse
        assertThat(cvJson["kompetanseObj"].size()).isEqualTo(4)
        assertThat(cvJson["kompetanseObj"][0]["kompKode"].asText()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKode"].asText())
        assertThat(cvJson["kompetanseObj"][0]["kompKodeNavn"].asText()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKodeNavn"].asText())
        assertThat(cvJson["kompetanseObj"][1]["kompKodeNavn"].asText()).isEqualTo(expectedJson["kompetanseObj"][1]["kompKodeNavn"].asText())
        assertThat(cvJson["kompetanseObj"][2]["kompKodeNavn"].asText()).isEqualTo(expectedJson["kompetanseObj"][2]["kompKodeNavn"].asText())
        assertThat(cvJson["kompetanseObj"][3]["kompKodeNavn"].asText()).isEqualTo(expectedJson["kompetanseObj"][3]["kompKodeNavn"].asText())

        // Språk
        assertThat(cvJson["sprak"].size()).isEqualTo(2)
        assertThat(cvJson["sprak"][0]["sprakKodeTekst"].asText()).isEqualTo(expectedJson["sprak"][0]["sprakKodeTekst"].asText())
        assertThat(cvJson["sprak"][0]["alternativTekst"].asText()).isEqualTo(expectedJson["sprak"][0]["alternativTekst"].asText())
        assertThat(cvJson["sprak"][0]["ferdighetMuntlig"].asText()).isEqualTo(expectedJson["sprak"][0]["ferdighetMuntlig"].asText())

        // Kurs
        assertThat(cvJson["kursObj"].size()).isEqualTo(3)
        assertThat(cvJson["kursObj"][0]["tittel"].asText()).isEqualTo(expectedJson["kursObj"][0]["tittel"].asText())
        assertThat(cvJson["kursObj"][0]["arrangor"].asText()).isEqualTo(expectedJson["kursObj"][0]["arrangor"].asText())
        assertThat(cvJson["kursObj"][1]["tittel"].asText()).isEqualTo(expectedJson["kursObj"][1]["tittel"].asText())
        assertThat(cvJson["kursObj"][2]["tittel"].asText()).isEqualTo(expectedJson["kursObj"][2]["tittel"].asText())

        // Sertifikater
        assertThat(cvJson["sertifikatObj"].size()).isEqualTo(3)
        assertThat(cvJson["sertifikatObj"][0]["sertifikatKodeNavn"].asText()).isEqualTo(expectedJson["sertifikatObj"][0]["sertifikatKodeNavn"].asText())
        assertThat(cvJson["sertifikatObj"][0]["sertifikatKode"].asText()).isEqualTo(expectedJson["sertifikatObj"][0]["sertifikatKode"].asText())
        assertThat(cvJson["sertifikatObj"][0]["fraDato"].asText()).isEqualTo(expectedJson["sertifikatObj"][0]["fraDato"].asText())
        assertThat(cvJson["sertifikatObj"][0]["tilDato"].asText()).isEqualTo(expectedJson["sertifikatObj"][0]["tilDato"].asText())

        // Førerkort
        assertThat(cvJson["forerkort"].size()).isEqualTo(4)
        assertThat(cvJson["forerkort"][0]["forerkortKodeKlasse"].asText()).isEqualTo(expectedJson["forerkort"][0]["forerkortKodeKlasse"].asText())
        assertThat(cvJson["forerkort"][1]["forerkortKodeKlasse"].asText()).isEqualTo(expectedJson["forerkort"][1]["forerkortKodeKlasse"].asText())
    }

    @Test
    fun `giveMeEsCv2 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv2()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asText()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asText())
        assertThat(cvJson["utdanning"][0]["nusKode"].asText()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asText())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asText()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asText())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["alternativStillingstittel"].asText())
    }

    @Test
    fun `giveMeEsCv3 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv3()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asText()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asText())
        assertThat(cvJson["utdanning"][0]["nusKode"].asText()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asText())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asText()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asText())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["alternativStillingstittel"].asText())
    }

    @Test
    fun `giveMeEsCv4 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv4()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asText()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asText())
        assertThat(cvJson["utdanning"][0]["nusKode"].asText()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asText())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asText()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asText())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asText())
    }

    @Test
    fun `giveMeEsCv5 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv5()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asText())

        // Kompetanse
        assertThat(cvJson["kompetanseObj"].size()).isGreaterThan(0)
        assertThat(cvJson["kompetanseObj"][0]["kompKode"].asText()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKode"].asText())
        assertThat(cvJson["kompetanseObj"][0]["kompKodeNavn"].asText()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKodeNavn"].asText())
    }

    @Test
    fun `giveMeEsCv6 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv6()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asText()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asText())
        assertThat(cvJson["utdanning"][0]["nusKode"].asText()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asText())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asText()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asText())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asText())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asText())

        // Kurs
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

    @Disabled
    @Test
    fun `fritattAgKandidatsok skal fjernes test at felter ikkenfinnes  i ES-dokumentet Fjern testen etter fjerning av feltet`() {
        val cv = EsCvObjectMother.giveMeCvFritattForAgKandidatsok()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asText()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asText()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson.has("fritattAgKandidatsok")).isFalse
    }

    @Disabled
    @Test
    fun `fritattKandidatsok skal fjernes test at felter finnes ikke i ES-dokumentet Fjern testen etter fjerning av feltet`(){
        val cv = EsCvObjectMother.giveMeEsCv()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        // Assert that deprecated fields are absent
        assertThat(cvJson.has("fritattKandidatsok")).isFalse
    }
}
