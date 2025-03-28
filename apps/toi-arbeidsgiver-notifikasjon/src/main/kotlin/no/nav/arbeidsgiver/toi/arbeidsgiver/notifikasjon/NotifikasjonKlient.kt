package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class NotifikasjonKlient(
    val url: String,
    val hentAccessToken: () -> String,
) {
    private val startDatoForNotifikasjoner =
        ZonedDateTime.of(LocalDateTime.of(2023, Month.JANUARY, 27, 13, 45), ZoneId.of("Europe/Oslo"))

    fun opprettSak(
        stillingsId: UUID,
        stillingstittel: String,
        organisasjonsnummer: String
    ) {
        val query = queryOpprettNySak(
            stillingsId,
            stillingstittel,
            organisasjonsnummer
        )

        try {
            val (_, response, result) = Fuel
                .post(path = url)
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer ${hentAccessToken()}")
                .body(query)
                .responseString()

            val json = jacksonObjectMapper().readTree(result.get())
            val notifikasjonsSvar = json["data"]?.get("nySak")?.get("__typename")?.asText()

            when (notifikasjonsSvar) {
                NySakSvar.NySakVellykket.name -> {
                    log.info("Sak opprettet hos notifikasjon-api for stilling: $stillingsId")
                }

                NySakSvar.DuplikatGrupperingsid.name -> {
                    log.info("Sak ikke opprettet hos notifikasjon-api fordi det allerede finnes en sak med stillingsId: $stillingsId")
                }

                else -> {
                    håndterFeil(json, response, query)
                }
            }
        } catch (e: Throwable) {
            log.error("Uventet feil i kall til notifikasjon-api med body: (se secureLog)")
            secureLog.error("Uventet feil i kall til notifikasjon-api med body: $query", e)
            throw e
        }
    }

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

        val epostBody = lagEpostBody(
            tittel = stillingstittel,
            tekst = meldingTilArbeidsgiver,
            avsender = avsender
        )

        val query =
            queryOpprettNyBeskjed(
                notifikasjonsId = notifikasjonsId,
                stillingsId = stillingsId.toString(),
                virksomhetsnummer = virksomhetsnummer,
                epostBody = epostBody,
                tidspunktForVarsel = tidspunktForHendelse,
                mottakerEpostAdresser = mottakerEpostadresser,
            )

        val erLokal: Boolean = System.getenv()["NAIS_CLUSTER_NAME"] == null
        val erDev: Boolean = System.getenv()["NAIS_CLUSTER_NAME"]?.equals("dev-gcp") ?: false


        if (erDev) {
            log.info("graphqlmelding (bør ikke vises i prod), se securelog for detaljer")
            secureLog.info("graphqlmelding (bør ikke vises i prod) ${query}")
        } else if (erLokal) {
            println("query: $query")
        }

        try {
            val (_, response, result) = Fuel
                .post(path = url)
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer ${hentAccessToken()}")
                .body(query)
                .responseString()

            val json = jacksonObjectMapper().readTree(result.get())
            val notifikasjonsSvar = json["data"]?.get("nyBeskjed")?.get("__typename")?.asText()

            when (notifikasjonsSvar) {
                NyBeskjedSvar.DuplikatEksternIdOgMerkelapp.name -> {
                    log.info("Duplikatmelding sendt mot notifikasjon api")
                }

                NyBeskjedSvar.NyBeskjedVellykket.name -> {
                    log.info("Melding sendt til notifikasjon-api med notifikasjonsId: $notifikasjonsId")
                }

                else -> {
                    håndterFeil(json, response, query)
                }
            }
        } catch (e: Throwable) {
            log.error("Uventet feil i kall til notifikasjon-api med body: (se secureLog)")
            secureLog.error("Uventet feil i kall til notifikasjon-api med body: $query", e)
            throw e
        }
    }

    fun ferdigstillSak(stillingsId: UUID) {
        val query = queryFerdigstillSak(
            stillingsId
        )

        try {
            val (_, response, result) = Fuel
                .post(path = url)
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer ${hentAccessToken()}")
                .body(query)
                .responseString()

            val json = jacksonObjectMapper().readTree(result.get())
            val svar = json["data"]?.get("nyStatusSakByGrupperingsid")?.get("__typename")?.asText()

            when (svar) {
                NyStatusSakSvar.NyStatusSakVellykket.name -> {
                    log.info("Sak fullført hos notifikasjon-api for stilling: $stillingsId")
                }

                NyStatusSakSvar.SakFinnesIkke.name -> {
                    log.info("Forsøkte å lukke sak, men sak med stillingsId $stillingsId finnes ikke i notifikasjon-api")
                }

                NyStatusSakSvar.Konflikt.name -> {
                    log.error("Forsøkte å lukke sak, men fikk konflikt på stillingsId $stillingsId")
                }

                else -> {
                    håndterFeil(json, response, query)
                }
            }
        } catch (e: Throwable) {
            log.error("Uventet feil i kall til notifikasjon-api med body: (se secureLog)")
            secureLog.error("Uventet feil i kall til notifikasjon-api med body: $query", e)
            throw e
        }
    }

    fun slettSak(stillingsId: UUID) {
        val query = querySlettSak(
            stillingsId
        )

        try {
            val (_, response, result) = Fuel
                .post(path = url)
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer ${hentAccessToken()}")
                .body(query)
                .responseString()

            val json = jacksonObjectMapper().readTree(result.get())
            val svar = json["data"]?.get("hardDeleteSakByGrupperingsid")?.get("__typename")?.asText()

            when (svar) {
                SlettSakSvar.HardDeleteSakVellykket.name -> {
                    log.info("Sak slettet hos notifikasjon-api for stilling: $stillingsId")
                }

                SlettSakSvar.SakFinnesIkke.name -> {
                    log.info("Forsøkte å slette sak, men sak med stillingsId $stillingsId finnes ikke i notifikasjon-api")
                }

                else -> {
                    håndterFeil(json, response, query)
                }
            }
        } catch (e: Throwable) {
            log.error("Uventet feil i kall til notifikasjon-api med body: (se secureLog)")
            secureLog.error("Uventet feil i kall til notifikasjon-api med body: $query", e)
            throw e
        }
    }

    private fun håndterFeil(
        json: JsonNode,
        response: Response,
        body: String,
    ) {
        log.error("Feilet kall til notifikasjon-api med følgende body: (se securelog)")
        secureLog.error("Feilet kall til notifikasjon-api med følgende body: $body")
        val errors = json["errors"]
        if (errors != null && errors.size() > 0) {
            log.error("Feil fra notifikasjon api, errors: $errors}")
        }
        throw RuntimeException("Kall mot notifikasjon-api feilet, statuskode: ${response.statusCode}")
    }

    enum class NyBeskjedSvar {
        NyBeskjedVellykket,
        DuplikatEksternIdOgMerkelapp,
    }

    enum class NySakSvar {
        NySakVellykket,
        DuplikatGrupperingsid
    }

    enum class NyStatusSakSvar {
        NyStatusSakVellykket,
        SakFinnesIkke,
        Konflikt
    }

    enum class SlettSakSvar {
        HardDeleteSakVellykket,
        SakFinnesIkke
    }
}
