package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.log
import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient.NotifikasjonsSvar.DuplikatEksternIdOgMerkelapp
import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient.NotifikasjonsSvar.NyBeskjedVellykket
import java.time.ZonedDateTime
import java.util.*

class NotifikasjonKlient(
    val url: String,
    val hentAccessToken: () -> String,
) {

    fun sendNotifikasjon(
        notifikasjonsId: String,
        mottakerEpost: String,
        stillingsId: UUID,
        virksomhetsnummer: String,
        avsender: String,
        tidspunktForHendelse: ZonedDateTime,
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
                tidspunktForVarsel = tidspunktForHendelse,
                mottakerEpost = mottakerEpost
            )

        log.info("TMP: Melding til notifikasjonssystemet: $spørring")

        // TODO: Legg på call-ID
        val (_, response, result) = Fuel
            .post(path = url)
            .header("Content-type", "application/json")
            .header("Authorization", "Bearer ${hentAccessToken()}")
            .body(spørring)
            .responseString()

        if (response.statusCode == 200) {
            log.info("Sendte notifikasjon til arbeidsgiver via notifikasjon-produsent-api og fikk 200 OK men er det riktig?")
        }
        if (response.statusCode != 200) {
            log.error("Feilkode fra notifikasjonssystemet: ${response.statusCode}")
        }

        val json = jacksonObjectMapper().readTree(result.get())

        val notifikasjonsSvar = json["data"]?.get("nyBeskjed")?.get("__typename")?.asText()

        when (notifikasjonsSvar) {
            DuplikatEksternIdOgMerkelapp.name -> {
                log.info("Duplikatmelding sendt mot notifikasjon api")
            }
            NyBeskjedVellykket.name -> {
                log.info("Melding sendt til notifikasjon api")
            }
            else -> {
                håndterFeil(json, response)
            }
        }

        if (response.statusCode == 200) {
            log.info("Sendte notifikasjon til arbeidsgiver via notifikasjon-produsent-api og fikk 200 OK men er det riktig?")
        }
    }

    private fun håndterFeil(
        json: JsonNode,
        response: Response,
    ) {
        val errors = json["errors"]
        if (errors != null && errors.size() > 0) {
            log.error("Feil fra notifikasjon api, errors: $errors}")
        }
        throw RuntimeException("Kall mot notifikasjon-api feilet, statuskode: ${response.statusCode}")
    }

    enum class NotifikasjonsSvar {
        NyBeskjedVellykket,
        DuplikatEksternIdOgMerkelapp,
    }
}
