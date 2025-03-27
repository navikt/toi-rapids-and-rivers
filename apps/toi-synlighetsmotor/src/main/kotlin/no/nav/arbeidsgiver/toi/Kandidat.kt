package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.Instant
import java.time.ZonedDateTime

data class Kandidat(
    val aktørId: String,
    private val arbeidsmarkedCv: Synlighetsnode<CvMelding>,
    private val oppfølgingsinformasjon: Synlighetsnode<Oppfølgingsinformasjon>,
    private val oppfølgingsperiode: Synlighetsnode<Oppfølgingsperiode>,
    private val arenaFritattKandidatsøk: Synlighetsnode<ArenaFritattKandidatsøk>,
    private val hjemmel: Synlighetsnode<Hjemmel>,
    private val måBehandleTidligereCv: Synlighetsnode<MåBehandleTidligereCv>,
    private val kvp: Synlighetsnode<Kvp>,
    val adressebeskyttelse: Synlighetsnode<String>,
) {
    private val erAAP: BooleanVerdi
        get() = oppfølgingsinformasjon.hvisIkkeNullOg(Oppfølgingsinformasjon::erAAP)

    private val erKvp: BooleanVerdi
        get() = kvp.hvisIkkeNullOg { it.event == "STARTET" }

    /**
     * Reglene for synligheten spesifiseres i denne klassen. Ved endringer i denne klassen, pass på at dokumentasjonen i microsoft loop er oppdatert med endringene.
     */
    fun toEvaluering() = Evaluering(
        harAktivCv = arbeidsmarkedCv.hvisIkkeNullOg(::harAktivCv),
        harJobbprofil = arbeidsmarkedCv.hvisIkkeNullOg(::harJobbprofil),
        harSettHjemmel = hjemmel.hvisIkkeNullOg(::harSettHjemmel),
        maaIkkeBehandleTidligereCv = måBehandleTidligereCv.hvisNullEller(::maaIkkeBehandleTidligereCv),
        arenaIkkeFritattKandidatsøk = arenaFritattKandidatsøk.hvisNullEller(::erIkkeArenaFritattKandidatsøk),
        erUnderOppfoelging = oppfølgingsperiode.hvisIkkeNullOg(::erUnderOppfølging),
        harRiktigFormidlingsgruppe = oppfølgingsinformasjon.hvisIkkeNullOg(::harRiktigFormidlingsgruppe),
        erIkkeKode6eller7 = oppfølgingsinformasjon.hvisIkkeNullOg(::erIkkeKode6EllerKode7),
        erIkkeSperretAnsatt = oppfølgingsinformasjon.hvisIkkeNullOg(::erIkkeSperretAnsatt),
        erIkkeDoed = oppfølgingsinformasjon.hvisIkkeNullOg(::erIkkeDød),
        erIkkeKvp = !erKvp,
        harIkkeAdressebeskyttelse = adressebeskyttelse.hvisIkkeNullOg(::harIkkeAdressebeskyttelse),
        komplettBeregningsgrunnlag = beregningsgrunnlag()
    )

    private fun maaIkkeBehandleTidligereCv(it: MåBehandleTidligereCv) = !it.maaBehandleTidligereCv
    private fun erIkkeArenaFritattKandidatsøk(it: ArenaFritattKandidatsøk) = !it.erFritattKandidatsøk
    private fun harRiktigFormidlingsgruppe(it: Oppfølgingsinformasjon) = it.formidlingsgruppe == Formidlingsgruppe.ARBS
    private fun erIkkeSperretAnsatt(it: Oppfølgingsinformasjon) = !it.sperretAnsatt
    private fun erIkkeDød(it: Oppfølgingsinformasjon) = !it.erDoed
    private fun harAktivCv(arbeidsmarkedCv: CvMelding) = arbeidsmarkedCv.meldingstype.let {
        listOf(CvMeldingstype.OPPRETT, CvMeldingstype.ENDRE).contains(it)
    }

    private fun harJobbprofil(cvMelding: CvMelding) =
        cvMelding.endreJobbprofil != null || cvMelding.opprettJobbprofil != null

    private fun harSettHjemmel(hjemmel: Hjemmel) =
        hjemmel.ressurs == Samtykkeressurs.CV_HJEMMEL &&
                hjemmel.opprettetDato != null &&
                hjemmel.slettetDato == null

    private fun erUnderOppfølging(oppfølgingsperiode: Oppfølgingsperiode): Boolean {
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
            log.error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
            secureLog.error("startdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
        }
        if (sluttDatoOppfølging?.isAfter(now) == true) {
            log.error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: se secure log")
            secureLog.error("sluttdato for oppfølgingsperiode er frem i tid. Det håndterer vi ikke, vi har ingen egen trigger. Aktørid: ${kandidat.aktørId}")
        }
        if (kandidat.arenaFritattKandidatsøk.hvisIkkeNullOg { it.erFritattKandidatsøk }
                .default(false) && (!kandidat.erAAP).default(false)) {
            log.info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: se securelog")
            secureLog.info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: ${kandidat.aktørId}, fnr: ${kandidat.fødselsNummer()}, hovedmål: ${kandidat.oppfølgingsinformasjon} formidlingsgruppe: ${kandidat.oppfølgingsinformasjon}, rettighetsgruppe: ${kandidat.oppfølgingsinformasjon}")
        }
    }

    private fun erIkkeKode6EllerKode7(oppfølgingsinformasjon: Oppfølgingsinformasjon): Boolean =
        (oppfølgingsinformasjon.diskresjonskode == null
                || oppfølgingsinformasjon.diskresjonskode !in listOf("6", "7"))

    private fun harIkkeAdressebeskyttelse(adressebeskyttelse: String) =
        adressebeskyttelse == "UKJENT" || adressebeskyttelse == "UGRADERT"


    private fun beregningsgrunnlag() = listOf(
        arbeidsmarkedCv, oppfølgingsinformasjon, oppfølgingsperiode,
        arenaFritattKandidatsøk, hjemmel, måBehandleTidligereCv, kvp, adressebeskyttelse
    ).all { it.svarPåDetteFeltetLiggerPåHendelse() }


    companion object {
        private val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .registerModule(JavaTimeModule())

        fun fraJson(jsonMessage: JsonMessage): Kandidat {
            val json = mapper.readTree(jsonMessage.toJson())
            return Kandidat(
                aktørId = json["aktørId"].asText(),
                arbeidsmarkedCv = Synlighetsnode.fromJsonNode(json.path("arbeidsmarkedCv"), mapper),
                oppfølgingsinformasjon = Synlighetsnode.fromJsonNode(json.path("oppfølgingsinformasjon"), mapper),
                oppfølgingsperiode = Synlighetsnode.fromJsonNode(json.path("oppfølgingsperiode"), mapper),
                arenaFritattKandidatsøk = Synlighetsnode.fromJsonNode(json.path("arenaFritattKandidatsøk"), mapper),
                hjemmel = Synlighetsnode.fromJsonNode(json.path("hjemmel"), mapper),
                måBehandleTidligereCv = Synlighetsnode.fromJsonNode(json.path("måBehandleTidligereCv"), mapper),
                kvp = Synlighetsnode.fromJsonNode(json.path("kvp"), mapper),
                adressebeskyttelse = Synlighetsnode.fromJsonNode(json.path("adressebeskyttelse"), mapper)
            )
        }
    }

    fun fødselsNummer() =
        arbeidsmarkedCv.verdiEllerNull()?.opprettCv?.cv?.fodselsnummer
            ?: arbeidsmarkedCv.verdiEllerNull()?.endreCv?.cv?.fodselsnummer ?: hjemmel.verdiEllerNull()?.fnr
            ?: oppfølgingsinformasjon.verdiEllerNull()?.fodselsnummer ?: arenaFritattKandidatsøk.verdiEllerNull()?.fnr
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
