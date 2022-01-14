package no.nav.arbeidsgiver.toi.identmapper

import no.nav.arbeidsgiver.toi.cv.PdlLytterConfiguration
import no.nav.person.pdl.aktor.v2.Aktor
import no.nav.person.pdl.aktor.v2.Identifikator
import no.nav.person.pdl.aktor.v2.Type
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition


fun mottaAktorMelding(consumer: MockConsumer<String, Aktor>, aktor: Aktor, offset: Long = 0) {
    val record = ConsumerRecord(
        pdlTopic.topic(),
        pdlTopic.partition(),
        offset,
        aktor.getIdentifikatorer().filter { it.getType()== Type.AKTORID }.map { it.getIdnummer() }.first(),
        aktor,
    )

    consumer.schedulePollTask {
        consumer.addRecord(record)
    }
}

val pdlTopic = TopicPartition(PdlLytterConfiguration.topicName, 0)

fun mockConsumer() = MockConsumer<String, Aktor>(OffsetResetStrategy.EARLIEST).apply {
    schedulePollTask {
        rebalance(listOf(pdlTopic))
        updateBeginningOffsets(mapOf(Pair(pdlTopic, 0)))
    }
}

fun aktor(aktørId: String, fødselsnummer: String) = Aktor(listOf(
    Identifikator(aktørId, Type.AKTORID, true),
    Identifikator(fødselsnummer, Type.FOLKEREGISTERIDENT, true)
))