package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.log
import java.time.LocalDateTime
import java.util.*

class NotifikasjonKlient(val url: String) {

    fun sendNotifikasjon(
        mottakerEpost: String,
        stillingsId: UUID,
        virksomhetsnummer: String,
        avsender: String,
    ) {
        val epostBody = lagEpostBody(
            tittel = "Todo tittel",
            tekst = "Todo tekst",
            avsender = avsender
        )

        val spørring =
            graphQlSpørringForCvDeltMedArbeidsgiver(
                stillingsId = stillingsId.toString(),
                virksomhetsnummer = virksomhetsnummer,
                epostBody = epostBody,
                tidspunkt = LocalDateTime.now(),
                mottakerEpost = mottakerEpost
            )
        val (_, response, result) = Fuel.post(path = url).body(spørring).responseString()

        if(response.statusCode != 200) {
            log.error("Feilkode fra notifikasjonssystemet: ${response.statusCode}")
            throw RuntimeException("Feilkode fra notifikasjonssystemet: ${response.statusCode} ${result.get()}")
        }


        val json = jacksonObjectMapper().readTree(result.get())

        val errors = json["errors"]

        if(!errors.isNull && errors.size() > 0) {
            log.error("Feil fra notifiksjonssystemet ${errors.asText()}")
            throw RuntimeException("Feil fra notifiksjonssystemet ${errors.asText()}")

        }

    }
}
