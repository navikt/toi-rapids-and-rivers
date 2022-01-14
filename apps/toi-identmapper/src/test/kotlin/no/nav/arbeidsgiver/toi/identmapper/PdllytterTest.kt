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
}
