package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZonedDateTime

data class Kandidat(
    val aktørId: String,
    private val arbeidsmarkedCv: CvMelding?,
    private val oppfølgingsinformasjon: Oppfølgingsinformasjon?,
    private val oppfølgingsperiode: Oppfølgingsperiode?,
    private val arenaFritattKandidatsøk: ArenaFritattKandidatsøk?,
    private val hjemmel: Hjemmel?,
    private val måBehandleTidligereCv: MåBehandleTidligereCv?,
    private val kvp: Kvp?,
    val adressebeskyttelse: String?,
) {
    private val erAAP: Boolean
        get() = oppfølgingsinformasjon?.erAAP == true

    private val erKvp: Boolean
        get() = when {
            kvp?.event == null -> false
            kvp.event == "STARTET" -> true
            kvp.event == "AVSLUTTET" -> false
            else -> false
        }

    /**
     * Reglene for synligheten spesifiseres i denne klassen. Ved endringer i denne klassen, pass på at dokumentasjonen i microsoft loop er oppdatert med endringene.
     */
    fun toEvaluering() = Evaluering(
        harAktivCv = arbeidsmarkedCv?.meldingstype.let {
            listOf(CvMeldingstype.OPPRETT, CvMeldingstype.ENDRE).contains(it)
        },
        harJobbprofil = arbeidsmarkedCv?.endreJobbprofil != null || arbeidsmarkedCv?.opprettJobbprofil != null,
        harSettHjemmel = harSettHjemmel(),
        maaIkkeBehandleTidligereCv = måBehandleTidligereCv?.maaBehandleTidligereCv != true,
        arenaIkkeFritattKandidatsøk = arenaFritattKandidatsøk == null || !arenaFritattKandidatsøk.erFritattKandidatsøk,
        erUnderOppfoelging = erUnderOppfølging(),
        harRiktigFormidlingsgruppe = oppfølgingsinformasjon?.formidlingsgruppe in listOf(
            Formidlingsgruppe.ARBS
        ),
        erIkkeKode6eller7 = erIkkeKode6EllerKode7(),
        erIkkeSperretAnsatt = oppfølgingsinformasjon?.sperretAnsatt == false,
        erIkkeDoed = oppfølgingsinformasjon?.erDoed == false,
        erIkkeKvp = !erKvp,
        erFerdigBeregnet = beregningsgrunnlag()
    )

    private fun harSettHjemmel(): Boolean {
        return if (hjemmel != null && hjemmel.ressurs == Samtykkeressurs.CV_HJEMMEL) {
            val opprettetDato = hjemmel.opprettetDato
            val slettetDato = hjemmel.slettetDato

            opprettetDato != null && slettetDato == null
        } else {
            false
        }
    }

    private fun erUnderOppfølging(): Boolean {
        if (oppfølgingsperiode == null) return false

        val now = Instant.now()
        val startDato = oppfølgingsperiode.startDato.toInstant()
        val sluttDato = oppfølgingsperiode.sluttDato?.toInstant()
        sanityCheckOppfølging(now, this, startDato, sluttDato)
        return startDato.isBefore(now) && (sluttDato == null || sluttDato.isAfter(now))
    }

    private fun sanityCheckOppfølging(
        now: Instant?,
        kandidat: Kandidat,
        startDatoOppfølging: Instant,
        sluttDatoOppfølging: Instant?
    ) {
        if (startDatoOppfølging.isAfter(now)) {
            log("erUnderOppfølging").error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
            secureLog.error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
        }
        if (sluttDatoOppfølging?.isAfter(now) == true) {
            log("erUnderOppfølging").error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
            secureLog.error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
        }
        if(kandidat.arenaFritattKandidatsøk?.erFritattKandidatsøk == true && !kandidat.erAAP) {
            log("erUnderOppfølging").info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: se securelog")
            secureLog.info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: ${kandidat.aktørId}, fnr: ${kandidat.fødselsNummer()}, hovedmål: ${kandidat.oppfølgingsinformasjon?.hovedmaal} formidlingsgruppe: ${kandidat.oppfølgingsinformasjon?.formidlingsgruppe}, rettighetsgruppe: ${kandidat.oppfølgingsinformasjon?.rettighetsgruppe}")
        }
    }

    private fun erIkkeKode6EllerKode7(): Boolean =
        oppfølgingsinformasjon != null &&
                (oppfølgingsinformasjon.diskresjonskode == null
                        || oppfølgingsinformasjon.diskresjonskode !in listOf("6", "7"))

    private fun beregningsgrunnlag() =
        arbeidsmarkedCv != null && oppfølgingsperiode != null && oppfølgingsinformasjon != null


    companion object {
        private val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .registerModule(JavaTimeModule())

        fun fraJson(jsonMessage: JsonMessage) = mapper.readValue(jsonMessage.toJson(), Kandidat::class.java)
    }

    fun fødselsNummer() = arbeidsmarkedCv?.opprettCv?.cv?.fodselsnummer ?:
        arbeidsmarkedCv?.endreCv?.cv?.fodselsnummer ?:
        hjemmel?.fnr ?:
        oppfølgingsinformasjon?.fodselsnummer ?:
        arenaFritattKandidatsøk?.fnr
}

data class CvMelding(
    val meldingstype: CvMeldingstype,
    val opprettCv: OpprettEllerEndreCv?,
    val endreCv: OpprettEllerEndreCv?,
    val opprettJobbprofil: Any?,
    val endreJobbprofil: Any?
)

data class OpprettEllerEndreCv(
    val cv: Cv
)

data class Cv(
    val fodselsnummer: String
)

enum class CvMeldingstype {
    SLETT,
    ENDRE,
    OPPRETT
}

data class Oppfølgingsperiode(
    val startDato: ZonedDateTime,
    val sluttDato: ZonedDateTime?
)

data class Oppfølgingsinformasjon(
    val erDoed: Boolean,
    val sperretAnsatt: Boolean,
    val formidlingsgruppe: Formidlingsgruppe?,
    val diskresjonskode: Diskresjonskode?,
    val fodselsnummer: String,
    val hovedmaal: String?,
    val rettighetsgruppe: String?
) {
    val erAAP: Boolean
        get() = rettighetsgruppe == "AAP"
}

typealias Diskresjonskode = String

enum class Formidlingsgruppe {
    ARBS
}

data class ArenaFritattKandidatsøk(
    val erFritattKandidatsøk: Boolean,
    val fnr: String? // TODO: ta bort nullable når kilde hos oss er oppdatert og ferdigkjørt
)

data class Hjemmel(
    val ressurs: Samtykkeressurs?,
    val opprettetDato: ZonedDateTime?,
    val slettetDato: ZonedDateTime?,
    val fnr: String?
)

enum class Samtykkeressurs {
    CV_HJEMMEL
}

data class MåBehandleTidligereCv(
    val maaBehandleTidligereCv: Boolean
)

data class Kvp(
    val event: String
)