package no.nav.toi.stilling.indekser.eksternLytter

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.toi.stilling.indekser.*
import no.nav.toi.stilling.indekser.stillingsinfo.KunneIkkeHenteStillingsinsinfoException
import no.nav.toi.stilling.indekser.stillingsinfo.StillingsinfoClient
import org.apache.hc.core5.http.ConnectionClosedException
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

                        val ads = records.map { record -> record.value() }
                        behandleStillingerMedRetry(ads, indeks)
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

    fun behandleStillingerMedRetry(stillinger: List<Ad>, indeks: String) {
        try {
            behandleStillinger(stillinger, indeks)
        } catch (e: ConnectionClosedException) {
            log.warn("Feil ved kall mot Open Search, prøver igjen", e)
            behandleStillinger(stillinger, indeks)
        } catch (e: KunneIkkeHenteStillingsinsinfoException) {
            log.warn("Feil ved henting av stillingsinfo, prøver igjen", e)
            behandleStillinger(stillinger, indeks)
        }
    }

    private fun behandleStillinger(ads: List<Ad>, indeks: String) {
        val alleMeldinger = ads.map { konverterTilStilling(it) }
        val stillinger = beholdSisteMeldingPerStilling(alleMeldinger)
        val stillingsinfo = stillingsinfoClient.hentStillingsinfo(stillinger.map { it.uuid.toString() })

        stillinger.forEach { stilling ->
            log.info("Mottok stilling for indeksering: ${stilling.uuid}")

            val rekrutteringsbistandStilling = RekrutteringsbistandStilling(
                stilling = stilling,
                stillingsinfo = stillingsinfo.find { info -> info.stillingsid == stilling.uuid.toString() }
            )
            // Her skal det kun indekseres eksterne stillinger
            if(!rekrutteringsbistandStilling.stilling.source.equals("DIR")) {
                openSearchService.indekserStilling(rekrutteringsbistandStilling, indeks)
            }
        }
        log.info("Indekserte ${stillinger.size} stillinger i indeks '$indeks'. UUIDer: ${stillinger.map { it.uuid }}")
    }

    private fun beholdSisteMeldingPerStilling(stillinger: List<Stilling>) =
        stillinger.associateBy { it.uuid }.values.toList()
}
