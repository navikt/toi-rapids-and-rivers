package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.contains
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeserfaring.Companion.totalYrkeserfaringIManeder
import no.nav.pam.geography.PostDataDAO
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

private const val jobbskifter = "JOBBS"
private const val ingenAnonymitet = "G"
private const val tomAdresse = ""
private val ingenMobiltelefon: String? = null
private val ingenNasjonalitet: String? = null
private val ingenLandkode: String? = null
private val ingenDisponererBil: Boolean? = null
private const val erIkkeDød = false
private const val defaultFrKode = "0"
private const val harIkkeGjeldende14aVedtak = "HAR_IKKE_GJELDENDE_14A_VEDTAK"
private val fritattAgKandidatsokerDeprikert: Boolean? = null

@JsonIgnoreProperties(ignoreUnknown = true)
class EsCv(
    private val aktorId: String,
    private val fodselsnummer: String,
    private val fornavn: String,
    private val etternavn: String,
    private val fodselsdato: LocalDate,

    private val fodselsdatoErDnr: Boolean,
    private val formidlingsgruppekode: String,
    private val epostadresse: String,
    private val mobiltelefon: String?,
    private val telefon: String,

    private val statsborgerskap: String?,
    private val kandidatnr: String,
    private val beskrivelse: String,
    private val samtykkeStatus: String,
    private val samtykkeDato: OffsetDateTime,

    private val adresselinje1: String,
    private val adresselinje2: String,
    private val adresselinje3: String,
    private val postnummer: String,
    private val poststed: String,

    private val landkode: String?,
    private val kommunenummer: Int,
    private val disponererBil: Boolean?,
    private val tidsstempel: OffsetDateTime,  // TODO Dårlig/vagt navn, bør hete sistEndret, slik at det stemmer med CV AVRO-modellen
    private val kommunenummerkw: Int,

    private val doed: Boolean,
    private val frKode: String?,
    private val kvalifiseringsgruppekode: String?,
    private val hovedmaalkode: String?,
    private val hovedmal: String?,
    private val innsatsgruppe: String?,
    private val navkontor: String?,
    private val orgenhet: String?,

    private val fritattKandidatsok: Boolean?,
    private val fritattAgKandidatsok: Boolean?,
    private val synligForArbeidsgiverSok: Boolean,
    private val synligForVeilederSok: Boolean,
    private val oppstartKode: String?,

    private val kommunenummerstring: String,
    veilederIdent: String,
    private val veilederVisningsnavn: String?,
    private val veilederEpost: String?,
    private val fylkeNavn: String?,
    private val kommuneNavn: String?,

    private val utdanning: List<EsUtdanning>,
    private val fagdokumentasjon: List<EsFagdokumentasjon>,
    private val yrkeserfaring: List<EsYrkeserfaring>,
    private val kompetanseObj: List<EsKompetanse>,
    private val annenerfaringObj: List<EsAnnenErfaring>,
    private val sertifikatObj: List<EsSertifikat>,
    private val forerkort: List<EsForerkort>,
    private val sprak: List<EsSprak>,
    private val kursObj: List<EsKurs>,
    private val vervObj: List<EsVerv>,
    private val geografiJobbonsker: List<EsGeografiJobbonsker>,
    private val yrkeJobbonskerObj: List<EsYrkeJobbonsker>,
    private val omfangJobbonskerObj: List<EsOmfangJobbonsker>,
    private val ansettelsesformJobbonskerObj: List<EsAnsettelsesformJobbonsker>,
    private val arbeidstidsordningJobbonskerObj: List<EsArbeidstidsordningJobbonsker>,
    private val arbeidsdagerJobbonskerObj: List<EsArbeidsdagerJobbonsker>,
    private val arbeidstidJobbonskerObj: List<EsArbeidstidJobbonsker>,
    private val godkjenninger: List<EsGodkjenning>,
    private val perioderMedInaktivitet: EsPerioderMedInaktivitet?
) {
    private val fritekst: String? = null
    private val isHarKontaktinformasjon: Boolean = listOfNotNull(epostadresse, mobiltelefon, telefon).any(String::isNotBlank)
    private val arenaKandidatnr: String = kandidatnr

    private val totalLengdeYrkeserfaring = yrkeserfaring.totalYrkeserfaringIManeder()

    private val veilederIdent = veilederIdent.lowercase(Locale.getDefault())

    @Deprecated("")
    private val veileder = veilederIdent

    private val samletKompetanseObj: List<EsSamletKompetanse> = listOfNotNull(fagdokumentasjon, kompetanseObj, sertifikatObj, godkjenninger)
        .flatMap { it.flatMap(EnAvFlereSamledeKompetaser::tilSamletKompetanse) }

    override fun toString(): String {
        return StringJoiner(", ", EsCv::class.java.getSimpleName() + "[", "]")
            .add("fritekst='" + fritekst + "'")
            .add("aktorId='" + aktorId + "'")
            .add("fodselsnummer='" + fodselsnummer + "'")
            .add("fornavn='" + fornavn + "'")
            .add("etternavn='" + etternavn + "'")
            .add("fodselsdato=" + fodselsdato)
            .add("fodselsdatoErDnr=" + fodselsdatoErDnr)
            .add("formidlingsgruppekode='" + formidlingsgruppekode + "'")
            .add("epostadresse='" + epostadresse + "'")
            .add("mobiltelefon='" + mobiltelefon + "'")
            .add("harKontaktinformasjon=" + this.isHarKontaktinformasjon)
            .add("telefon='" + telefon + "'")
            .add("statsborgerskap='" + statsborgerskap + "'")
            .add("kandidatnr='" + kandidatnr + "'")
            .add("arenaKandidatnr='" + arenaKandidatnr + "'")
            .add("beskrivelse='" + beskrivelse + "'")
            .add("samtykkeStatus='" + samtykkeStatus + "'")
            .add("samtykkeDato=" + samtykkeDato)
            .add("adresselinje1='" + adresselinje1 + "'")
            .add("adresselinje2='" + adresselinje2 + "'")
            .add("adresselinje3='" + adresselinje3 + "'")
            .add("postnummer='" + postnummer + "'")
            .add("poststed='" + poststed + "'")
            .add("landkode='" + landkode + "'")
            .add("kommunenummer=" + kommunenummer)
            .add("kommunenummerkw=" + kommunenummerkw)
            .add("kommunenummerstring='" + kommunenummerstring + "'")
            .add("fylkeNavn='" + fylkeNavn + "'")
            .add("kommuneNavn='" + kommuneNavn + "'")
            .add("disponererBil=" + disponererBil)
            .add("tidsstempel=" + tidsstempel)
            .add("doed=" + this.doed)
            .add("frKode='" + frKode + "'")
            .add("kvalifiseringsgruppekode='" + kvalifiseringsgruppekode + "'")
            .add("hovedmaalkode='" + hovedmaalkode + "'")
            .add("hovedmal='" + hovedmal + "'")
            .add("innsatsgruppe='" + innsatsgruppe + "'")
            .add("orgenhet='" + orgenhet + "'")
            .add("navkontor='" + navkontor + "'")
            .add("fritattKandidatsok=" + this.fritattKandidatsok)
            .add("fritattAgKandidatsok=" + this.fritattAgKandidatsok)
            .add("utdanning=" + utdanning)
            .add("fagdokumentasjon=" + fagdokumentasjon)
            .add("yrkeserfaring=" + yrkeserfaring)
            .add("kompetanseObj=" + kompetanseObj)
            .add("annenerfaringObj=" + annenerfaringObj)
            .add("sertifikatObj=" + sertifikatObj)
            .add("forerkort=" + forerkort)
            .add("sprak=" + sprak)
            .add("kursObj=" + kursObj)
            .add("vervObj=" + vervObj)
            .add("geografiJobbonsker=" + geografiJobbonsker)
            .add("yrkeJobbonskerObj=" + yrkeJobbonskerObj)
            .add("omfangJobbonskerObj=" + omfangJobbonskerObj)
            .add("ansettelsesformJobbonskerObj=" + ansettelsesformJobbonskerObj)
            .add("arbeidstidsordningJobbonskerObj=" + arbeidstidsordningJobbonskerObj)
            .add("arbeidsdagerJobbonskerObj=" + arbeidsdagerJobbonskerObj)
            .add("arbeidstidJobbonskerObj=" + arbeidstidJobbonskerObj)
            .add("samletKompetanseObj=" + samletKompetanseObj)
            .add("totalLengdeYrkeserfaring=" + totalLengdeYrkeserfaring)
            .add("synligForArbeidsgiverSok=" + synligForArbeidsgiverSok)
            .add("synligForVeilederSok=" + synligForVeilederSok)
            .add("oppstartKode='" + oppstartKode + "'")
            .add("veileder='" + veileder + "'")
            .add("veilederIdent='" + veilederIdent + "'")
            .add("veilederVisningsnavn='" + veilederVisningsnavn + "'")
            .add("veilederEpost='" + veilederEpost + "'")
            .add("godkjenninger=" + godkjenninger)
            .add("perioderMedInaktivitet=" + perioderMedInaktivitet)
            .toString()
    }

    fun indekseringsnøkkel() = kandidatnr

    companion object {
        private fun JsonNode.yyyymmddToLocalDate(): LocalDate {
            val (year, month, day) = map { it.asInt() }
            return LocalDate.of(year, month, day)
        }
        private val postDataDAO = PostDataDAO()

        fun fraMelding(packet: JsonMessage): EsCv {
            val arbeidsmarkedCv = packet["arbeidsmarkedCv"]
            val cvNode = listOf("slettCv", "endreCv", "opprettCv").first { arbeidsmarkedCv.hasNonNull(it) }.let { arbeidsmarkedCv[it] }["cv"]
            val jobbProfilNode = listOf("slettJobbprofil", "endreJobbprofil", "opprettJobbprofil").first { arbeidsmarkedCv.hasNonNull(it) }.let { arbeidsmarkedCv[it] }["jobbprofil"]

            val postData = postDataDAO.findPostData(cvNode["postnummer"].asText())
            val kommunenummer = postData.map { it.municipality.code }.orElse(null)
            val fylkeNavn = postData.map { it.county.capitalizedName}.orElse(null)
            val kommuneNavn = postData.map { it.municipality.capitalizedName }.orElse(null)
            return EsCv(
                aktorId = packet["aktørId"].asText(null),
                fodselsnummer = cvNode["fodselsnummer"].asText(null),
                fornavn = cvNode["fornavn"].asText(null),
                etternavn = cvNode["etternavn"].asText(null),
                fodselsdato = cvNode["foedselsdato"].yyyymmddToLocalDate(),
                fodselsdatoErDnr = cvNode["fodselsnummer"].asText(null).first() >= '4',
                formidlingsgruppekode = packet["oppfølgingsinformasjon.formidlingsgruppe"].asText(jobbskifter),
                epostadresse = cvNode["epost"].asText(null),
                mobiltelefon = ingenMobiltelefon,
                telefon = cvNode["telefon"].asText(null),
                statsborgerskap = ingenNasjonalitet,
                kandidatnr = cvNode["arenaKandidatnr"].asText(null),
                beskrivelse = cvNode["sammendrag"].asText(null),
                samtykkeStatus = ingenAnonymitet,
                samtykkeDato = Instant.ofEpochMilli((cvNode["opprettet"].asDouble() * 1000).toLong())
                    .atOffset(ZoneOffset.UTC), // samtykkeDato, XXX hvorfor heter ikke feltet opprettetDato i EsCV ?
                adresselinje1 = cvNode["gateadresse"].asText(null),
                adresselinje2 = tomAdresse,
                adresselinje3 = tomAdresse,
                postnummer = cvNode["postnummer"].asText(null),
                poststed = cvNode["poststed"].asText(null),
                landkode = ingenLandkode,
                kommunenummer = kommunenummer.toInt(),
                disponererBil = ingenDisponererBil,
                tidsstempel = Instant.ofEpochMilli((cvNode["sistEndret"].asDouble() * 1000).toLong())
                    .atOffset(ZoneOffset.UTC),
                kommunenummerkw = kommunenummer.toInt(),
                doed = erIkkeDød,
                frKode = defaultFrKode,
                kvalifiseringsgruppekode = packet["oppfølgingsinformasjon.kvalifiseringsgruppe"].asText(""),
                hovedmaalkode = packet["oppfølgingsinformasjon.hovedmaal"].asText(""),
                hovedmal = packet["siste14avedtak.hovedmaal"].asText(harIkkeGjeldende14aVedtak),
                innsatsgruppe = packet["siste14avedtak.innsatsgruppe"].asText(harIkkeGjeldende14aVedtak),
                navkontor = packet["organisasjonsenhetsnavn"].asText(null),
                orgenhet = packet["oppfølgingsinformasjon.oppfolgingsenhet"].asText(""),
                fritattKandidatsok = packet["fritattKandidatsøk.fritattKandidatsok"].let { if(it.isMissingOrNull()) null else it.asBoolean() },
                fritattAgKandidatsok = fritattAgKandidatsokerDeprikert,
                synligForArbeidsgiverSok = cvNode["synligForArbeidsgiver"].asBoolean(),
                synligForVeilederSok = cvNode["synligForVeileder"].asBoolean(),
                oppstartKode = jobbProfilNode.asText(""),
                kommunenummerstring = kommunenummer,
                veilederIdent = packet["veileder.veilederId"].asText(null),
                veilederVisningsnavn = packet["veileder.veilederinformasjon.visningsNavn"].asText(null),
                veilederEpost = packet["veileder.veilederinformasjon.epost"].asText(null),
                fylkeNavn = fylkeNavn,
                kommuneNavn = kommuneNavn,
                utdanning = TODO(),
                fagdokumentasjon = TODO(),
                yrkeserfaring = TODO(),
                kompetanseObj = TODO(),
                annenerfaringObj = TODO(),
                sertifikatObj = TODO(),
                forerkort = TODO(),
                sprak = TODO(),
                kursObj = TODO(),
                vervObj = TODO(),
                geografiJobbonsker = TODO(),
                yrkeJobbonskerObj = TODO(),
                omfangJobbonskerObj = TODO(),
                ansettelsesformJobbonskerObj = TODO(),
                arbeidstidsordningJobbonskerObj = TODO(),
                arbeidsdagerJobbonskerObj = TODO(),
                arbeidstidJobbonskerObj = TODO(),
                godkjenninger = TODO(),
                perioderMedInaktivitet = TODO()
            )
        }
    }
}
