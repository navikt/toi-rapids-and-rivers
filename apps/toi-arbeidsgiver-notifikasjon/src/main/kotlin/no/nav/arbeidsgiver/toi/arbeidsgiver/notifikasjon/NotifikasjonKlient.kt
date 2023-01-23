package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.log
import java.time.LocalDateTime
import java.util.*

class NotifikasjonKlient(
    val url: String,
    val lagTidspunktForVarsel: () -> LocalDateTime = { LocalDateTime.now() },
    val hentAccessToken: () -> String
) {

    fun sendNotifikasjon(
        notifikasjonsId: String,
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
                notifikasjonsId = notifikasjonsId,
                stillingsId = stillingsId.toString(),
                virksomhetsnummer = virksomhetsnummer,
                epostBody = epostBody,
                tidspunktForVarsel = lagTidspunktForVarsel(),
                mottakerEpost = mottakerEpost
            )

        val (_, response, result) = Fuel
            .post(path = url)
            .header("Authorization", "Bearer ${hentAccessToken()}")
            .body(spørring)
            .responseString()

        if (response.statusCode != 200) {
            log.error("Feilkode fra notifikasjonssystemet: ${response.statusCode}")
            throw RuntimeException("Feilkode fra notifikasjonssystemet: ${response.statusCode} ${result.get()}")
        }

        val json = jacksonObjectMapper().readTree(result.get())
        val errors = json["errors"]

        if (errors != null && errors.size() > 0) {
            log.error("Feil fra notifiksjonssystemet ${errors.asText()}")
            throw RuntimeException("Feil fra notifiksjonssystemet ${errors.asText()}")
        }

        val varVellykket = json["data"]?.get("nyBeskjed")?.get("__typename")?.asText() == "NyBeskjedVellykket"
        if (!varVellykket) {
            throw RuntimeException("Kall mot notifikasjon-api feilet, men fikk 200 OK uten errors i responsen")
        }
    }
}