package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Objects

@JsonIgnoreProperties(ignoreUnknown = true)
class EsYrkeserfaring(
    private val fraDato: YearMonth,
    private val tilDato: YearMonth?,
    private val arbeidsgiver: String,
    private val styrkKode: String?,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) private val stillingstittel: String,
    private val stillingstitlerForTypeahead: Set<String>,
    private val alternativStillingstittel: String,
    private val organisasjonsnummer: String?,
    private val naceKode: String?,
    private val utelukketForFremtiden: Boolean,
    sokeTitler: List<String>,
    private val sted: String,
    private val beskrivelse: String?
) {
    private val styrkKode4Siffer =  (styrkKode?.let { (if (it.length <= 3) null else it.substring(0, 4)) })
    private val styrkKode3Siffer = (styrkKode?.let { (if (styrkKode.length <= 2) null else styrkKode.substring(0, 3)) })
    private val sokeTitler: List<String> = sokeTitler + stillingstittel
    private val yrkeserfaringManeder = toYrkeserfaringManeder(fraDato, tilDato)

    constructor(
        fraDato: YearMonth,
        tilDato: YearMonth?,
        arbeidsgiver: String,
        styrkKode: String,
        kodeverkStillingstittel: String,
        stillingstitlerForTypeahead: Set<String>,
        alternativStillingstittel: String,
        beskrivelse: String,
        sokeTitler: List<String>,
        sted: String
    ) : this(
        fraDato,
        tilDato,
        arbeidsgiver,
        styrkKode,
        kodeverkStillingstittel,
        stillingstitlerForTypeahead,
        alternativStillingstittel,
        null,
        null,
        false,
        sokeTitler,
        sted, beskrivelse
    )
    constructor(
        fraDato: YearMonth,
        tilDato: YearMonth?,
        arbeidsgiver: String,
        styrkKode: String?,
        stillingstittel: String,
        stillingstitlerForTypeahead: Set<String>,
        alternativStillingstittel: String,
        organisasjonsnummer: String?,
        naceKode: String?,
        utelukketForFremtiden: Boolean,
        sokeTitler: List<String>,
        sted: String,
    ): this(
        fraDato,
        tilDato,
        arbeidsgiver,
        styrkKode,
        stillingstittel,
        stillingstitlerForTypeahead,
        alternativStillingstittel,
        organisasjonsnummer,
        naceKode,
        utelukketForFremtiden,
        sokeTitler,
        sted, null
    )


    override fun equals(other: Any?) = other is EsYrkeserfaring && fraDato == other.fraDato && tilDato == other.tilDato
            && arbeidsgiver == other.arbeidsgiver
            && styrkKode == other.styrkKode
            && stillingstittel == other.stillingstittel
            && stillingstitlerForTypeahead == other.stillingstitlerForTypeahead
            && alternativStillingstittel == other.alternativStillingstittel
            && beskrivelse == other.beskrivelse
            && organisasjonsnummer == other.organisasjonsnummer
            && naceKode == other.naceKode
            && yrkeserfaringManeder == other.yrkeserfaringManeder
            && sted == other.sted
            && utelukketForFremtiden == other.utelukketForFremtiden

    override fun hashCode() = Objects.hash(
        fraDato, tilDato, arbeidsgiver, styrkKode, stillingstittel,
        stillingstitlerForTypeahead, alternativStillingstittel, beskrivelse, organisasjonsnummer, naceKode,
        yrkeserfaringManeder, utelukketForFremtiden, sted
    )

    override fun toString() = ("EsYrkeserfaring{" + "fraDato=" + fraDato + ", tilDato=" + tilDato
            + ", arbeidsgiver='" + arbeidsgiver + '\'' + ", styrkKode='" + styrkKode + '\''
            + ", stillingstittel='" + stillingstittel + '\''
            + ", alternativStillingstittel='" + alternativStillingstittel + '\''
            + ", beskrivelse='" + beskrivelse + '\'' + ", organisasjonsnummer='"
            + organisasjonsnummer + '\'' + ", naceKode='" + naceKode + '\''
            + ", yrkeserfaringManeder='" + yrkeserfaringManeder + '\''
            + ", sted='" + sted + '\''
            + ", utelukketForFremtiden='" + utelukketForFremtiden + '\'' + '}')

    companion object {
        private fun toYrkeserfaringManeder(fraDato: YearMonth, tilDato: YearMonth?) =
            ChronoUnit.MONTHS.between(fraDato, tilDato?: now()).toInt()
        fun List<EsYrkeserfaring>.totalYrkeserfaringIManeder() = this.sumOf(EsYrkeserfaring::yrkeserfaringManeder)
        fun fraMelding(packet: JsonMessage, cvNode: JsonNode): List<EsYrkeserfaring> = cvNode["arbeidserfaring"].map { arbeidserfaringNode ->
            val stillingstittel = arbeidserfaringNode["stillingstittel"].asText()
            EsYrkeserfaring(
                fraDato = arbeidserfaringNode["fraTidspunkt"].asText(null).let(YearMonth::parse),
                tilDato = arbeidserfaringNode["tilTidspunkt"].asText(null)?.let(YearMonth::parse),
                arbeidsgiver = arbeidserfaringNode["arbeidsgiver"].asText(),
                styrkKode = arbeidserfaringNode["styrkkode"].asText(),
                kodeverkStillingstittel = stillingstittel,
                stillingstitlerForTypeahead = TODO(),
                alternativStillingstittel = arbeidserfaringNode["stillingstittelFritekst"].asText("").let { if(it=="") stillingstittel else it },
                beskrivelse = arbeidserfaringNode["beskrivelse"].asText(null),
                sokeTitler = TODO(),
                sted = arbeidserfaringNode["sted"].asText("")
            )
        }
    }
}
