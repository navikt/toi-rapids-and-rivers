package no.nav.arbeidsgiver.toi.identmapper

import TestDatabase
import no.nav.arbeidsgiver.toi.cv.PdlLytterConfiguration
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PdllytterTest {

    private val testDatabase: TestDatabase = TestDatabase()

    @AfterEach
    fun slettDatabase() {
        testDatabase.slettAlt()
    }

    @Test
    fun `Lesing av pdlMelding fra eksternt topic skal lagres i cache`() {

        val testAktørId = "123456789"
        val testFnr = "987654321"
        var lagreFunksjonHarBlittKalt = false

        val lagreAssert: (String?, String) -> Unit = { aktørId, fødselsnummer ->
            assertThat(aktørId).isEqualTo(testAktørId)
            assertThat(fødselsnummer).isEqualTo(testFnr)
            lagreFunksjonHarBlittKalt = true
        }

        val consumer = mockConsumer()
        val pdlytter = PdlLytter(consumer,lagreAssert)
        val rapid = TestRapid()
        val melding = aktor(testAktørId, testFnr)

        mottaAktorMelding(consumer, melding)
        pdlytter.onReady(rapid)

        Thread.sleep(300)
        assertThat(lagreFunksjonHarBlittKalt).isTrue()
    }

    private fun aktor(aktørId: String, fødselsnummer: String) = Aktor(listOf(
        Identifikator(aktørId, Type.AKTORID, true),
        Identifikator(fødselsnummer, Type.FOLKEREGISTERIDENT, true)
    ))

    private fun lagre(aktørId: String?, fnr: String) {
        testDatabase.lagreIdentMapping(
            IdentMapping(
                aktørId = aktørId,
                fødselsnummer = fnr,
                cachetTidspunkt = LocalDateTime.now()
            )
        )
    }
}
private fun mottaAktorMelding(consumer: MockConsumer<String, Aktor>, aktor: Aktor, offset: Long = 0) {
    val record = ConsumerRecord(
        topic.topic(),
        topic.partition(),
        offset,
        aktor.getIdentifikatorer().filter { it.getType()==Type.AKTORID }.map { it.getIdnummer() }.first(),
        aktor,
    )

    consumer.schedulePollTask {
        consumer.addRecord(record)
    }
}

private val topic = TopicPartition(PdlLytterConfiguration.topicName, 0)

private fun mockConsumer() = MockConsumer<String, Aktor>(OffsetResetStrategy.EARLIEST).apply {
    schedulePollTask {
        rebalance(listOf(topic))
        updateBeginningOffsets(mapOf(Pair(topic, 0)))
    }
}
