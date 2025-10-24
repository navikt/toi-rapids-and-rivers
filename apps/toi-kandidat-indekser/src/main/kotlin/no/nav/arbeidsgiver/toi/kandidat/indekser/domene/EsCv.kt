package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.arbeidsgiver.toi.kandidat.indekser.geografi.PostDataKlient
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeserfaring.Companion.totalYrkeserfaringIManeder
import no.nav.arbeidsgiver.toi.kandidat.indekser.geografi.GeografiKlient
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

private const val indekseringsnøkkel = "arenaKandidatnr"

@JsonIgnoreProperties(ignoreUnknown = true)
class EsCv(
    @field:JsonProperty private val aktorId: String,
    @field:JsonProperty private val fodselsnummer: String,
    @field:JsonProperty private val fornavn: String,
    @field:JsonProperty private val etternavn: String,
    @field:JsonDeserialize(using = LocalDateDeserializer::class)
    @field:JsonSerialize(using = LocalDateSerializer::class)
    @field:JsonProperty private val fodselsdato: LocalDate,

    @field:JsonProperty private val fodselsdatoErDnr: Boolean,
    @field:JsonProperty private val formidlingsgruppekode: String,
    @field:JsonProperty private val epostadresse: String?,
    @field:JsonProperty private val mobiltelefon: String?,
    @field:JsonProperty private val telefon: String?,

    @field:JsonProperty private val statsborgerskap: String?,
    @field:JsonProperty private val kandidatnr: String,
    @field:JsonProperty private val beskrivelse: String,
    @field:JsonProperty private val samtykkeStatus: String,
    @field:JsonProperty private val samtykkeDato: OffsetDateTime,

    @field:JsonProperty private val adresselinje1: String?,
    @field:JsonProperty private val adresselinje2: String,
    @field:JsonProperty private val adresselinje3: String,
    @field:JsonProperty private val postnummer: String?,
    @field:JsonProperty private val poststed: String?,

    @field:JsonProperty private val landkode: String?,
    @field:JsonProperty private val kommunenummer: Int?,
    @field:JsonProperty private val disponererBil: Boolean?,
    @field:JsonProperty private val tidsstempel: OffsetDateTime,  // TODO Dårlig/vagt navn, bør hete sistEndret, slik at det stemmer med CV AVRO-modellen
    @field:JsonProperty private val kommunenummerkw: Int?,

    @field:JsonProperty private val doed: Boolean,
    @field:JsonProperty private val frKode: String?,
    @field:JsonProperty private val kvalifiseringsgruppekode: String?,
    @field:JsonProperty private val hovedmaalkode: String?,
    @field:JsonProperty private val hovedmal: String?,
    @field:JsonProperty private val innsatsgruppe: String?,
    @field:JsonProperty private val navkontor: String?,
    @field:JsonProperty private val orgenhet: String?,

    @field:JsonProperty private val fritattKandidatsok: Boolean?,
    @field:JsonProperty private val fritattAgKandidatsok: Boolean?,
    @field:JsonProperty private val synligForArbeidsgiverSok: Boolean,
    @field:JsonProperty private val synligForVeilederSok: Boolean,
    @field:JsonProperty private val oppstartKode: String?,

    @field:JsonProperty private val kommunenummerstring: String?,
    veilederIdent: String?,
    @field:JsonProperty private val veilederVisningsnavn: String?,
    @field:JsonProperty private val veilederEpost: String?,
    @field:JsonProperty private val fylkeNavn: String?,
    @field:JsonProperty private val kommuneNavn: String?,

    @field:JsonProperty private val utdanning: List<EsUtdanning>,
    @field:JsonProperty private val fagdokumentasjon: List<EsFagdokumentasjon>,
    @field:JsonProperty private val yrkeserfaring: List<EsYrkeserfaring>,
    @field:JsonProperty private val kompetanseObj: List<EsKompetanse>,
    @field:JsonProperty private val annenerfaringObj: List<EsAnnenErfaring>,
    @field:JsonProperty private val sertifikatObj: List<EsSertifikat>,
    @field:JsonProperty private val forerkort: List<EsForerkort>,
    @field:JsonProperty private val sprak: List<EsSprak>,
    @field:JsonProperty private val kursObj: List<EsKurs>,
    @field:JsonProperty private val geografiJobbonsker: List<EsGeografiJobbonsker>,
    @field:JsonProperty private val yrkeJobbonskerObj: List<EsYrkeJobbonsker>,
    @field:JsonProperty private val omfangJobbonskerObj: List<EsOmfangJobbonsker>,
    @field:JsonProperty private val ansettelsesformJobbonskerObj: List<EsAnsettelsesformJobbonsker>,
    @field:JsonProperty private val arbeidstidsordningJobbonskerObj: List<EsArbeidstidsordningJobbonsker>,
    @field:JsonProperty private val arbeidsdagerJobbonskerObj: List<EsArbeidsdagerJobbonsker>,
    @field:JsonProperty private val arbeidstidJobbonskerObj: List<EsArbeidstidJobbonsker>,
    @field:JsonProperty private val godkjenninger: List<EsGodkjenning>,
    @field:JsonProperty private val perioderMedInaktivitet: EsPerioderMedInaktivitet?
) {
    @field:JsonProperty private val harKontaktinformasjon: Boolean = listOfNotNull(epostadresse, mobiltelefon, telefon).any(String::isNotBlank)
    @field:JsonProperty private val arenaKandidatnr: String = kandidatnr

    @field:JsonProperty private val totalLengdeYrkeserfaring = yrkeserfaring.totalYrkeserfaringIManeder()

    @field:JsonProperty private val veilederIdent = veilederIdent?.lowercase(Locale.getDefault())
    @field:JsonProperty private val vervObj = emptyList<Any>()

    @Deprecated("")
    @field:JsonProperty
    private val veileder = veilederIdent

    @field:JsonProperty private val samletKompetanseObj: List<EsSamletKompetanse> = listOfNotNull(fagdokumentasjon, kompetanseObj, sertifikatObj, godkjenninger)
        .flatMap { it.flatMap(EnAvFlereSamledeKompetaser::tilSamletKompetanse) }

    override fun toString(): String {
        return StringJoiner(", ", EsCv::class.java.getSimpleName() + "[", "]")
            .add("aktorId='" + aktorId + "'")
            .add("fodselsnummer='" + fodselsnummer + "'")
            .add("fornavn='" + fornavn + "'")
            .add("etternavn='" + etternavn + "'")
            .add("fodselsdato=" + fodselsdato)
            .add("fodselsdatoErDnr=" + fodselsdatoErDnr)
            .add("formidlingsgruppekode='" + formidlingsgruppekode + "'")
            .add("epostadresse='" + epostadresse + "'")
            .add("mobiltelefon='" + mobiltelefon + "'")
            .add("harKontaktinformasjon=" + this.harKontaktinformasjon)
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

        fun indekseringsnøkkel(packet: JsonMessage) = if(packet["arbeidsmarkedCv"].isMissingOrNull()) null else
            getCvNode(packet["arbeidsmarkedCv"])[indekseringsnøkkel].asText(null)

        fun fraMelding(packet: JsonMessage, postDataKlient: PostDataKlient, geografiKlient: GeografiKlient): EsCv {
            val arbeidsmarkedCv = packet["arbeidsmarkedCv"]
            val cvNode = getCvNode(arbeidsmarkedCv)
            val jobbProfilNode = listOf("slettJobbprofil", "endreJobbprofil", "opprettJobbprofil").first { arbeidsmarkedCv.hasNonNull(it) }.let { arbeidsmarkedCv[it] }["jobbprofil"]

            val postData = postDataKlient.findPostData(cvNode["postnummer"].asText())
            val kommunenummer = postData?.kommune?.kommunenummer
            val fylkeNavn = postData?.fylke?.korrigertNavn
            val kommuneNavn = postData?.kommune?.korrigertNavn
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
                kandidatnr = cvNode[indekseringsnøkkel].asText(null),
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
                kommunenummer = kommunenummer?.toInt(),
                disponererBil = ingenDisponererBil,
                tidsstempel = Instant.ofEpochMilli((cvNode["sistEndret"].asDouble() * 1000).toLong())
                    .atOffset(ZoneOffset.ofHours(0)),
                kommunenummerkw = kommunenummer?.toInt(),
                doed = erIkkeDød,
                frKode = defaultFrKode,
                kvalifiseringsgruppekode = packet["oppfølgingsinformasjon.kvalifiseringsgruppe"].asText(""),
                hovedmaalkode = packet["oppfølgingsinformasjon.hovedmaal"].asText(""),
                hovedmal = packet["siste14avedtak.hovedmal"].asText(harIkkeGjeldende14aVedtak),
                innsatsgruppe = packet["siste14avedtak.innsatsgruppe"].asText(harIkkeGjeldende14aVedtak),
                navkontor = packet["organisasjonsenhetsnavn"].asText(null),
                orgenhet = packet["oppfølgingsinformasjon.oppfolgingsenhet"].asText(""),
                fritattKandidatsok = packet["fritattKandidatsøk.fritattKandidatsok"].let { if(it.isMissingOrNull()) null else it.asBoolean() },
                fritattAgKandidatsok = fritattAgKandidatsokerDeprikert,
                synligForArbeidsgiverSok = cvNode["synligForArbeidsgiver"].asBoolean(),
                synligForVeilederSok = cvNode["synligForVeileder"].asBoolean(),
                oppstartKode = jobbProfilNode["oppstartKode"].asText(""),
                kommunenummerstring = kommunenummer,
                veilederIdent = packet["veileder.veilederId"].asText(null),
                veilederVisningsnavn = packet["veileder.veilederinformasjon.visningsNavn"].asText(null),
                veilederEpost = packet["veileder.veilederinformasjon.epost"].asText(null),
                fylkeNavn = fylkeNavn,
                kommuneNavn = kommuneNavn,
                utdanning = EsUtdanning.fraMelding(cvNode),
                fagdokumentasjon = EsFagdokumentasjon.fraMelding(cvNode),
                yrkeserfaring = EsYrkeserfaring.fraMelding(packet, cvNode),
                kompetanseObj = EsKompetanse.fraMelding(jobbProfilNode, packet),
                annenerfaringObj = EsAnnenErfaring.fraMelding(cvNode),
                sertifikatObj = EsSertifikat.fraMelding(cvNode),
                forerkort = EsForerkort.fraMelding(cvNode),
                sprak = EsSprak.fraMelding(cvNode),
                kursObj = EsKurs.fraMelding(cvNode),
                geografiJobbonsker = EsGeografiJobbonsker.fraMelding(jobbProfilNode, geografiKlient),
                yrkeJobbonskerObj = EsYrkeJobbonsker.fraMelding(jobbProfilNode, packet),
                omfangJobbonskerObj = EsOmfangJobbonsker.fraMelding(jobbProfilNode),
                ansettelsesformJobbonskerObj = EsAnsettelsesformJobbonsker.fraMelding(jobbProfilNode),
                arbeidstidsordningJobbonskerObj = EsArbeidstidsordningJobbonsker.fraMelding(jobbProfilNode),
                arbeidsdagerJobbonskerObj = EsArbeidsdagerJobbonsker.fraMelding(jobbProfilNode),
                arbeidstidJobbonskerObj = EsArbeidstidJobbonsker.fraMelding(jobbProfilNode),
                godkjenninger = EsGodkjenning.fraMelding(cvNode),
                perioderMedInaktivitet = EsPerioderMedInaktivitet.fraMelding(packet)
            ).apply { require(indekseringsnøkkel() == indekseringsnøkkel(packet)) }
        }

        private fun getCvNode(arbeidsmarkedCv: JsonNode): JsonNode =
            listOf("slettCv", "endreCv", "opprettCv").first { arbeidsmarkedCv.hasNonNull(it) }
                .let { arbeidsmarkedCv[it] }["cv"]
    }
}

fun JsonNode.yyyyMMddTilLocalDate() = LocalDate.of(this[0].asInt(), this[1].asInt(), this[2].asInt())