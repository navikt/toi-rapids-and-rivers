package no.nav.toi.stilling.indekser.eksternLytter

import io.mockk.Runs
import io.mockk.andThenJust
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.toi.TestRapid
import no.nav.toi.stilling.indekser.OpenSearchService
import no.nav.toi.stilling.indekser.dto.Stillingsinfo
import no.nav.toi.stilling.indekser.stillingsinfo.StillingsinfoClient
import org.apache.hc.core5.http.ConnectionClosedException
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class EksternStillingLytterTest {

    private val indeks = "stilling_20250328"

    private fun lytter(
        rapid: TestRapid,
        openSearchService: OpenSearchService = mockk(relaxed = true),
        stillingsinfoClient: StillingsinfoClient = mockk<StillingsinfoClient>().apply {
            every { hentStillingsinfo(any()) } returns emptyList()
        },
    ) = EksternStillingLytter(
        consumer = mockk<Consumer<String, Ad>>(),
        openSearchService = openSearchService,
        stillingsinfoClient = stillingsinfoClient,
        rapidsConnection = rapid,
    )

    private fun dirAd(uuid: String) = ad(uuid).apply { source = "DIR" }

    @Test
    fun `Publiserer ett kandidatlisteInfo-behov per indekserte eksterne stilling`() {
        val rapid = TestRapid()
        val uuid1 = UUID.randomUUID().toString()
        val uuid2 = UUID.randomUUID().toString()

        lytter(rapid).behandleStillingerMedRetry(listOf(ad(uuid1), ad(uuid2)), indeks)

        assertThat(rapid.inspektør.size).isEqualTo(2)
    }

    @Test
    fun `Behov-melding har riktig event_name, behov og stillingsId`() {
        val rapid = TestRapid()
        val uuid = UUID.randomUUID().toString()

        lytter(rapid).behandleStillingerMedRetry(listOf(ad(uuid)), indeks)

        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        assertThat(inspektør.field(0, "@event_name").asText()).isEqualTo("indekserKandidatlisteInfo")
        assertThat(inspektør.field(0, "stillingsId").asText()).isEqualTo(uuid)
        assertThat(inspektør.field(0, "@behov").map { it.asText() }).containsExactly("kandidatlisteInfo")
    }

    @Test
    fun `Publiserer med stillingsId som Kafka-nøkkel`() {
        val rapid = TestRapid()
        val uuid = UUID.randomUUID().toString()

        lytter(rapid).behandleStillingerMedRetry(listOf(ad(uuid)), indeks)

        assertThat(rapid.inspektør.key(0)).isEqualTo(uuid)
    }

    @Test
    fun `Publiserer ingen behov når det bare finnes DIR-stillinger`() {
        val rapid = TestRapid()
        val openSearchService = mockk<OpenSearchService>(relaxed = true)

        lytter(rapid, openSearchService = openSearchService)
            .behandleStillingerMedRetry(listOf(dirAd(UUID.randomUUID().toString())), indeks)

        assertThat(rapid.inspektør.size).isEqualTo(0)
        verify(exactly = 0) { openSearchService.indekser(any(), any()) }
    }

    @Test
    fun `Publiserer ingen behov for tom liste`() {
        val rapid = TestRapid()

        lytter(rapid).behandleStillingerMedRetry(emptyList(), indeks)

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `Dedupliserer slik at samme stilling gir kun ett behov`() {
        val rapid = TestRapid()
        val uuid = UUID.randomUUID().toString()

        lytter(rapid).behandleStillingerMedRetry(listOf(ad(uuid), ad(uuid)), indeks)

        assertThat(rapid.inspektør.size).isEqualTo(1)
        assertThat(rapid.inspektør.field(0, "stillingsId").asText()).isEqualTo(uuid)
    }

    @Test
    fun `Retry ved ConnectionClosedException gir ikke dupliserte behov`() {
        val rapid = TestRapid()
        val uuid = UUID.randomUUID().toString()
        val openSearchService = mockk<OpenSearchService>()
        every { openSearchService.indekser(any(), any()) } throws ConnectionClosedException() andThenJust Runs

        lytter(rapid, openSearchService = openSearchService)
            .behandleStillingerMedRetry(listOf(ad(uuid)), indeks)

        assertThat(rapid.inspektør.size).isEqualTo(1)
        assertThat(rapid.inspektør.field(0, "stillingsId").asText()).isEqualTo(uuid)
        verify(exactly = 2) { openSearchService.indekser(any(), any()) }
    }

    private fun stillingsinfo(stillingsid: String) = Stillingsinfo(
        eierNavident = null,
        eierNavn = null,
        eierNavKontorEnhetId = null,
        stillingsid = stillingsid,
        stillingsinfoid = null,
        stillingskategori = null,
    )

    private fun adOppdatert(uuid: String, updated: LocalDateTime) = ad(uuid).apply { this.updated = updated.toString() }

    @Test
    fun `Filtrerer bort stillinger med updated eldre enn fem år, og eldre enn ett år hvis de mangler stillingsinfo`() {
        val rapid = TestRapid()
        val openSearchService = mockk<OpenSearchService>(relaxed = true)
        val nå = LocalDateTime.now()

        val over5ÅrMedInfo = UUID.randomUUID().toString()
        val over1ÅrMedInfo = UUID.randomUUID().toString()
        val over1ÅrUtenInfo = UUID.randomUUID().toString()
        val under1ÅrUtenInfo = UUID.randomUUID().toString()

        val stillingsinfoClient = mockk<StillingsinfoClient>().apply {
            every { hentStillingsinfo(any()) } returns listOf(stillingsinfo(over5ÅrMedInfo), stillingsinfo(over1ÅrMedInfo))
        }

        lytter(rapid, openSearchService, stillingsinfoClient).behandleStillingerMedRetry(
            listOf(
                adOppdatert(over5ÅrMedInfo, nå.minusYears(5).minusDays(1)),
                adOppdatert(over1ÅrMedInfo, nå.minusYears(2)),
                adOppdatert(over1ÅrUtenInfo, nå.minusYears(1).minusDays(1)),
                adOppdatert(under1ÅrUtenInfo, nå.minusYears(1).plusDays(1)),
            ),
            indeks,
        )

        val forventet = listOf(over1ÅrMedInfo, under1ÅrUtenInfo)
        verify(exactly = 1) {
            openSearchService.indekser(match { it.map { s -> s.stilling.uuid.toString() } == forventet }, indeks)
        }
        assertThat((0 until rapid.inspektør.size).map { rapid.inspektør.field(it, "stillingsId").asText() })
            .containsExactlyElementsOf(forventet)
    }

    @Test
    fun `Filtrerer bort stillinger som ikke er publisert`() {
        val rapid = TestRapid()
        val openSearchService = mockk<OpenSearchService>(relaxed = true)
        val publisertUuid = UUID.randomUUID().toString()

        lytter(rapid, openSearchService).behandleStillingerMedRetry(
            listOf(
                ad(publisertUuid),
                ad(UUID.randomUUID().toString()).apply { publishedByAdmin = null },
            ),
            indeks,
        )

        verify(exactly = 1) {
            openSearchService.indekser(match { it.map { s -> s.stilling.uuid.toString() } == listOf(publisertUuid) }, indeks)
        }
        assertThat(rapid.inspektør.size).isEqualTo(1)
    }
}
