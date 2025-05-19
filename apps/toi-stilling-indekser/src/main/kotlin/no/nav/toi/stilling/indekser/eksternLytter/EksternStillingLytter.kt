package no.nav.toi.stilling.indekser.eksternLytter

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.toi.stilling.indekser.*
import no.nav.toi.stilling.indekser.stillingsinfo.StillingsinfoClient
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.errors.WakeupException
import java.io.Closeable
import java.time.Duration

class EksternStillingLytter(
    private val consumer: Consumer<String, Ad>,
    private val openSearchService: OpenSearchService,
    private val stillingsinfoClient: StillingsinfoClient
) : Closeable {

    fun start(indeks: String) {
        try {
            consumer.use {
                it.subscribe(listOf(stillingstopic))
                log.info("Starter å konsumere topic: $stillingstopic med groupId ${it.groupMetadata().groupId()} på indeks: $indeks")

                while (true) {
                    try {
                        val records: ConsumerRecords<String, Ad> = it.poll(Duration.ofSeconds(5))
                        if(records.count() == 0) continue

                        val stillinger: List<Stilling> = records.map { record -> konverterTilStilling(record.value()) }
                        val stillingsinfoer: List<Stillingsinfo> = stillingsinfoClient.hentStillingsinfo(stillinger.map { stilling -> stilling.uuid.toString() })

                        stillinger.forEach { stilling ->
                            log.info("Mottok stilling for indeksering: ${stilling.uuid}")

                            val rekrutteringsbistandStilling = RekrutteringsbistandStilling(
                                stilling = stilling,
                                stillingsinfo = stillingsinfoer.find { info -> info.stillingsid == stilling.uuid.toString() }
                            )
                            // Her skal det kun indekseres eksterne stillinger
                            if(!rekrutteringsbistandStilling.stilling.source.equals("DIR")) {
                                openSearchService.indekserStilling(rekrutteringsbistandStilling, indeks)
                            }
                        }
                        it.commitSync()
                    } catch (e: RetriableException) {
                        log.warn("Fikk en retriable exception, prøver på nytt", e)
                    }
                }
            }
        } catch (exception: WakeupException) {
            log.info("Fikk beskjed om å lukke consument med groupId ${consumer.groupMetadata().groupId()}")
        } catch (exception: Exception) {
            log.error("Noe galt skjedde i konsument", exception)
           throw Exception("Noe galt skjedde i konsument", exception)
        } finally {
            consumer.close()
        }
    }

    override fun close() {
        // Vil kaste WakeupException i konsument slik at den stopper, thread-safe.
        consumer.wakeup()
    }
}
