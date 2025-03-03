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
    private val arbeidsmarkedCv: Synlighetsnode<CvMelding>,
    private val oppfølgingsinformasjon: Synlighetsnode<Oppfølgingsinformasjon>,
    private val oppfølgingsperiode: Synlighetsnode<Oppfølgingsperiode>,
    private val arenaFritattKandidatsøk: Synlighetsnode<ArenaFritattKandidatsøk>,
    private val hjemmel: Synlighetsnode<Hjemmel>,
    private val måBehandleTidligereCv: Synlighetsnode<MåBehandleTidligereCv>,
    private val kvp: Synlighetsnode<Kvp>,
    val adressebeskyttelse: Synlighetsnode<String>,
) {
    private val erAAP: Boolean
        get() = oppfølgingsinformasjon.hvisFinnesOg(Oppfølgingsinformasjon::erAAP)

    private val erKvp: Boolean
        get() = kvp.hvisFinnesOg { it.event == "STARTET" }

    /**
     * Reglene for synligheten spesifiseres i denne klassen. Ved endringer i denne klassen, pass på at dokumentasjonen i microsoft loop er oppdatert med endringene.
     */
    fun toEvaluering() = Evaluering(
        harAktivCv = arbeidsmarkedCv.hvisFinnesOg { arbeidsmarkedCv ->
            arbeidsmarkedCv.meldingstype.let {
                listOf(CvMeldingstype.OPPRETT, CvMeldingstype.ENDRE).contains(it)
            }
        },
        harJobbprofil = arbeidsmarkedCv.hvisFinnesOg {  it.endreJobbprofil != null || it.opprettJobbprofil != null },
        harSettHjemmel = harSettHjemmel(),
        maaIkkeBehandleTidligereCv = måBehandleTidligereCv.hvisNullEller{ !it.maaBehandleTidligereCv },
        arenaIkkeFritattKandidatsøk = arenaFritattKandidatsøk.hvisNullEller{ !it.erFritattKandidatsøk },
        erUnderOppfoelging = erUnderOppfølging(),
        harRiktigFormidlingsgruppe = oppfølgingsinformasjon.hvisFinnesOg {
            it.formidlingsgruppe == Formidlingsgruppe.ARBS
        },
        erIkkeKode6eller7 = erIkkeKode6EllerKode7(),
        erIkkeSperretAnsatt = oppfølgingsinformasjon.hvisFinnesOg { !it.sperretAnsatt },
        erIkkeDoed = oppfølgingsinformasjon.hvisFinnesOg { !it.erDoed },
        erIkkeKvp = !erKvp,
        erFerdigBeregnet = beregningsgrunnlag()
    )

    private fun harSettHjemmel() = hjemmel.hvisFinnesOg { hjemmel ->
        val opprettetDato = hjemmel.opprettetDato
        val slettetDato = hjemmel.slettetDato
        hjemmel.ressurs == Samtykkeressurs.CV_HJEMMEL && opprettetDato != null && slettetDato == null
    }

    private fun erUnderOppfølging() = oppfølgingsperiode.hvisFinnesOg { oppfølgingsperiode ->
        val now = Instant.now()
        val startDato = oppfølgingsperiode.startDato.toInstant()
        val sluttDato = oppfølgingsperiode.sluttDato?.toInstant()
        sanityCheckOppfølging(now, this, startDato, sluttDato)
        startDato.isBefore(now) && (sluttDato == null || sluttDato.isAfter(now))
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
        if(kandidat.arenaFritattKandidatsøk.hvisFinnesOg { it.erFritattKandidatsøk  } && !kandidat.erAAP) {
            log("erUnderOppfølging").info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: se securelog")
            secureLog.info("kandidat er fritatt for kandidatsøk, men har ikke aap Aktørid: ${kandidat.aktørId}, fnr: ${kandidat.fødselsNummer()}, hovedmål: ${kandidat.oppfølgingsinformasjon} formidlingsgruppe: ${kandidat.oppfølgingsinformasjon}, rettighetsgruppe: ${kandidat.oppfølgingsinformasjon}")
        }
    }

    private fun erIkkeKode6EllerKode7(): Boolean = oppfølgingsinformasjon.hvisFinnesOg { oppfølgingsinformasjon ->
        (oppfølgingsinformasjon.diskresjonskode == null
                || oppfølgingsinformasjon.diskresjonskode !in listOf("6", "7"))
    }

    private fun beregningsgrunnlag() = listOf(arbeidsmarkedCv, oppfølgingsinformasjon, oppfølgingsperiode,
        arenaFritattKandidatsøk, hjemmel, måBehandleTidligereCv, kvp).all { it.svarPåDetteFeltetLiggerPåHendelse() }


    companion object {
        private val mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .registerModule(JavaTimeModule())

        fun fraJson(jsonMessage: JsonMessage): Kandidat {
            val json = mapper.readTree(jsonMessage.toJson())
            return Kandidat(
                aktørId = json["aktørId"].asText(),
                arbeidsmarkedCv = Synlighetsnode.fromJsonNode(json.path("arbeidsmarkedCv")),
                oppfølgingsinformasjon = Synlighetsnode.fromJsonNode(json.path("oppfølgingsinformasjon")),
                oppfølgingsperiode = Synlighetsnode.fromJsonNode(json.path("oppfølgingsperiode")),
                arenaFritattKandidatsøk = Synlighetsnode.fromJsonNode(json.path("arenaFritattKandidatsøk")),
                hjemmel = Synlighetsnode.fromJsonNode(json.path("hjemmel")),
                måBehandleTidligereCv = Synlighetsnode.fromJsonNode(json.path("måBehandleTidligereCv")),
                kvp = Synlighetsnode.fromJsonNode(json.path("kvp")),
                adressebeskyttelse = Synlighetsnode.fromJsonNode(json.path("adressebeskyttelse"))
            )
        }
    }

    fun fødselsNummer() =
        arbeidsmarkedCv.verdiEllerNull()?.opprettCv?.cv?.fodselsnummer ?:
        arbeidsmarkedCv.verdiEllerNull()?.endreCv?.cv?.fodselsnummer ?:
        hjemmel.verdiEllerNull()?.fnr ?:
        oppfølgingsinformasjon.verdiEllerNull()?.fodselsnummer ?:
        arenaFritattKandidatsøk.verdiEllerNull()?.fnr
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