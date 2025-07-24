package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeserfaring.Companion.totalYrkeserfaringIManeder
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class EsCv(
    private val aktorId: String,
    private val fodselsnummer: String,
    private val fornavn: String,
    private val etternavn: String,
    private val fodselsdato: String,

    private val fodselsdatoErDnr: Boolean,
    private val formidlingsgruppekode: String,
    private val epostadresse: String,
    private val mobiltelefon: String,
    private val telefon: String,

    private val statsborgerskap: String,
    private val kandidatnr: String,
    private val beskrivelse: String,
    private val samtykkeStatus: String,
    private val samtykkeDato: Date,

    private val adresselinje1: String,
    private val adresselinje2: String,
    private val adresselinje3: String,
    private val postnummer: String,
    private val poststed: String,

    private val landkode: String,
    private val kommunenummer: Int,
    private val disponererBil: Boolean,
    private val tidsstempel: Date,  // TODO Dårlig/vagt navn, bør hete sistEndret, slik at det stemmer med CV AVRO-modellen
    private val kommunenummerkw: Int,

    private val doed: Boolean,
    private val frKode: String?,
    private val kvalifiseringsgruppekode: String,
    private val hovedmaalkode: String?,
    private val hovedmal: String?,
    private val innsatsgruppe: String?,
    private val navkontor: String,
    private val orgenhet: String,

    private val fritattKandidatsok: Boolean,
    private val fritattAgKandidatsok: Boolean,
    private val synligForArbeidsgiverSok: Boolean,
    private val synligForVeilederSok: Boolean,
    private val oppstartKode: String?,

    private val kommunenummerstring: String,
    veilederIdent: String,
    private val veilederVisningsnavn: String,
    private val veilederEpost: String,
    private val fylkeNavn: String,
    private val kommuneNavn: String,

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
    private val perioderMedInaktivitet: EsPerioderMedInaktivitet
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
}
