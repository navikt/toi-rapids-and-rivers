package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.log
import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient.NotifikasjonsSvar.DuplikatEksternIdOgMerkelapp
import no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner.NotifikasjonKlient.NotifikasjonsSvar.NyBeskjedVellykket
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class NotifikasjonKlient(
    val url: String,
    val hentAccessToken: () -> String,
) {

    private val secureLog = LoggerFactory.getLogger("secureLog")
    private val startDatoForNotifikasjoner =
        ZonedDateTime.of(LocalDateTime.of(2023, Month.JANUARY, 27, 13, 45), ZoneId.of("Europe/Oslo"))
    private val notifikasjonsIderTilSendteMeldinger = mutableListOf<String>()

    fun sendNotifikasjon(
        notifikasjonsId: String,
        mottakerEpostadresser: List<String>,
        stillingsId: UUID,
        virksomhetsnummer: String,
        avsender: String,
        tidspunktForHendelse: ZonedDateTime,
        meldingTilArbeidsgiver: String,
        stillingstittel: String,
    ) {

        if (tidspunktForHendelse.isBefore(startDatoForNotifikasjoner)) {
            log.info("Sender ikke notifikasjoner til arbeidsgivere for hendelser opprettet før $startDatoForNotifikasjoner")
            return
        }

        if (notifikasjonsId in notifikasjonsIderTilSendteMeldinger) {
            log.info("Sender ikke notifikasjon for $notifikasjonsId fordi lik notifikasjon allerede har blitt sendt")
            return
        }

        val epostBody = lagEpostBody(
            tittel = stillingstittel,
            tekst = meldingTilArbeidsgiver,
            avsender = avsender
        )

        val spørring =
            graphQlSpørringForCvDeltMedArbeidsgiver(
                notifikasjonsId = notifikasjonsId,
                stillingsId = stillingsId.toString(),
                virksomhetsnummer = virksomhetsnummer,
                epostBody = epostBody,
                tidspunktForVarsel = tidspunktForHendelse,
                mottakerEpostAdresser = mottakerEpostadresser,
            )

        val erLokal: Boolean = System.getenv()["NAIS_CLUSTER_NAME"] == null
        val erDev: Boolean = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false


        if(erDev)  {
            log.info("graphqlmelding (bør ikke vises i prod) ${spørring}")
        } else if(erLokal) {
            println("query: $spørring")
        }


        val (_, response, result) = Fuel
            .post(path = url)
            .header("Content-type", "application/json")
            .header("Authorization", "Bearer ${hentAccessToken()}")
            .body(spørring)
            .responseString()

        val json = jacksonObjectMapper().readTree(result.get())
        val notifikasjonsSvar = json["data"]?.get("nyBeskjed")?.get("__typename")?.asText()

        when (notifikasjonsSvar) {
            DuplikatEksternIdOgMerkelapp.name -> {
                log.info("Duplikatmelding sendt mot notifikasjon api")
                notifikasjonsIderTilSendteMeldinger.add(notifikasjonsId)
            }

            NyBeskjedVellykket.name -> {
                log.info("Melding sendt til notifikasjon-api med notifikasjonsId: $notifikasjonsId")
                notifikasjonsIderTilSendteMeldinger.add(notifikasjonsId)
            }

            else -> {
                håndterFeil(json, response, spørring)
            }
        }
    }

    private fun håndterFeil(
        json: JsonNode,
        response: Response,
        body: String,
    ) {
        secureLog.error("Feilet kall til notifikasjon-api med følgende body: $body")
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
