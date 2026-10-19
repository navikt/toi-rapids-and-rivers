package no.nav.toi.stilling.indekser.fasit

import tools.jackson.databind.json.JsonMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.head
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.AdStatus
import no.nav.pam.stilling.ext.avro.Administration
import no.nav.pam.stilling.ext.avro.AdministrationStatus
import no.nav.pam.stilling.ext.avro.Classification
import no.nav.pam.stilling.ext.avro.Company
import no.nav.pam.stilling.ext.avro.Contact
import no.nav.pam.stilling.ext.avro.Location
import no.nav.pam.stilling.ext.avro.PrivacyChannel
import no.nav.pam.stilling.ext.avro.Property
import no.nav.pam.stilling.ext.avro.RemarkType
import no.nav.pam.stilling.ext.avro.StyrkCategory
import no.nav.toi.TestRapid
import no.nav.toi.stilling.indekser.AccessTokenClient
import no.nav.toi.stilling.indekser.IndekserStillingLytter
import no.nav.toi.stilling.indekser.IndekserStillingsinfoLytter
import no.nav.toi.stilling.indekser.IndexClient
import no.nav.toi.stilling.indekser.JacksonConfig
import no.nav.toi.stilling.indekser.OpenSearchConfig
import no.nav.toi.stilling.indekser.OpenSearchService
import no.nav.toi.stilling.indekser.RekrutteringsbistandStilling
import no.nav.toi.stilling.indekser.configureAvroSecurityWhitelist
import no.nav.toi.stilling.indekser.dto.Stillingsinfo
import no.nav.toi.stilling.indekser.eksternLytter.konverterTilStilling
import no.nav.toi.stilling.indekser.kandidatlisteInfo.KandidatlisteInfoLytter
import no.nav.toi.stilling.indekser.stillingsinfo.StillingsinfoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.net.http.HttpClient

/**
 * Sikrer at JSON som appen sender til OpenSearch og stilling-api ikke endrer seg.
 *
 * Meldinger fra src/test/resources/fasit går gjennom de ekte lytterne, OpenSearchService og IndexClient. WireMock tar
 * imot forespørslene, og innholdet sammenlignes med fasitfilene.
 *
 * Etter en tilsiktet endring skriver du fasitfilene på nytt med
 *   OPPDATER_FASIT=true ./gradlew :apps:toi-stilling-indekser:test --tests "*IndekseringFasitTest*"
 * og går gjennom endringene i git diff før du committer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndekseringFasitTest {

    init {
        // Avro låser klasse-whitelisten første gang Ad lastes
        configureAvroSecurityWhitelist()
    }

    private val indeks = "stilling_1"
    private val fasitmappe = "src/test/resources/fasit"

    private val openSearch = WireMockServer(options().dynamicPort()).also(WireMockServer::start).apply {
        stubFor(head(urlPathMatching("/$indeks/_doc/.+")).willReturn(ok()))
        stubFor(put(urlPathMatching("/$indeks/_doc/.+")).willReturn(okJson(dokumentrespons("created"))))
        stubFor(post(urlPathMatching("/$indeks/_update/.+")).willReturn(okJson(dokumentrespons("updated"))))
        stubFor(post(urlPathEqualTo("/_bulk")).willReturn(okJson("""{ "took": 1, "errors": false, "items": [] }""")))
    }

    private val stillingApi = WireMockServer(options().dynamicPort()).also(WireMockServer::start).apply {
        stubFor(
            post(urlEqualTo("/token")).willReturn(
                okJson("""{ "token_type": "Bearer", "expires_in": 3599, "ext_expires_in": 3599, "access_token": "et-token" }""")
            )
        )
        stubFor(
            post(urlEqualTo("/indekser/stillingsinfo/bulk")).willReturn(
                okJson(
                    """
                    [
                      {
                        "eierNavident": "Z999999", "eierNavn": "Eier Eiersen", "eierNavKontorEnhetId": "0315",
                        "stillingsid": "aaaaaaaa-1111-2222-3333-444444444444", "stillingsinfoid": "1",
                        "stillingskategori": "STILLING", "ukjentFelt": "ignoreres"
                      },
                      {
                        "eierNavident": null, "eierNavn": null, "eierNavKontorEnhetId": null,
                        "stillingsid": "bbbbbbbb-1111-2222-3333-444444444444", "stillingsinfoid": null,
                        "stillingskategori": null
                      }
                    ]
                    """
                )
            )
        )
    }

    private val env = mutableMapOf(
        "OPEN_SEARCH_URI" to openSearch.baseUrl(),
        "OPEN_SEARCH_USERNAME" to "brukernavn",
        "OPEN_SEARCH_PASSWORD" to "passord",
        "INDEKS_VERSJON" to indeks,
        "STILLING_API_URL" to stillingApi.baseUrl(),
        "STILLING_API_SCOPE" to "api://stilling-api/.default",
        "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "${stillingApi.baseUrl()}/token",
        "AZURE_APP_CLIENT_ID" to "klient",
        "AZURE_APP_CLIENT_SECRET" to "hemmelig",
    )

    private val openSearchService = OpenSearchService(
        IndexClient(OpenSearchConfig(env, JacksonConfig.objectMapper).openSearchClient()),
        env
    )

    @AfterAll
    fun afterAll() {
        openSearch.stop()
        stillingApi.stop()
    }

    @Test
    fun `direktemeldt stilling med alle felt utfylt`() {
        val dokument = indekserFraRapid(fil("direktemeldt-melding.json").readText())

        sammenlignMedFasit(fil("direktemeldt-dokument.json"), dokument)
    }

    @Test
    fun `direktemeldt stilling med nullfelt`() {
        val dokument = indekserFraRapid(fil("direktemeldt-med-nullfelt-melding.json").readText())

        sammenlignMedFasit(fil("direktemeldt-med-nullfelt-dokument.json"), dokument)
    }

    @Test
    fun `eksterne stillinger fra topic indekseres med bulk`() {
        openSearch.resetRequests()

        openSearchService.indekser(
            listOf(
                RekrutteringsbistandStilling(konverterTilStilling(fullAd()), stillingsinfo()),
                RekrutteringsbistandStilling(konverterTilStilling(adMedNullfelt()), null),
            ),
            indeks
        )

        val forespørsel = openSearch.findAll(postRequestedFor(urlPathEqualTo("/_bulk"))).single()
        val linjer = forespørsel.bodyAsString.trim().lines()
        sammenlignMedFasit(fil("ekstern-bulk.json"), linjer.joinToString(",", "[", "]"))
    }

    @Test
    fun `oppdatering av stillingsinfo fra rapid`() {
        val dokument = oppdaterFraRapid(fil("stillingsinfo-melding.json").readText()) { rapid ->
            IndekserStillingsinfoLytter(rapid, openSearchService, indeks)
        }

        sammenlignMedFasit(fil("stillingsinfo-oppdatering.json"), dokument)
    }

    @Test
    fun `oppdatering av kandidatlisteInfo fra rapid`() {
        val dokument = oppdaterFraRapid(fil("kandidatlisteinfo-melding.json").readText()) { rapid ->
            KandidatlisteInfoLytter(rapid, openSearchService, indeks)
        }

        sammenlignMedFasit(fil("kandidatlisteinfo-oppdatering.json"), dokument)
    }

    @Test
    fun `henting av stillingsinfo fra stilling-api`() {
        stillingApi.resetRequests()
        val httpClient = HttpClient.newHttpClient()
        val accessTokenClient = AccessTokenClient(env, httpClient, JacksonConfig.objectMapper)
        val stillingsinfoClient = StillingsinfoClient(env, httpClient, accessTokenClient, JacksonConfig.objectMapper)

        val stillingsinfo = stillingsinfoClient.hentStillingsinfo(
            listOf("aaaaaaaa-1111-2222-3333-444444444444", "bbbbbbbb-1111-2222-3333-444444444444")
        )

        val forespørsel = stillingApi.findAll(postRequestedFor(urlEqualTo("/indekser/stillingsinfo/bulk"))).single()
        assertThatJson(forespørsel.bodyAsString).isEqualTo(
            """{ "uuider": ["aaaaaaaa-1111-2222-3333-444444444444", "bbbbbbbb-1111-2222-3333-444444444444"] }"""
        )
        assertThat(stillingsinfo).containsExactly(
            stillingsinfo(),
            Stillingsinfo(
                eierNavident = null,
                eierNavn = null,
                eierNavKontorEnhetId = null,
                stillingsid = "bbbbbbbb-1111-2222-3333-444444444444",
                stillingsinfoid = null,
                stillingskategori = null
            )
        )
        stillingApi.verify(
            postRequestedFor(urlEqualTo("/token")).withRequestBody(
                equalTo("grant_type=client_credentials&client_id=klient&client_secret=hemmelig&scope=api%3A%2F%2Fstilling-api%2F.default")
            )
        )
    }

    /** Sender meldingen til IndekserStillingLytter og returnerer dokumentet slik det ble sendt til OpenSearch. */
    private fun indekserFraRapid(melding: String): String {
        openSearch.resetRequests()

        val rapid = TestRapid()
        IndekserStillingLytter(rapid, openSearchService, indeks)
        rapid.sendTestMessage(melding)

        val forespørsel = openSearch.findAll(putRequestedFor(urlPathMatching("/$indeks/_doc/.+"))).single()
        assertThat(forespørsel.url).startsWith("/$indeks/_doc/123e4567-e89b-12d3-a456-426614174000")
        assertThat(rapid.inspektør.size).isEqualTo(1)
        return forespørsel.bodyAsString
    }

    /** Sender meldingen til lytteren og returnerer oppdateringen slik den ble sendt til OpenSearch. */
    private fun oppdaterFraRapid(melding: String, lytter: (TestRapid) -> Unit): String {
        openSearch.resetRequests()

        val rapid = TestRapid()
        lytter(rapid)
        rapid.sendTestMessage(melding)

        val forespørsel = openSearch.findAll(postRequestedFor(urlPathMatching("/$indeks/_update/.+"))).single()
        assertThat(forespørsel.url).startsWith("/$indeks/_update/123e4567-e89b-12d3-a456-426614174000")
        return forespørsel.bodyAsString
    }

    private fun fil(navn: String) = File("$fasitmappe/$navn")

    private fun sammenlignMedFasit(fasit: File, dokument: String) {
        if (System.getenv("OPPDATER_FASIT") == "true") {
            val mapper = JsonMapper.shared()
            fasit.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(dokument)) + "\n")
        }

        assertThatJson(dokument).isEqualTo(fasit.readText())
    }

    private fun dokumentrespons(resultat: String) = """
        {
          "_index": "$indeks", "_id": "123e4567-e89b-12d3-a456-426614174000", "_version": 1, "result": "$resultat",
          "_shards": { "total": 1, "successful": 1, "failed": 0 }, "_seq_no": 0, "_primary_term": 1
        }
    """

    private fun stillingsinfo() = Stillingsinfo(
        eierNavident = "Z999999",
        eierNavn = "Eier Eiersen",
        eierNavKontorEnhetId = "0315",
        stillingsid = "aaaaaaaa-1111-2222-3333-444444444444",
        stillingsinfoid = "1",
        stillingskategori = "STILLING"
    )

    private fun fullAd() = Ad(
        "aaaaaaaa-1111-2222-3333-444444444444",
        "R123456",
        "Utvikler i Nav",
        AdStatus.ACTIVE,
        PrivacyChannel.SHOW_ALL,
        Administration(
            AdministrationStatus.PENDING,
            listOf(RemarkType.FOREIGN_JOB, RemarkType.DUPLICATE),
            "kommentar",
            "Rapportør Rapportørsen",
            "Z999999"
        ),
        "2025-01-01T12:00:00",
        "2025-02-01T00:00:00.123",
        "2024-12-31T09:30:00",
        "2025-01-02T10:15:30.5",
        Company("Navn AS", "Offentlig navn AS", "123456789", "987654321", "BEDR"),
        listOf(StyrkCategory("2512.01", "Utvikler")),
        "IMPORTAPI",
        "FINN",
        "ref-1",
        "2025-01-01T12:00:01",
        "Testbedrift AS",
        listOf(Location("Gate 1", "0123", "OSLO", "OSLO", "Oslo", "NORGE", "59.91", "10.75", "0301", "03")),
        egenskaper().map { (key, value) -> Property(key, value) },
        listOf(Contact("Kari Kontakt", "kari@eksempel.no", "12345678", "Kontaktperson", "Leder")),
        listOf(
            Classification("JANZZ", "123", "Utvikler", 0.9, "456"),
            Classification("ESCO", "http://data.europa.eu/esco/occupation/1", "Programmerer", 1.0, "789"),
        )
    )

    private fun adMedNullfelt() = Ad(
        "bbbbbbbb-1111-2222-3333-444444444444",
        "R654321",
        "Tittel som ikke brukes for DIR",
        AdStatus.INACTIVE,
        PrivacyChannel.INTERNAL_NOT_SHOWN,
        null,
        "",
        null,
        "2025-03-30T01:30:00",
        "2025-03-30T02:30:00",
        null,
        emptyList(),
        "DIR",
        "DIR",
        "ref-2",
        null,
        null,
        listOf(Location(null, null, null, null, null, "NORGE", null, null, null, null)),
        emptyList(),
        null,
        null
    )

    private fun egenskaper() = listOf(
        "externalref" to "1005/18",
        "sector" to "Offentlig",
        "positioncount" to "2",
        "salary" to "550000.5",
        "stortall" to "12345678901234567890",
        "antall" to "2 stillinger",
        "workday" to "true story",
        "tom" to "",
        "blank" to "   ",
        "adtext" to "<p>Den beste stillingen</p>",
        "tags" to """["INKLUDERING__ARBEIDSTID","TILTAK_ELLER_VIRKEMIDDEL__LÆRLINGPLASS"]""",
        "searchtags" to """[{"label":"Utvikler","score":1.0},{"label":"Programmerer","score":0.25137392}]""",
        "ontologyJobtitle" to """{"konseptId":"123","label":"Utvikler","styrk08":"2512"}""",
    )
}
