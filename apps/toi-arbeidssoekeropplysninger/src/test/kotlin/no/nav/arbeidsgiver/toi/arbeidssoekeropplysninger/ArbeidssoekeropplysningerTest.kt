package no.nav.arbeidsgiver.toi.arbeidssoekeropplysninger

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class ArbeidssoekeropplysningerTest {
    val arbeidssokeropplysningerTopic = TopicPartition("paw.opplysninger-om-arbeidssoeker-v1", 0)

    val behandleMelding: (OpplysningerOmArbeidssoeker) -> ArbeidssokerOpplysninger = { melding ->
        ArbeidssokerOpplysninger(melding, PrometheusMeterRegistry(PrometheusConfig.DEFAULT))
    }

    @Test
    fun `lesing av arbeidssøkeropplysninger fra topic skal publiseres på rapid`() {
        val melding = melding()
        val consumer = mockConsumer()
        val rapid = TestRapid()
        val arbeidssoekeropplysningerLytter = ArbeidssoekeropplysningerLytter({ consumer }, behandleMelding)

        produserArbeidssoekeropplysningerMelding(consumer, melding)
        arbeidssoekeropplysningerLytter.onReady(rapid)

        Thread.sleep(600)
        val inspektør = rapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "id",
            "@event_name",
            "periodeId",
            "helsetilstandHindrerArbeid",
            "andreForholdHindrerArbeid",
            "@id",
            "@opprettet",
            "system_read_count",
            "system_participating_services"
        )

        assertThat(meldingJson.get("periodeId")).isNotNull
    }

    private fun mockConsumer() = MockConsumer<Long, OpplysningerOmArbeidssoeker>(OffsetResetStrategy.EARLIEST).apply {
        schedulePollTask {
            rebalance(listOf(arbeidssokeropplysningerTopic))
            updateBeginningOffsets(mapOf(Pair(arbeidssokeropplysningerTopic, 0)))
        }
    }

    private fun produserArbeidssoekeropplysningerMelding(consumer: MockConsumer<Long, OpplysningerOmArbeidssoeker>, melding: OpplysningerOmArbeidssoeker, offset: Long = 0) {
        val record = ConsumerRecord(
            arbeidssokeropplysningerTopic.topic(),
            arbeidssokeropplysningerTopic.partition(),
            offset,
            (kotlin.random.Random(System.currentTimeMillis())).nextLong(),
            melding
        )
        consumer.schedulePollTask {
            consumer.addRecord(record)
        }
    }

    private fun melding() = OpplysningerOmArbeidssoeker.newBuilder()
        .setId(UUID.randomUUID())
        .setPeriodeId(UUID.randomUUID())
        .setAnnet(Annet.newBuilder()
            .setAndreForholdHindrerArbeid(JaNeiVetIkke.JA)
            .build()
        )
        .setHelse(Helse.newBuilder()
            .setHelsetilstandHindrerArbeid(JaNeiVetIkke.VET_IKKE)
            .build()
        )
        .setSendtInnAv(Metadata.newBuilder()
            .setUtfoertAv(Bruker.newBuilder().setId("junit").setType(BrukerType.SYSTEM).build())
            .setKilde("junit")
            .setTidspunkt(Instant.now())
            .setAarsak("test")
            .build()
        )
        .setJobbsituasjon(Jobbsituasjon.newBuilder()
            .setBeskrivelser(emptyList())
            .build()
        )
        .build()
}
