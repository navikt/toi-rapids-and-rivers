package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.time.ZonedDateTime
import java.util.*

class PubliserOpplysningerJobbTest {
    @Test
    fun skalPublisereOpplysningerTilRapid() {
        val rapid = TestRapid()
        val repository = mockk<Repository>()
        val meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        val leaderElector = LeaderElector("NOLEADERELECTION", HttpClient.newHttpClient())

        val jobb = PubliserOpplysningerJobb(repository, rapid, leaderElector, meterRegistry)

        val melding = lagMelding()
        jobb.publiserArbeidssøkeropplysning(melding)

        val inspektør = rapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "arbeidssokeropplysninger",
            "fodselsnummer",
            "aktørId",
            "@id",
            "@opprettet",
            "system_read_count",
            "system_participating_services"
        )

    }

    private fun lagMelding() =
        PeriodeOpplysninger(
            id=1,
            periodeId = UUID.randomUUID(),
            identitetsnummer = "01010012345",
            aktørId = "1234566789",
            periodeStartet = ZonedDateTime.now().minusMonths(2),
            periodeMottattDato = ZonedDateTime.now(),
            opplysningerMottattDato = ZonedDateTime.now(),
            helsetilstandHindrerArbeid = true,
            andreForholdHindrerArbeid = false
        )
}