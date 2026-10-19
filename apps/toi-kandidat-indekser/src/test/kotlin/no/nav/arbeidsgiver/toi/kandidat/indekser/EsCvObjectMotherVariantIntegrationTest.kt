package no.nav.arbeidsgiver.toi.kandidat.indekser

import tools.jackson.module.kotlin.jacksonObjectMapper
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
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Toppnivå felter (kun felt vi vet finnes i ES)
        assertThat(cvJson["kommunenummerstring"].asString()).isEqualTo(expectedJson["kommunenummerstring"].asString())
        assertThat(cvJson["fylkeNavn"].asString()).isEqualTo(expectedJson["fylkeNavn"].asString())
        assertThat(cvJson["kommuneNavn"].asString()).isEqualTo(expectedJson["kommuneNavn"].asString())
        assertThat(cvJson["oppstartKode"].asString()).isEqualTo(expectedJson["oppstartKode"].asString())
        assertThat(cvJson["veilederIdent"].asString()).isEqualTo(expectedJson["veilederIdent"].asString())
        assertThat(cvJson["veilederVisningsnavn"].asString()).isEqualTo(expectedJson["veilederVisningsnavn"].asString())
        assertThat(cvJson["veilederEpost"].asString()).isEqualTo(expectedJson["veilederEpost"].asString())

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
        assertThat(cvJson["utdanning"][0]["fraDato"].asString()).isEqualTo(expectedJson["utdanning"][0]["fraDato"].asString())
        assertThat(cvJson["utdanning"][0]["tilDato"].asString()).isEqualTo(expectedJson["utdanning"][0]["tilDato"].asString())
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asString()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asString())
        assertThat(cvJson["utdanning"][0]["nusKode"].asString()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asString())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asString()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asString())
        assertThat(cvJson["utdanning"][1]["utdannelsessted"].asString()).isEqualTo(expectedJson["utdanning"][1]["utdannelsessted"].asString())
        assertThat(cvJson["utdanning"][1]["nusKode"].asString()).isEqualTo(expectedJson["utdanning"][1]["nusKode"].asString())
        assertThat(cvJson["utdanning"][1]["alternativGrad"].asString()).isEqualTo(expectedJson["utdanning"][1]["alternativGrad"].asString())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["fraDato"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["fraDato"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["tilDato"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["tilDato"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["alternativStillingstittel"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["sted"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["sted"].asString())

        // Kompetanse
        assertThat(cvJson["kompetanseObj"].size()).isEqualTo(4)
        assertThat(cvJson["kompetanseObj"][0]["kompKode"].asString()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKode"].asString())
        assertThat(cvJson["kompetanseObj"][0]["kompKodeNavn"].asString()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKodeNavn"].asString())
        assertThat(cvJson["kompetanseObj"][1]["kompKodeNavn"].asString()).isEqualTo(expectedJson["kompetanseObj"][1]["kompKodeNavn"].asString())
        assertThat(cvJson["kompetanseObj"][2]["kompKodeNavn"].asString()).isEqualTo(expectedJson["kompetanseObj"][2]["kompKodeNavn"].asString())
        assertThat(cvJson["kompetanseObj"][3]["kompKodeNavn"].asString()).isEqualTo(expectedJson["kompetanseObj"][3]["kompKodeNavn"].asString())

        // Språk
        assertThat(cvJson["sprak"].size()).isEqualTo(2)
        assertThat(cvJson["sprak"][0]["sprakKodeTekst"].asString()).isEqualTo(expectedJson["sprak"][0]["sprakKodeTekst"].asString())
        assertThat(cvJson["sprak"][0]["alternativTekst"].asString()).isEqualTo(expectedJson["sprak"][0]["alternativTekst"].asString())
        assertThat(cvJson["sprak"][0]["ferdighetMuntlig"].asString()).isEqualTo(expectedJson["sprak"][0]["ferdighetMuntlig"].asString())

        // Kurs
        assertThat(cvJson["kursObj"].size()).isEqualTo(3)
        assertThat(cvJson["kursObj"][0]["tittel"].asString()).isEqualTo(expectedJson["kursObj"][0]["tittel"].asString())
        assertThat(cvJson["kursObj"][0]["arrangor"].asString()).isEqualTo(expectedJson["kursObj"][0]["arrangor"].asString())
        assertThat(cvJson["kursObj"][1]["tittel"].asString()).isEqualTo(expectedJson["kursObj"][1]["tittel"].asString())
        assertThat(cvJson["kursObj"][2]["tittel"].asString()).isEqualTo(expectedJson["kursObj"][2]["tittel"].asString())

        // Sertifikater
        assertThat(cvJson["sertifikatObj"].size()).isEqualTo(3)
        assertThat(cvJson["sertifikatObj"][0]["sertifikatKodeNavn"].asString()).isEqualTo(expectedJson["sertifikatObj"][0]["sertifikatKodeNavn"].asString())
        assertThat(cvJson["sertifikatObj"][0]["sertifikatKode"].asString()).isEqualTo(expectedJson["sertifikatObj"][0]["sertifikatKode"].asString())
        assertThat(cvJson["sertifikatObj"][0]["fraDato"].asString()).isEqualTo(expectedJson["sertifikatObj"][0]["fraDato"].asString())
        assertThat(cvJson["sertifikatObj"][0]["tilDato"].asString()).isEqualTo(expectedJson["sertifikatObj"][0]["tilDato"].asString())

        // Førerkort
        assertThat(cvJson["forerkort"].size()).isEqualTo(4)
        assertThat(cvJson["forerkort"][0]["forerkortKodeKlasse"].asString()).isEqualTo(expectedJson["forerkort"][0]["forerkortKodeKlasse"].asString())
        assertThat(cvJson["forerkort"][1]["forerkortKodeKlasse"].asString()).isEqualTo(expectedJson["forerkort"][1]["forerkortKodeKlasse"].asString())
    }

    @Test
    fun `giveMeEsCv2 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv2()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asString()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asString())
        assertThat(cvJson["utdanning"][0]["nusKode"].asString()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asString())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asString()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asString())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["alternativStillingstittel"].asString())
    }

    @Test
    fun `giveMeEsCv3 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv3()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asString()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asString())
        assertThat(cvJson["utdanning"][0]["nusKode"].asString()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asString())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asString()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asString())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["alternativStillingstittel"].asString())
    }

    @Test
    fun `giveMeEsCv4 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv4()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asString()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asString())
        assertThat(cvJson["utdanning"][0]["nusKode"].asString()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asString())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asString()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asString())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asString())
    }

    @Test
    fun `giveMeEsCv5 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv5()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["stillingstittel"].asString())

        // Kompetanse
        assertThat(cvJson["kompetanseObj"].size()).isGreaterThan(0)
        assertThat(cvJson["kompetanseObj"][0]["kompKode"].asString()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKode"].asString())
        assertThat(cvJson["kompetanseObj"][0]["kompKodeNavn"].asString()).isEqualTo(expectedJson["kompetanseObj"][0]["kompKodeNavn"].asString())
    }

    @Test
    fun `giveMeEsCv6 - alternativ CV-variant (via ES)`() {
        val cv = EsCvObjectMother.giveMeEsCv6()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val expectedJson = mapper.readTree(mapper.writeValueAsString(cv))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)

        // Utdanning
        assertThat(cvJson["utdanning"].size()).isEqualTo(1)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asString()).isEqualTo(expectedJson["utdanning"][0]["utdannelsessted"].asString())
        assertThat(cvJson["utdanning"][0]["nusKode"].asString()).isEqualTo(expectedJson["utdanning"][0]["nusKode"].asString())
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asString()).isEqualTo(expectedJson["utdanning"][0]["alternativGrad"].asString())

        // Yrkeserfaring
        assertThat(cvJson["yrkeserfaring"].size()).isEqualTo(6)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["arbeidsgiver"].asString())
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asString()).isEqualTo(expectedJson["yrkeserfaring"][0]["styrkKode"].asString())

        // Kurs
        assertThat(cvJson["kursObj"].isArray).isTrue
    }

    @Test
    fun `giveMeCvUtenKompetanse - kompetanse skal være tom (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvUtenKompetanse()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["kompetanseObj"].size()).isEqualTo(0)
    }

    @Test
    fun `giveMeCvForDoedPerson - doed true (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvForDoedPerson()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["doed"].asBoolean()).isTrue
    }

    @Test
    fun `giveMeCvForKode6 - skjerming frKode=6 (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvForKode6()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["frKode"].asString()).isEqualTo("6")
    }

    @Test
    fun `giveMeCvForKode7 - skjerming frKode=7 (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvForKode7()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
        assertThat(cv.indekseringsnøkkel()).isEqualTo(kandidatnr)
        assertThat(cvJson["frKode"].asString()).isEqualTo("7")
    }

    @Test
    fun `giveMeCvFritattForKandidatsok - flagg satt (via ES)`() {
        val cv = EsCvObjectMother.giveMeCvFritattForKandidatsok()
        val response = lagreOgHent(cv)
        assertThat(response.found()).isTrue
        val cvJson = mapper.readTree(mapper.writeValueAsString(response.source()))
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
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
        val kandidatnr = cvJson.get("kandidatnr").asString()
        assertThat(kandidatnr).isNotBlank
        assertThat(cvJson.get("arenaKandidatnr").asString()).isEqualTo(kandidatnr)
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
