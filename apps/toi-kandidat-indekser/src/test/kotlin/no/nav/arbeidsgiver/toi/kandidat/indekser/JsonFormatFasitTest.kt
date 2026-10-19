package no.nav.arbeidsgiver.toi.kandidat.indekser

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Fasittester som låser JSON-formatet appen leser og skriver:
 * - dokumentet som indekseres i OpenSearch (EsCv serialisert med esObjectMapper)
 * - meldingene lytterne publiserer på rapid
 * - hvordan null, manglende felt, andre JSON-typer og datoer i innkommende melding påvirker resultatet
 *
 * Meldingene endres som rene Kotlin-strukturer (Map og List), slik at testen ikke avhenger av
 * JsonNode-API-et den skal kontrollere.
 *
 * Fasitfilene ligger i src/test/resources/fasit. Kjør med OPPDATER_FASIT=true
 * for å skrive dem på nytt etter en tilsiktet endring.
 */
class JsonFormatFasitTest {

    companion object {
        private const val CV = "/arbeidsmarkedCv/endreCv/cv"
        private const val JOBBPROFIL = "/arbeidsmarkedCv/endreJobbprofil/jobbprofil"

        private val mapper = esObjectMapper()

        private fun standardSynligMelding() = rapidMelding(synlighet(), ontologi = ontologiDel(), geografi = geografiDel())
        private fun standardUferdigMelding() = rapidMelding(synlighet())

        @JvmStatic
        fun synligeMeldinger() = listOf(
            Arguments.of("escv-standard", standardSynligMelding()),
            Arguments.of(
                "escv-localdate-datoer",
                rapidMelding(synlighet(), ontologi = ontologiDel(), geografi = geografiDel(), yyyymmddTilLocalDate = true)
            ),
            Arguments.of(
                "escv-uten-jobbprofil",
                rapidMelding(synlighet(), ontologi = ontologiDel(), geografi = geografiDel(), jobbprofilEksisterer = false)
            ),
            Arguments.of(
                "escv-uten-kontor-og-tomme-lister",
                rapidMelding(
                    synlighet(), ontologi = ontologiDel(), geografi = geografiDel(),
                    kontorNavn = null, kontorId = null,
                    omfangJobbonsker = listOf(), ansettelsesformJobbonsker = listOf(),
                    arbeidstidsordningJobbonsker = listOf(), arbeidsdagerJobbonsker = listOf("LOERDAG", "SOENDAG"),
                    arbeidstidJobbonsker = listOf("DAGTID", "KVELD"),
                )
            ),
        )

        private val felterSomLesesVedIndeksering: List<String> = buildList {
            listOf(
                "fodselsnummer", "foedselsdato", "fornavn", "etternavn", "epost", "telefon", "gateadresse",
                "postnummer", "poststed", "sammendrag", "opprettet", "sistEndret", "synligForArbeidsgiver",
                "synligForVeileder", "arenaKandidatnr", "foererkort", "foererkort/klasse", "arbeidserfaring",
                "utdannelse", "fagdokumentasjon", "godkjenninger", "kurs", "sertifikat", "spraakferdigheter",
                "annenErfaring",
            ).forEach { add("$CV/$it") }
            listOf("klasse", "utloeper", "klasseBeskrivelse", "fraTidspunkt").forEach { add("$CV/foererkort/klasse/0/$it") }
            listOf(
                "stillingstittel", "styrkkode", "arbeidsgiver", "sted", "beskrivelse", "fraTidspunkt",
                "tilTidspunkt", "stillingstittelFritekst",
            ).forEach { add("$CV/arbeidserfaring/0/$it") }
            listOf(
                "laerested", "beskrivelse", "utdanningsretning", "fraTidspunkt", "tilTidspunkt", "nuskodeGrad",
                "utdannelseYrkestatus",
            ).forEach { add("$CV/utdannelse/0/$it") }
            listOf("type", "tittel", "beskrivelse").forEach { add("$CV/fagdokumentasjon/0/$it") }
            listOf("tittel", "utsteder", "gjennomfoert", "utloeper", "konseptId").forEach { add("$CV/godkjenninger/0/$it") }
            listOf("tittel", "utsteder", "tidspunkt", "varighet", "varighetEnhet").forEach { add("$CV/kurs/0/$it") }
            listOf(
                "tittel", "sertifikatnavn", "sertifikatnavnFritekst", "konseptId", "utsteder", "gjennomfoert", "utloeper",
            ).forEach { add("$CV/sertifikat/0/$it") }
            listOf("spraaknavn", "muntlig", "skriftlig").forEach { add("$CV/spraakferdigheter/0/$it") }
            listOf("beskrivelse", "rolle", "fraTidspunkt", "tilTidspunkt").forEach { add("$CV/annenErfaring/0/$it") }
            listOf(
                "stillinger", "kompetanser", "stillingkladder", "geografi", "ansettelsesformer", "arbeidstider",
                "arbeidsdager", "arbeidstidsordninger", "omfang", "oppstartKode", "geografi/0/kode", "geografi/0/sted",
            ).forEach { add("$JOBBPROFIL/$it") }
            listOf("endreCv", "endreJobbprofil", "opprettCv", "slettCv").forEach { add("/arbeidsmarkedCv/$it") }
            add(CV)
            add(JOBBPROFIL)
            addAll(
                listOf(
                    "/aktørId",
                    "/arbeidsmarkedCv",
                    "/oppfølgingsinformasjon",
                    "/oppfølgingsinformasjon/formidlingsgruppe",
                    "/oppfølgingsinformasjon/kvalifiseringsgruppe",
                    "/oppfølgingsinformasjon/hovedmaal",
                    "/siste14avedtak",
                    "/siste14avedtak/hovedmal",
                    "/siste14avedtak/innsatsgruppe",
                    "/fritattKandidatsøk",
                    "/fritattKandidatsøk/fritattKandidatsok",
                    "/veileder",
                    "/veileder/veilederId",
                    "/veileder/veilederinformasjon/visningsNavn",
                    "/veileder/veilederinformasjon/epost",
                    "/sisteOppfølgingsperiode",
                    "/sisteOppfølgingsperiode/kontor/kontorNavn",
                    "/sisteOppfølgingsperiode/kontor/kontorId",
                    "/geografi/kommune",
                    "/geografi/kommune/kommunenummer",
                    "/geografi/kommune/korrigertNavn",
                    "/geografi/fylke/korrigertNavn",
                    "/geografi/geografi",
                    "/geografi/geografi/NO03",
                    "/hullICv",
                    "/hullICv/førsteDagIInneværendeInaktivePeriode",
                    "/hullICv/sluttdatoerForInaktivePerioder",
                    "/ontologi/stillingstittel",
                    "/ontologi/stillingstittel/Lege",
                    "/ontologi/stillingstittel/Lege/synonymer",
                    "/ontologi/stillingstittel/Lege/merGenerell",
                    "/ontologi/stillingstittel/Sjåfør",
                    "/ontologi/kompetansenavn",
                    "/ontologi/kompetansenavn/Sirkusestetikk",
                    "/ontologi/kompetansenavn/Sirkusestetikk/synonymer",
                    "/ontologi/kompetansenavn/Sirkusestetikk/merGenerell",
                    "/synlighet/erSynlig",
                )
            )
        }

        private val andreVerdierVedIndeksering: List<Pair<String, Any?>> = listOf(
            "$CV/foedselsdato" to "1990-07-15",
            "$CV/foedselsdato" to listOf(1990, 7, 15),
            "$CV/foedselsdato" to listOf(1990, 7),
            "$CV/foedselsdato" to "1990-07-15T00:00:00",
            "$CV/foedselsdato" to 19900715,
            "$CV/kurs/0/tidspunkt" to "2013-02-18",
            "$CV/kurs/0/tidspunkt" to listOf(2013, 7, 1),
            "$CV/kurs/0/varighet" to "5",
            "$CV/kurs/0/varighet" to 5.7,
            "$CV/kurs/0/varighet" to "fem",
            "$CV/godkjenninger/0/gjennomfoert" to "2019-06-30",
            "$CV/sertifikat/0/utloeper" to "2030-12-31",
            "$CV/foererkort/klasse/0/fraTidspunkt" to "2015-03-29",
            "$CV/synligForArbeidsgiver" to "true",
            "$CV/synligForArbeidsgiver" to 1,
            "$CV/synligForArbeidsgiver" to "ja",
            "$CV/synligForVeileder" to "ja",
            "$CV/opprettet" to "1578319832.314",
            "$CV/opprettet" to 1578319832,
            "$CV/opprettet" to 1688165432.5,
            "$CV/opprettet" to "ikke-tall",
            "$CV/sistEndret" to "ikke-tall",
            "$CV/sistEndret" to 1688165432.5,
            "$CV/telefon" to 12345678,
            "$CV/postnummer" to 1798,
            "$CV/fornavn" to "Åse Ørjan-Æsa",
            "$CV/sammendrag" to "Linje 1\nLinje 2\t\"sitat\" æøå ÆØÅ \uD83D\uDE00 <b>&amp;</b> \\ /",
            "$CV/arbeidserfaring/0/fraTidspunkt" to "2020-01-15",
            "$CV/arbeidserfaring/0/fraTidspunkt" to "2020-1",
            "$CV/arbeidserfaring/0/stillingstittel" to "Tittel uten ontologi",
            "$CV/utdannelse/0/utdannelseYrkestatus" to "SVENNEBREV_FAGBREV",
            "$CV/utdannelse/0/utdannelseYrkestatus" to "MESTERBREV",
            "$CV/utdannelse/0/utdannelseYrkestatus" to "INGEN",
            "$CV/utdannelse/0/utdannelseYrkestatus" to "UKJENT",
            "$CV/arbeidserfaring" to listOf<Any>(),
            "$JOBBPROFIL/geografi/0/kode" to "NO99",
            "$JOBBPROFIL/kompetanser" to listOf<Any>(),
            "$JOBBPROFIL/kompetanser" to listOf("Kompetanse uten ontologi"),
            "$JOBBPROFIL/stillinger" to listOf("Stilling uten ontologi"),
            "$JOBBPROFIL/omfang" to listOf("HELTID", "DELTID"),
            "/aktørId" to 123,
            "/fritattKandidatsøk/fritattKandidatsok" to "true",
            "/fritattKandidatsøk/fritattKandidatsok" to 1,
            "/fritattKandidatsøk/fritattKandidatsok" to true,
            "/fritattKandidatsøk/fritattKandidatsok" to "ja",
            "/geografi/kommune/kommunenummer" to 301,
            "/geografi/kommune/kommunenummer" to "0301",
            "/geografi/kommune/kommunenummer" to "abc",
            "/hullICv/førsteDagIInneværendeInaktivePeriode" to "2024-03-31",
            "/hullICv/sluttdatoerForInaktivePerioder" to listOf<Any>(),
            "/hullICv/sluttdatoerForInaktivePerioder" to listOf("2024-10-27"),
            "/@slutt_av_hendelseskjede" to true,
            "/@slutt_av_hendelseskjede" to false,
        )

        private val felterSomLesesAvUferdigLytter: List<String> = listOf(
            "$CV/postnummer",
            "$CV/arbeidserfaring",
            "$CV/arbeidserfaring/0/stillingstittel",
            "$JOBBPROFIL/stillinger",
            "$JOBBPROFIL/kompetanser",
            "$JOBBPROFIL/stillingkladder",
            "$JOBBPROFIL/geografi",
            "$JOBBPROFIL/geografi/0/kode",
            "/arbeidsmarkedCv/endreCv",
            "/arbeidsmarkedCv/endreJobbprofil",
            CV,
            JOBBPROFIL,
            "/aktørId",
            "/synlighet/ferdigBeregnet",
        )

        /** CV og jobbprofil kan komme i opprett-, endre- eller slett-feltet. */
        private val flyttinger = listOf(
            "/arbeidsmarkedCv/endreCv" to "opprettCv",
            "/arbeidsmarkedCv/endreCv" to "slettCv",
            "/arbeidsmarkedCv/endreJobbprofil" to "opprettJobbprofil",
            "/arbeidsmarkedCv/endreJobbprofil" to "slettJobbprofil",
        )

        private val andreVerdierForUferdigLytter: List<Pair<String, Any?>> = listOf(
            "/@behov" to listOf("hullICv", "ontologi", "geografi"),
            "/@behov" to listOf("geografi", "ontologi", "hullICv", "annet"),
            "/@behov" to listOf("hullICv"),
            "/@behov" to listOf("annet"),
            "/@behov" to listOf<Any>(),
            "/@behov" to "hullICv",
            "$CV/postnummer" to 1798,
            "$CV/arbeidserfaring/0/stillingstittel" to "Sjåfør",
            "$JOBBPROFIL/stillingkladder" to listOf("Kladd"),
            "$JOBBPROFIL/stillinger" to listOf("Sjåfør", "Sjåfør"),
            "$JOBBPROFIL/geografi/0/kode" to 3,
            "/aktørId" to 123,
        )

        private val volatileMeldingsfelter = setOf(
            "@id", "@opprettet", "@forårsaket_av", "system_read_count", "system_participating_services",
        )
        private val behovsfelter = listOf("@behov", "kompetanse", "stillingstittel", "geografiKode", "postnummer")
    }

    private sealed interface Endring {
        val navn: String
    }

    private data object SettNull : Endring {
        override val navn = "null"
    }

    private data object Fjern : Endring {
        override val navn = "mangler"
    }

    private data class Flytt(val til: String) : Endring {
        override val navn: String get() = "flyttet til $til"
    }

    private data class SettVerdi(val verdi: Any?) : Endring {
        override val navn: String get() = "verdi " + tilJson(verdi)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("synligeMeldinger")
    fun `indeksert dokument er uendret`(navn: String, melding: String) {
        val utfall = indekser(melding)
        assertThat(utfall).isInstanceOf(Map::class.java)
        sammenlignMedFasit(navn, utfall)
    }

    @Test
    fun `melding publisert etter indeksering er uendret`() {
        val rapid = TestRapid()
        val esClient = mockk<ESClient>(relaxed = true)
        SynligKandidatfeedLytter(rapid, esClient)

        rapid.sendTestMessage(standardSynligMelding())

        assertThat(rapid.inspektør.size).isEqualTo(1)
        sammenlignMedFasit("publisert-etter-indeksering", utenVolatileFelter(rapid.inspektør.message(0).toString()))
    }

    @Test
    fun `null, manglende felt og andre verdier i melding gir samme indekserte dokument`() {
        val standard = indekser(standardSynligMelding())
        val resultat = sortedMapOf<String, Any?>()
        felterSomLesesVedIndeksering.forEach { peker ->
            resultat[peker] = listOf(SettNull, Fjern).associate { endring ->
                endring.navn to endringIDokument(standard, indekser(endre(standardSynligMelding(), peker, endring)))
            }
        }
        andreVerdierVedIndeksering.forEach { (peker, verdi) ->
            val endring = SettVerdi(verdi)
            val forrige = resultat[peker] as? Map<*, *> ?: emptyMap<String, Any?>()
            resultat[peker] = forrige + (endring.navn to endringIDokument(standard, indekser(endre(standardSynligMelding(), peker, endring))))
        }
        flyttinger.forEach { (peker, til) ->
            val endring = Flytt(til)
            val forrige = resultat[peker] as? Map<*, *> ?: emptyMap<String, Any?>()
            resultat[peker] = forrige + (endring.navn to endringIDokument(standard, indekser(endre(standardSynligMelding(), peker, endring))))
        }
        sammenlignMedFasit("escv-feltvariasjoner", resultat)
    }

    @Test
    fun `UferdigKandidatLytter publiserer samme melding som før`() {
        sammenlignMedFasit("uferdig-standard", publiserUferdig(standardUferdigMelding(), helMelding = true))
        sammenlignMedFasit(
            "uferdig-uten-jobbprofil",
            publiserUferdig(rapidMelding(synlighet(), jobbprofilEksisterer = false), helMelding = true)
        )
    }

    @Test
    fun `null, manglende felt og andre verdier gir samme behovsfelter fra UferdigKandidatLytter`() {
        val standard = publiserUferdig(standardUferdigMelding())
        val resultat = sortedMapOf<String, Any?>()
        felterSomLesesAvUferdigLytter.forEach { peker ->
            resultat[peker] = listOf(SettNull, Fjern).associate { endring ->
                endring.navn to endringIDokument(standard, publiserUferdig(endre(standardUferdigMelding(), peker, endring)))
            }
        }
        andreVerdierForUferdigLytter.forEach { (peker, verdi) ->
            val endring = SettVerdi(verdi)
            val forrige = resultat[peker] as? Map<*, *> ?: emptyMap<String, Any?>()
            resultat[peker] = forrige + (endring.navn to endringIDokument(standard, publiserUferdig(endre(standardUferdigMelding(), peker, endring))))
        }
        flyttinger.forEach { (peker, til) ->
            val endring = Flytt(til)
            val forrige = resultat[peker] as? Map<*, *> ?: emptyMap<String, Any?>()
            resultat[peker] = forrige + (endring.navn to endringIDokument(standard, publiserUferdig(endre(standardUferdigMelding(), peker, endring))))
        }
        sammenlignMedFasit("uferdig-feltvariasjoner", resultat)
    }

    @Test
    fun `UsynligKandidatfeedLytter sletter med samme aktørId som før`() {
        val usynlig = rapidMelding(synlighet(erSynlig = false))
        val varianter = linkedMapOf(
            "standard" to usynlig,
            "aktørId som tall" to endre(usynlig, "/aktørId", SettVerdi(123)),
            "aktørId null" to endre(usynlig, "/aktørId", SettNull),
            "aktørId mangler" to endre(usynlig, "/aktørId", Fjern),
            "aktørId tom" to endre(usynlig, "/aktørId", SettVerdi("")),
        )
        val resultat = varianter.mapValues { (_, melding) ->
            try {
                val esClient = mockk<ESClient>()
                val slettet = mutableListOf<String>()
                every { esClient.slettCv(capture(slettet)) } just runs
                val rapid = TestRapid()
                UsynligKandidatfeedLytter(rapid, esClient)
                rapid.sendTestMessage(melding)
                mapOf(
                    "slettCv" to slettet.toList(),
                    "publisert" to (0 until rapid.inspektør.size).map {
                        utenVolatileFelter(rapid.inspektør.message(it).toString())["@slutt_av_hendelseskjede"]
                    },
                )
            } catch (e: Throwable) {
                "FEIL ${e::class.simpleName}"
            }
        }
        sammenlignMedFasit("usynlig-variasjoner", resultat)
    }

    @Test
    fun `datoer og tider serialiseres og leses likt`() {
        val oslo = OffsetDateTime.of(2024, 7, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2))
        val serialisert = linkedMapOf<String, Any?>(
            "localDateVinter" to LocalDate.of(2024, 1, 15),
            "localDateSommer" to LocalDate.of(2024, 7, 1),
            "localDateSommertidStarter" to LocalDate.of(2024, 3, 31),
            "localDateSommertidSlutter" to LocalDate.of(2024, 10, 27),
            "localDate1900" to LocalDate.of(1900, 1, 1),
            "localDateSkuddag" to LocalDate.of(2024, 2, 29),
            "yearMonthVinter" to YearMonth.of(2024, 1),
            "yearMonthSommer" to YearMonth.of(2024, 7),
            "offsetDateTimeUtcMedNanos" to OffsetDateTime.of(2024, 7, 1, 12, 30, 45, 123456789, ZoneOffset.UTC),
            "offsetDateTimeUtcNyDatoIOslo" to OffsetDateTime.of(2024, 1, 15, 23, 30, 0, 0, ZoneOffset.UTC),
            "offsetDateTimePluss5" to OffsetDateTime.of(2024, 1, 15, 8, 0, 0, 0, ZoneOffset.ofHours(5)),
            "offsetDateTimeOslo" to oslo,
            "nullverdi" to null,
            "listeMedDatoer" to listOf(LocalDate.of(2024, 1, 15), null, YearMonth.of(2020, 12)),
        ).mapValues { (_, verdi) -> tilStruktur(mapper.writeValueAsString(verdi)) }

        val lest = linkedMapOf(
            "localDate fra offsetformat" to les("\"2024-07-01T00:00:00.000+02:00\"", LocalDate::class.java),
            "localDate fra offsetformat i UTC" to les("\"2024-06-30T22:00:00.000Z\"", LocalDate::class.java),
            "localDate fra ISO-dato" to les("\"2024-07-01\"", LocalDate::class.java),
            "localDate med mellomrom" to les("\" 2024-07-01T00:00:00.000+02:00 \"", LocalDate::class.java),
            "yearMonth fra offsetformat" to les("\"2024-03-01T00:00:00.000+01:00\"", YearMonth::class.java),
            "yearMonth fra ISO" to les("\"2024-03\"", YearMonth::class.java),
            "offsetDateTime fra offsetformat" to les("\"2024-07-01T12:30:45.123+02:00\"", OffsetDateTime::class.java),
            "offsetDateTime fra ISO uten millis" to les("\"2024-07-01T12:30:45+02:00\"", OffsetDateTime::class.java),
            "localDate fra null" to les("null", LocalDate::class.java),
        )

        sammenlignMedFasit("tidsformater", mapOf("serialisert" to serialisert, "lest" to lest))
    }

    private fun indekser(melding: String): Any? = try {
        val esClient = mockk<ESClient>()
        val lagret = mutableListOf<EsCv>()
        every { esClient.lagreEsCv(capture(lagret)) } just runs
        val rapid = TestRapid()
        SynligKandidatfeedLytter(rapid, esClient)
        rapid.sendTestMessage(melding)
        if (lagret.isEmpty()) "IKKE INDEKSERT" else tilStruktur(mapper.writeValueAsString(lagret.single()))
    } catch (e: Throwable) {
        "FEIL ${e::class.simpleName}"
    }

    private fun publiserUferdig(melding: String, helMelding: Boolean = false): Any? = try {
        val rapid = TestRapid()
        UferdigKandidatLytter(rapid)
        rapid.sendTestMessage(melding)
        if (rapid.inspektør.size == 0) {
            "IKKE PUBLISERT"
        } else {
            val publisert = utenVolatileFelter(rapid.inspektør.message(0).toString())
            if (helMelding) publisert else behovsfelter.associateWith { publisert[it] ?: "<mangler>" }
        }
    } catch (e: Throwable) {
        "FEIL ${e::class.simpleName}"
    }

    private fun utenVolatileFelter(json: String): Map<*, *> =
        (tilStruktur(json) as Map<*, *>).filterKeys { it !in volatileMeldingsfelter }

    private fun les(json: String, type: Class<*>): String = try {
        mapper.readValue(json, type)?.toString() ?: "null"
    } catch (_: Exception) {
        "FEIL"
    }

    private fun endre(melding: String, peker: String, endring: Endring): String {
        val rot = tilStruktur(melding)
        val segmenter = peker.removePrefix("/").split("/")
        val forelder = segmenter.dropLast(1).fold(rot) { node, segment ->
            when (node) {
                is Map<*, *> -> node[segment]
                is List<*> -> node[segment.toInt()]
                else -> null
            } ?: error("Fant ikke $peker i testmeldingen")
        }

        @Suppress("UNCHECKED_CAST")
        val objekt = forelder as? MutableMap<String, Any?> ?: error("$peker peker ikke inn i et objekt")
        val felt = segmenter.last()
        require(felt in objekt || endring is SettVerdi) { "Feltet $peker finnes ikke i testmeldingen" }
        when (endring) {
            SettNull -> objekt[felt] = null
            Fjern -> objekt.remove(felt)
            is SettVerdi -> objekt[felt] = endring.verdi
            is Flytt -> {
                objekt[endring.til] = objekt[felt]
                objekt[felt] = null
            }
        }
        return tilJson(rot)
    }

    /** Pekere til det som endret seg i dokumentet. Tomt kart betyr likt dokument. */
    private fun endringIDokument(før: Any?, etter: Any?): Any? {
        if (før !is Map<*, *> || etter !is Map<*, *>) return etter
        return sortedMapOf<String, Any?>().also { forskjeller(før, etter, "", it) }
    }

    private fun forskjeller(før: Any?, etter: Any?, sti: String, ut: MutableMap<String, Any?>) {
        when {
            før is Map<*, *> && etter is Map<*, *> -> (før.keys + etter.keys).map { it.toString() }.toSortedSet().forEach { nøkkel ->
                when {
                    nøkkel !in etter -> ut["$sti/$nøkkel"] = "<fjernet>"
                    nøkkel !in før -> ut["$sti/$nøkkel"] = etter[nøkkel]
                    else -> forskjeller(før[nøkkel], etter[nøkkel], "$sti/$nøkkel", ut)
                }
            }

            før is List<*> && etter is List<*> && før.size == etter.size ->
                før.indices.forEach { forskjeller(før[it], etter[it], "$sti/$it", ut) }

            før != etter -> ut[sti.ifEmpty { "/" }] = etter
        }
    }

    private fun sammenlignMedFasit(navn: String, faktisk: Any?) {
        val fil = File("src/test/resources/fasit/$navn.json")
        if (System.getenv("OPPDATER_FASIT") == "true") {
            fil.parentFile.mkdirs()
            fil.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(faktisk) + "\n")
        }
        val forventet = tilStruktur(fil.readText())
        val normalisertFaktisk = tilStruktur(tilJson(faktisk))
        val avvik = sortedMapOf<String, Any?>().also { forskjeller(forventet, normalisertFaktisk, "", it) }
        assertThat(avvik)
            .withFailMessage { "JSON for $navn avviker fra fasitfil ${fil.path}:\n" + avvik.entries.joinToString("\n") { (sti, verdi) -> "  $sti -> ${tilJson(verdi)}" } }
            .isEmpty()
    }
}

private val strukturMapper = esObjectMapper()
private fun tilStruktur(json: String): Any? = strukturMapper.readValue(json, Any::class.java)
private fun tilJson(struktur: Any?): String = strukturMapper.writeValueAsString(struktur)
