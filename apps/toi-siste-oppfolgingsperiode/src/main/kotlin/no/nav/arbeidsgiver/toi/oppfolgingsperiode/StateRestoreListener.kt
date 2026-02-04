package no.nav.arbeidsgiver.toi.oppfolgingsperiode

import org.apache.kafka.common.TopicPartition
/*import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.processor.StateRestoreListener

class StateRestoreListener(private val kafkaState: () -> KafkaStreams.State): StateRestoreListener {

    private val partisjonerUnderRestoring = mutableMapOf<TopicPartition, Boolean>()

    fun isReady() = kafkaState() == KafkaStreams.State.RUNNING && restoreringErFerdig()

    private fun restoreringErFerdig(): Boolean {
        return partisjonerUnderRestoring.isNotEmpty() && partisjonerUnderRestoring.values.none { it }
    }

    override fun onRestoreStart(
        topicPartition: TopicPartition,
        storeName: String?,
        startingOffset: Long,
        endingOffset: Long
    ) {
        log.info("Starter restoring av topicPartition: $topicPartition for store: $storeName fra offset $startingOffset til $endingOffset")
        partisjonerUnderRestoring[topicPartition] = true
    }


    override fun onRestoreEnd(
        topicPartition: TopicPartition,
        storeName: String?,
        totalRestored: Long
    ) {
        log.info("Ferdig med restoring av topicPartition: $topicPartition for store: $storeName. Totalt restaurert: $totalRestored")
        partisjonerUnderRestoring[topicPartition] = false
    }

    override fun onBatchRestored(
        topicPartition: TopicPartition?,
        storeName: String?,
        batchEndOffset: Long,
        numRestored: Long
    ) {}
}*/