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
            assertThat(size).isEqualTo(0)
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
    fun `Skal legge inn melding fra rapid inn i OpenSearch`() {
        leggTilKafkaMiljøvariabler()
        opprettIndeks()

        testProgramMedHendelse(env, melding) {
            assertThat(size).isEqualTo(0)
        }

        testMetoderOpenSearch.refreshIndex()
        val antallDokumenter = testMetoderOpenSearch.hentAntallDokumenter("stilling_20250328")

        assertThat(antallDokumenter).isEqualTo(1)

        val rekrutteringsbistandStilling = testMetoderOpenSearch.finnRekrutteringsbistandStilling("123e4567-e89b-12d3-a456-426614174000", "stilling_20250328")

        assertThat(rekrutteringsbistandStilling?.stilling?.uuid.toString()).isEqualTo("123e4567-e89b-12d3-a456-426614174000")
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
}
