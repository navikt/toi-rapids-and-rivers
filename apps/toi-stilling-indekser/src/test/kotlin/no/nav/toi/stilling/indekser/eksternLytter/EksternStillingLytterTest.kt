package no.nav.toi.stilling.indekser.eksternLytter

import io.mockk.Runs
import io.mockk.andThenJust
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.toi.TestRapid
import no.nav.toi.stilling.indekser.OpenSearchService
import no.nav.toi.stilling.indekser.stillingsinfo.StillingsinfoClient
import org.apache.hc.core5.http.ConnectionClosedException
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
    fun `Publiserer med stillingsId som Kafka-nokkel`() {
        val rapid = TestRapid()
        val uuid = UUID.randomUUID().toString()

        lytter(rapid).behandleStillingerMedRetry(listOf(ad(uuid)), indeks)

        assertThat(rapid.inspektør.key(0)).isEqualTo(uuid)
    }

    @Test
    fun `Publiserer ingen behov nar det bare finnes DIR-stillinger`() {
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
}
