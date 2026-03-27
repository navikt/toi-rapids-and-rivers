package no.nav.toi.stilling.indekser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.toi.stilling.indekser.dto.KandidatlisteInfo
import no.nav.toi.stilling.indekser.dto.Stillingsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.opensearch.client.opensearch.OpenSearchClient
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenSearchServiceTest {

    private val env = mutableMapOf<String, String>()
    private val osContainer = OpenSearchContainer()

    private val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

    private lateinit var openSearchClient: OpenSearchClient
    private lateinit var indexClient: IndexClient
    private lateinit var openSearchService: OpenSearchService
    private val stillingApiClient = mockk<StillingApiClient>()

    private lateinit var testMetoderOpenSearch: TestMetoderOpenSearch

    @BeforeEach
    fun init() {
        env["OPEN_SEARCH_URI"] = osContainer.container.httpHostAddress
        env["OPEN_SEARCH_USERNAME"] = osContainer.container.username
        env["OPEN_SEARCH_PASSWORD"] = osContainer.container.password

        env["INDEKS_VERSJON"] = "stilling_20250328"
        env["STILLING_API_URL"] = "enUrl"
        env["STILLING_API_SCOPE"] = "scope"
        env["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"] = "enUrl2"
        env["AZURE_APP_CLIENT_ID"] = "client_id"
        env["AZURE_APP_CLIENT_SECRET"] = "secret"
        env["REINDEKSER_ENABLED"] = "false"
        env["REINDEKSER_INDEKS"] = "stilling_20250401"

        openSearchClient = OpenSearchConfig(env, objectMapper).openSearchClient()
        indexClient = IndexClient(openSearchClient, objectMapper)
        openSearchService = OpenSearchService(indexClient, env)
        testMetoderOpenSearch = TestMetoderOpenSearch(openSearchClient)

        every { stillingApiClient.triggSendingAvStillingerPåRapid() } just runs
    }

    @Test
    fun `Indeks skal bli opprettet`() {
        opprettIndeks()
        val indeks = openSearchService.hentGjeldendeIndeks()

        assertThat(indeks).isEqualTo("stilling_20250328")
    }

    @Test
    fun `Alias 'stilling' skal bli opprettet`() {
        opprettIndeks()
        val alias = openSearchClient.indices().alias
        val result = alias.result()

        val opprettetAlias = result["stilling_20250328"]?.aliases()?.get("stilling")
        val ikkeEksisterendeAlias = result["stilling_20250329"]?.aliases()?.get("stilling")

        assertThat(opprettetAlias).isNotNull
        assertThat(ikkeEksisterendeAlias).isNull()
    }

    @Test
    fun `Alias 'stilling' skal peke på ny index etter reindeksering og bytte av alias`() {
        opprettIndeks()
        env["REINDEKSER_ENABLED"] = "true"
        env["REINDEKSER_INDEKS"] = "stilling_20250329"

        openSearchService.opprettReindekserIndeks()

        env["INDEKS_VERSJON"] = "stilling_20250329"
        openSearchService.byttTilNyIndeks()

        val gjeldendeIndeksMedAlias = indexClient.hentIndeksAliasPekerPå()

        assertThat(gjeldendeIndeksMedAlias).isEqualTo("stilling_20250329")
    }

    @Test
    fun `Ny indeks skal bli opprettet ved reindeksering og gamle skal fortsatt finnes`() {
        opprettIndeks()

        var index1 = indexClient.finnesIndeks("stilling_20250328")
        var indeks3 = indexClient.finnesIndeks("stilling_20250330")

        assertThat(index1).isTrue()
        assertThat(indeks3).isFalse()

        env["REINDEKSER_ENABLED"] = "true"
        env["REINDEKSER_INDEKS"] = "stilling_20250330"
        openSearchService.opprettReindekserIndeks()

        index1 = indexClient.finnesIndeks("stilling_20250328")
        indeks3 = indexClient.finnesIndeks("stilling_20250330")

        assertThat(index1).isTrue()
        assertThat(indeks3).isTrue()
    }

    @Test
    fun `Skal kunne oppdatere stillingsinfo for stilling som allerede er indeksert`() {
        leggTilKafkaMiljøvariabler()
        opprettIndeks()

        testProgramMedHendelse(env, melding) {
            assertThat(size).isEqualTo(1)
        }

        val stillingsinfo = Stillingsinfo(
            eierNavident = "T98765",
            eierNavn = "Tester 2",
            eierNavKontorEnhetId = "1234",
            stillingsid = "123e4567-e89b-12d3-a456-426614174000",
            stillingsinfoid = "24553",
            stillingskategori = "STILLING"
        )
        testMetoderOpenSearch.refreshIndex()

        val stillingFørOppdatering = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")

        openSearchService.oppdaterStillingsinfo(stillingsId = "123e4567-e89b-12d3-a456-426614174000", stillingsinfo = stillingsinfo, indeks = "stilling_20250328")
        testMetoderOpenSearch.refreshIndex()
        val stillingEtterOppdatering = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")

        assertThat(stillingFørOppdatering?.stillingsinfo?.eierNavident).isEqualTo("T123456")
        assertThat(stillingFørOppdatering?.stillingsinfo?.eierNavn).isEqualTo("Tester")

        assertThat(stillingEtterOppdatering?.stillingsinfo?.eierNavident).isEqualTo("T98765")
        assertThat(stillingEtterOppdatering?.stillingsinfo?.eierNavn).isEqualTo("Tester 2")
    }

    @Test
    fun `Skal lese inn melding om stillingsinfo fra rapid og oppdatere stillingsinfo i opensearch`() {
        leggTilKafkaMiljøvariabler()
        opprettIndeks()

        testProgramMedHendelse(env, melding) {
            assertThat(size).isEqualTo(1)
        }
        // Oppdaterer stillingsinfo for stilling som allerede er indeksert
        testProgramMedHendelse(env, stillingsinfoMelding) {
            assertThat(size).isEqualTo(0)
        }
        testMetoderOpenSearch.refreshIndex()

        val antallDokumenter = testMetoderOpenSearch.hentAntallDokumenter("stilling_20250328")
        assertThat(antallDokumenter).isEqualTo(1)

        val rekrutteringsbistandStilling = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")
        val stillingsinfo = rekrutteringsbistandStilling?.stillingsinfo

        assertThat(stillingsinfo).isNotNull
        assertThat(stillingsinfo?.eierNavident).isEqualTo("T23456")
        assertThat(stillingsinfo?.eierNavn).isEqualTo("Tester 2")
        assertThat(stillingsinfo?.eierNavKontorEnhetId).isEqualTo("5678")
    }

    @Test
    fun `Skal lese inn melding om stillingsinfo fra rapid og oppdatere stilling med stillingsinfo`() {
        leggTilKafkaMiljøvariabler()
        opprettIndeks()

        testProgramMedHendelse(env, melding) {
            assertThat(size).isEqualTo(1)
        }

        testMetoderOpenSearch.refreshIndex()
        val antallDokumenter = testMetoderOpenSearch.hentAntallDokumenter("stilling_20250328")

        assertThat(antallDokumenter).isEqualTo(1)

        val rekrutteringsbistandStilling = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")

        assertThat(rekrutteringsbistandStilling?.stilling?.uuid.toString()).isEqualTo("123e4567-e89b-12d3-a456-426614174000")
    }

    @Test
    fun `Skal ikke oppdatere stillingsinfo i opensearch hvis stilling ikke finnes`() {
        val mockIndexClient = mockk<IndexClient>()
        every { mockIndexClient.finnesStilling(any(), any()) } returns false

        val service = OpenSearchService(mockIndexClient, env)
        val rapid = no.nav.toi.TestRapid()
        IndekserStillingsinfoLytter(rapid, service, "stilling_20250328")

        rapid.sendTestMessage(stillingsinfoMelding)

        verify(exactly = 0) { mockIndexClient.oppdaterStillingsinfo(any(), any(), any()) }
    }

    @Test
    fun `Skal kunne oppdatere kandidatlisteInfo for stilling som allerede er indeksert`() {
        leggTilKafkaMiljøvariabler()
        opprettIndeks()

        testProgramMedHendelse(env, melding) {
            assertThat(size).isEqualTo(1)
        }

        val kandidatlisteInfo = KandidatlisteInfo(
            kandidatlisteId = java.util.UUID.fromString("aaaabbbb-cccc-dddd-eeee-ffff00000001"),
            antallKandidater = 3,
            kandidatlisteStatus = "ÅPEN",
            opprettetDato = java.time.ZonedDateTime.parse("2024-06-01T10:00:00Z"),
            eier = "T123456"
        )
        testMetoderOpenSearch.refreshIndex()

        val stillingFørOppdatering = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")

        openSearchService.oppdaterKandidatlisteInfo(stillingsId = "123e4567-e89b-12d3-a456-426614174000", kandidatlisteInfo = kandidatlisteInfo, indeks = "stilling_20250328")
        testMetoderOpenSearch.refreshIndex()
        val stillingEtterOppdatering = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")

        assertThat(stillingFørOppdatering?.kandidatlisteInfo).isNull()

        assertThat(stillingEtterOppdatering?.kandidatlisteInfo?.kandidatlisteId.toString()).isEqualTo("aaaabbbb-cccc-dddd-eeee-ffff00000001")
        assertThat(stillingEtterOppdatering?.kandidatlisteInfo?.antallKandidater).isEqualTo(3)
        assertThat(stillingEtterOppdatering?.kandidatlisteInfo?.kandidatlisteStatus).isEqualTo("ÅPEN")
        assertThat(stillingEtterOppdatering?.kandidatlisteInfo?.eier).isEqualTo("T123456")
    }

    @Test
    fun `Skal lese inn melding om kandidatlisteInfo fra rapid og oppdatere kandidatlisteInfo i opensearch`() {
        leggTilKafkaMiljøvariabler()
        opprettIndeks()

        testProgramMedHendelse(env, melding) {
            assertThat(size).isEqualTo(1)
        }
        // Oppdaterer kandidatlisteInfo for stilling som allerede er indeksert
        testProgramMedHendelse(env, kandidatlisteInfoMelding) {
            assertThat(size).isEqualTo(0)
        }
        testMetoderOpenSearch.refreshIndex()

        val antallDokumenter = testMetoderOpenSearch.hentAntallDokumenter("stilling_20250328")
        assertThat(antallDokumenter).isEqualTo(1)

        val rekrutteringsbistandStilling = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")
        val kandidatlisteInfo = rekrutteringsbistandStilling?.kandidatlisteInfo

        assertThat(kandidatlisteInfo).isNotNull
        assertThat(kandidatlisteInfo?.kandidatlisteId.toString()).isEqualTo("aaaabbbb-cccc-dddd-eeee-ffff00000002")
        assertThat(kandidatlisteInfo?.antallKandidater).isEqualTo(7)
        assertThat(kandidatlisteInfo?.kandidatlisteStatus).isEqualTo("LUKKET")
        assertThat(kandidatlisteInfo?.eier).isEqualTo("T654321")
    }

    @Test
    fun `Skal ikke oppdatere kandidatlisteInfo i opensearch hvis stilling ikke finnes`() {
        val mockIndexClient = mockk<IndexClient>()
        every { mockIndexClient.finnesStilling(any(), any()) } returns false

        val service = OpenSearchService(mockIndexClient, env)
        val rapid = no.nav.toi.TestRapid()
        KandidatlisteInfoLytter(rapid, service, "stilling_20250328")

        rapid.sendTestMessage(kandidatlisteInfoMelding)

        verify(exactly = 0) { mockIndexClient.oppdaterKandidatlisteInfo(any(), any(), any()) }
    }

    private fun leggTilKafkaMiljøvariabler() {
        env["KAFKA_BROKERS"] = "localhost:9092"
        env["KAFKA_SCHEMA_REGISTRY"]= "http://localhost:8081"
        env["KAFKA_SCHEMA_REGISTRY_USER"] = "user"
        env["KAFKA_SCHEMA_REGISTRY_PASSWORD"] = "pwd"
        env["USER_INFO"] = ""
        env["KAFKA_TRUSTSTORE_PATH"] = ""
        env["KAFKA_CREDSTORE_PASSWORD"]= ""
        env["KAFKA_KEYSTORE_PATH"] = ""
        env["KAFKA_CREDSTORE_PASSWORD"] = ""
    }

    private fun opprettIndeks() {
        openSearchService.opprettIndeks()    }

    private val melding = """
            {
                "stillingsId": "123e4567-e89b-12d3-a456-426614174000",
                "stillingsinfo": {
                    "eierNavident": "T123456",
                    "eierNavn": "Tester",
                    "eierNavKontorEnhetId": "1234",
                    "stillingsid": "123e4567-e89b-12d3-a456-426614174000",
                    "stillingsinfoid": "24553",
                    "stillingskategori": "STILLING"
                },
                "direktemeldtStilling": {
                    "stillingsId": "123e4567-e89b-12d3-a456-426614174000",
                    "innhold": {
                        "title": "Teststilling",
                        "administration": {
                            "status": "DONE",
                            "comments": null,
                            "reportee": "Testreporter",
                            "remarks": [],
                            "navIdent": "T123456"
                        },
                        "contactList": [],
                        "privacy": "SHOW_ALL",
                        "source": "DIR",
                        "medium": "DIR",
                        "reference": "Ref123",
                        "published": "2025-01-01T12:00:00Z",
                        "expires": "2025-12-31T12:00:00Z",
                        "employer": {
                            "name": "Testarbeidsgiver",
                            "orgnr": "123456789",
                            "parentOrgnr": "987654321",
                            "publicName": "Testarbeidsgiver AS",
                            "orgform": "AS"
                        },
                        "locationList": [
                            {
                                "address": "Testveien 1",
                                "postalCode": "1234",
                                "county": "Testfylke",
                                "municipal": "Testkommune",
                                "municipalCode": "5678",
                                "city": "Testby",
                                "country": "Norge",
                                "latitude": "59.1234",
                                "longitude": "10.1234"
                            }
                        ],
                        "categoryList": [
                            {
                                "code": "1234",
                                "categoryType": "JANZZ",
                                "name": "Utvikler",
                                "description": "Beskrivelse av kategori1",
                                "parentId": null
                            }
                        ],
                        "properties": {
                            "externalref": "1005/18"
                        },
                        "publishedByAdmin": "2023-01-01T12:00:00Z",
                        "businessName": "Testbedrift",
                        "firstPublished": true,
                        "deactivatedByExpiry": false,
                        "activationOnPublishingDate": true
                    },
                    "annonsenr": "123456789",
                    "opprettet": "2023-01-01T12:00:00Z",
                    "opprettetAv": "Testoppretter",
                    "sistEndret": "2023-01-02T12:00:00Z",
                    "sistEndretAv": "Testendrer",
                    "status": "ACTIVE"
                },
                "@event_name": "indekserDirektemeldtStilling"
            }
            """.trimIndent()

    private var stillingsinfoMelding = """
        {
            "@event_name": "indekserStillingsinfo",
            "stillingsId": "123e4567-e89b-12d3-a456-426614174000",
            "stillingsinfo": {
                "eierNavident": "T23456",
                "eierNavn": "Tester 2",
                "eierNavKontorEnhetId": "5678",
                "stillingsid": "123e4567-e89b-12d3-a456-426614174000",
                "stillingsinfoid": "24553",
                "stillingskategori": "STILLING"
            }
        }
    """.trimIndent()

    private val kandidatlisteInfoMelding = """
        {
            "@event_name": "indekserKandidatlisteInfo",
            "stillingsId": "123e4567-e89b-12d3-a456-426614174000",
            "kandidatlisteInfo": {
                "kandidatlisteId": "aaaabbbb-cccc-dddd-eeee-ffff00000002",
                "antallKandidater": 7,
                "kandidatlisteStatus": "LUKKET",
                "opprettetDato": "2024-06-01T10:00:00Z",
                "eier": "T654321"
            }
        }
    """.trimIndent()
}
