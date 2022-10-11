package no.nav.arbeidsgiver.toi.arbeidsmarked.cv

import no.nav.arbeid.cv.avro.Cv
import no.nav.arbeid.cv.avro.Foererkort
import no.nav.arbeid.cv.avro.FoererkortKlasse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class CvLytterTest {

    val cvTopic = TopicPartition("teampam.cv-endret-ekstern-v2", 0)

    @Disabled
    @Test
    fun `lesing av cv-meldinger fra topic skal publiseres på rapid`() {
        val cv = cv()
        val consumer = mockConsumer()
        val rapid = TestRapid()
        val cvLytter = CvLytter(consumer, behandleCv)

        produserCvMelding(consumer, cv)
        cvLytter.onReady(rapid)

        Thread.sleep(300)
        val inspektør = rapid.inspektør
        Assertions.assertThat(inspektør.size).isEqualTo(1)

        val meldingJson = inspektør.message(0)

        Assertions.assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "cv",
            "aktørId",
            "system_read_count"
        )

        Assertions.assertThat(meldingJson.get("aktørId")).isNotNull
    }

    private fun mockConsumer() = MockConsumer<String, Cv>(OffsetResetStrategy.EARLIEST).apply {
        schedulePollTask {
            rebalance(listOf(cvTopic))
            updateBeginningOffsets(mapOf(Pair(cvTopic, 0)))
        }
    }

    private fun produserCvMelding(consumer: MockConsumer<String, Cv>, cv: Cv, offset: Long = 0) {
        val record = ConsumerRecord(
            cvTopic.topic(),
            cvTopic.partition(),
            offset,
            cv.aktoerId,
            cv
        )
        consumer.schedulePollTask {
            consumer.addRecord(record)
        }
    }

    private fun cv() = Cv().apply() {
        aktoerId = "123"
        sistEndret = Instant.now()
        opprettet = Instant.now()
        foedselsdato = LocalDate.of(1992, 1, 11)
        synligForArbeidsgiver = true
        synligForVeileder = true
        val foererkortErvervetDato = LocalDate.of(2010, 11,5)
        foererkort = Foererkort(listOf(FoererkortKlasse("B", "Førerkort klasse B", foererkortErvervetDato, foererkortErvervetDato.plusYears(80))))
        arbeidserfaring = emptyList()
        utdannelse = emptyList()
        fagdokumentasjon = emptyList()
        godkjenninger = emptyList()
        kurs = emptyList()
        sertifikat = emptyList()
        annenErfaring = emptyList()
        spraakferdigheter = emptyList()
    }
}