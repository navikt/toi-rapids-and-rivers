package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidsdager
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.UtdannelseYrkestatus
import no.nav.toi.TestRapid
import org.assertj.core.api.AbstractStringAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.*
import java.time.format.DateTimeFormatter


@Testcontainers
class KandidatfeedIntegrasjonsTest {
    companion object {
        private val esIndex = "kandidater"
        @Container
        private var elasticsearch: ElasticsearchContainer =
            EsTestUtils.defaultElasticsearchContainer()
        private lateinit var testEsClient: ESClient
        private lateinit var client: OpenSearchClient
        private fun OpenSearchClient.flush() = this.indices().flush { it.index(esIndex) }
    }

    @BeforeEach
    fun setUp() {
        testEsClient = EsTestUtils.esClient(elasticsearch)
        client = EsTestUtils.openSearchClient(elasticsearch)
        EsTestUtils.ensureIndexClean(client, esIndex)
        EsTestUtils.sleepForAsyncES()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til false men dekte behov ikke eksisterer skal kandidat enda slettes i ES`() {
        assertIngenIIndekser()
        val kandidatnr = "CG133309"
        val aktørId = "1234567891011212"

        testEsClient.lagreEsCv(EsCvObjectMother.giveMeEsCv(kandidatnr = kandidatnr, aktorId = aktørId))

        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true), kandidatnr = kandidatnr, aktørId = aktørId)

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingUsynlig)

        assertIngenIIndekser()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet og har dekte behov skal kandidat legges til i ES`() {
        assertIngenIIndekser()
        val expectedKandidatnr = "CG133310"
        val meldingSynlig = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            ontologi = ontologiDel(),
            kandidatnr = expectedKandidatnr
        )
        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingSynlig)

        assertEnKandidatMedKandidatnr(expectedKandidatnr)
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til false, skal kandidat slettes om han er lagt til i ES`() {
        assertIngenIIndekser()
        val expectedKandidatnr = "CG133310"
        val meldingSynlig = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            ontologi = ontologiDel(),
            kandidatnr = expectedKandidatnr
        )
        val meldingUsynlig = rapidMelding(
            synlighet(erSynlig = false, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            ontologi = ontologiDel(),
            kandidatnr = expectedKandidatnr
        )

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingSynlig)
        assertEnKandidatMedKandidatnr(expectedKandidatnr)
        testrapid.sendTestMessage(meldingUsynlig)
        assertIngenIIndekser()
    }


    @Test
    fun `UsynligKandidatfeedLytter legger tilbake melding med slutt_av_hendelseskjede satt til true`() {
        assertIngenIIndekser()
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true))

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingUsynlig)

        assertThat(testrapid.inspektør.size).isEqualTo(1)
        assertThat(testrapid.inspektør.message(0).get("@slutt_av_hendelseskjede").booleanValue()).isEqualTo(true)
    }

    @Test
    fun `SynligKandidatfeedLytter legger tilbake melding med slutt_av_hendelseskjede satt til true`() {
        assertIngenIIndekser()
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", ontologi = ontologiDel())

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(rapidMelding)

        assertThat(testrapid.inspektør.size).isEqualTo(1)
        assertThat(testrapid.inspektør.message(0).get("@slutt_av_hendelseskjede").booleanValue()).isEqualTo(true)
    }



    @Test
    fun `Mapping fungerer rett på escv`() {
        assertIngenIIndekser()
        val expectedKandidatnr = "CG133310"
        val expectedAktorId = "987654321"
        val expectedFodselsnummer = "12345678910"
        val expectedFornavn = "Ola"
        val expectedEtternavn = "Nordmann"
        val expectedFodselsdato = LocalDate.of(1990, 1, 1)
        val expectedFodselsdatoErDnr = false
        val expectedFormidlingsgruppekode = "ARBS"
        val expectedEpostadresse = "test@epost.no"
        val expectedTelefon = "87654321"
        val expectedBeskrivelse = "Dette er en beskrivelse"
        val expectedSamtykkeDato = OffsetDateTime.of(LocalDateTime.of(2023, 1, 1, 13, 5, 45), ZoneOffset.UTC)
        val expectedAdresselinje1 = "Adresse 1"
        val expectedPostnummer = "0123"
        val expectedPoststed = "Oslo"
        val expectedKommunenummer = 301
        val expectedTidsstempel = OffsetDateTime.now()
        val expectedDoed = false
        val expectedKvalifiseringsgruppekode = "IVURD"
        val expectedHovedmaalkode = "SKAFFEA"
        val expectedHovedmal = "SKAFFE_ARBEID"
        val expectedInnsatsgruppe = "STANDARD_INNSATS"
        val expectedNavkontor = "0123"
        val expectedOrgenhet = "0123 ASKER"
        val expectedFritattKandidatsok = false
        val expectedFritattAgKandidatsok = false
        val expectedSynligForArbeidsgiverSok = true
        val expectedSynligForVeilederSok = true
        val expectedOppstartKode = "KODE123"
        val expectedKommunenummerstring = "0301"
        val expectedVeilederIdent = "X123456"
        val expectedVeilederVisningsnavn = "Ola Nordmann"
        val expectedVeilederEpost = "veilder@nav.no"
        val expectedFylkeNavn = "Oslo"
        val expectedKommuneNavn = "Oslo"
        val expectedUtdanningFraDato = YearMonth.of(2010, 1)
        val expectedUtdanningTilDato = YearMonth.of(2014, 1)
        val expectedUtdanningUtdannelsessted = "Universitetet i Oslo"
        val expectedUtdanningNusKode = "123456"
        val expectedUtdanningAlternativGrad = "Master"
        val expectedUtdanningYrkestatus = UtdannelseYrkestatus.MESTERBREV.name
        val expectedUtdanningBeskrivelse = "Dette er en utdanning"
        val expectedFagdokumentasjonType = "VITNEMAL"
        val expectedFagdokumentasjonTittel = "Tittel på fagdokumentasjon"
        val expectedFagdokumentasjonBeskrivelse = "Dette er en fagdokumentasjon"
        val expectedYrkeserfaringFraDato = YearMonth.of(2015, 1)
        val expectedYrkeserfaringTilDato = YearMonth.of(2020, 1)
        val expectedYrkeserfaringArbeidsgiver = "Arbeidsgiver AS"
        val expectedYrkeserfaringStyrkKode = "1111"
        val expectedYrkeserfaringStillingstittel = "Javautvikler"
        val expectedYrkeserfaringStillingstitlerForTypeahead = "Kotlinutvikler"
        val expectedYrkeserfaringAlternativStillingstittel = "Java programmerer"
        val expectedYrkeserfaringUtelukketForFremtiden = false
        val expectedYrkeserfaringSokeTitler = "Scalautvikler"
        val expectedYrkeserfaringSted = "Oslo"
        val expectedYrkeserfaringBeskrivelse = "Dette er en yrkeserfaring"
        val expectedKompetanser = listOf("Utvikling", "Kaffedrikking")
        val expectedAnnenErfaringFraDato = YearMonth.of(2020, 1)
        val expectedAnnenErfaringTilDato = YearMonth.of(2021, 1)
        val expectedAnnenErfaringBeskrivelse = "Dette er en annen erfaring"
        val expectedAnnenErfaringRolle = "Rolle"
        val expectedSertifikatTittel = "Organisasjon"
        val expectedSertifikatNavn = "Førerkort klasse B"
        val expectedSertifikatnavnFritekst = "Førerkort klasse B fritekst"
        val expectedSertifikatKonseptId = "K123"
        val expectedSertifikatUtsteder = "Statens vegvesen"
        val expectedSertifikatGjennomfoert = LocalDate.of(2020, 1, 1)
        val expectedSertifikatUtloeper = LocalDate.of(2025, 1, 1)
        val expectedForerkortFraDato = LocalDate.now().minusYears(1)
        val expectedForerkortTilDato = LocalDate.now().plusYears(4)
        val expectedForerkortKodeKlasse = "B - Bil"
        val expectedSprakKodeTekst = "Bokmål"
        val expectedSprakFerdighetMuntlig = "MUNTLIG_GODT"
        val expectedSprakFerdighetSkriftlig = "SKRIFTLIG_GODT"
        val expectedKursTittel = "Dette er et kurs"
        val expectedKursArrangor = "NAV"
        val expectedKursOmfangEnhet = "DAGER"
        val expectedKursOmfangVerdi = 5
        val expectedKursTilDato = LocalDate.of(2020, 3, 1)
        val expectedGeografiJobbonskerGeografiKode = "0301"
        val expectedGeografiJobbonskerGeografiKodeTekst = "Oslo"
        val expectedYrkeJobbonskerStyrkBeskrivelse = "Javautvikler"
        val expectedOmfangJobbonskerOmfangKode = "HELTID"
        val expectedOmfangJobbonskerOmfangKodeTekst = "Heltid"
        val expectedAnsettelsesformJobbonskerAnsettelsesformKode = "FAST"
        val expectedAnsettelsesformJobbonskerAnsettelsesformKodeTekst = "Fast"
        val expectedArbeidstidsordningJobbonskerArbeidstidsordningKode = "SKIFT"
        val expectedArbeidstidsordningJobbonskerArbeidstidsordningKodeTekst = "Skift"
        val expectedArbeidsdagerJobbonskerArbeidsdagerKode = Arbeidsdager.UKEDAGER.name
        val expectedArbeidsdagerJobbonskerArbeidsdagerKodeTekst = "Ukedager"
        val expectedArbeidstidJobbonskerArbeidstidKode = "DAGTID"
        val expectedArbeidstidJobbonskerArbeidstidKodeTekst = "Dagtid"
        val expectedGodkjenningerTittel = "Dette er en godkjenning"
        val expectedGodkjenningerUtsteder = "Utsteder"
        val expectedGodkjenningerGjennomfoert = LocalDate.of(2020, 5, 1)
        val expectedGodkjenningerUtloeper = LocalDate.of(2025, 5, 1)
        val expectedGodkjenningerKonseptId = "KON123"
        val expectedPerioderMedInaktivitetStartdatoForInnevarendeInaktivePeriode = LocalDate.of(2023, 1, 1)
        val expectedPerioderMedInaktivitetSluttdatoForInnevarendeInaktivePeriode = LocalDate.of(2024, 1, 1)

        val ontologiKompetanse = listOf("Ekstra kompetanse" , "Ekstra kompetanse 2")
        val melding = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            hullICv = """
                {
                    "førsteDagIInneværendeInaktivePeriode": "$expectedPerioderMedInaktivitetStartdatoForInnevarendeInaktivePeriode",
                    "sluttdatoerForInaktivePerioder": ["$expectedPerioderMedInaktivitetSluttdatoForInnevarendeInaktivePeriode"]
                }
            """.trimIndent(),
            ontologi = ontologiDel(
                stillingstittel = mapOf(
                    expectedYrkeserfaringStillingstittel to (listOf(expectedYrkeserfaringStillingstitlerForTypeahead) to listOf(expectedYrkeserfaringSokeTitler)),
                    "Ekstra yrkebeskrivelse" to (listOf("Ekstra yrkebeskrivelse") to listOf("Ekstra yrkebeskrivelse"))
                ),
                kompetansenavn = mapOf(
                    *expectedKompetanser.map { it to (listOf(ontologiKompetanse[0]) to listOf(ontologiKompetanse[1])) }.toTypedArray()
                )
            ),
            kandidatnr = expectedKandidatnr,
            aktørId = expectedAktorId,
            fødselsnummer = expectedFodselsnummer,
            fornavn = expectedFornavn,
            etternavn = expectedEtternavn,
            fødselsdato = expectedFodselsdato,
            formidlingsgruppe = expectedFormidlingsgruppekode,
            epostadresse = expectedEpostadresse,
            telefonnummer = expectedTelefon,
            sammendrag = expectedBeskrivelse,
            opprettetCv = Instant.from(expectedSamtykkeDato).toEpochMilli().let { "${it/1000}.${it%1000}" },
            gateadresse = expectedAdresselinje1,
            postnummer = expectedPostnummer,
            poststed = expectedPoststed,
            kommunenr = "54321",
            sistendret = Instant.from(expectedTidsstempel).toEpochMilli().let { "${it/1000}.${it%1000}" },
            kvalifiseringsgruppe = expectedKvalifiseringsgruppekode,
            oppfølgingsinformasjonHovedmaal = expectedHovedmaalkode,
            siste14AvedtakHovedmaal = expectedHovedmal,
            siste14AInnsatsgruppe = expectedInnsatsgruppe,
            organisasjonsenhetsnavn = expectedNavkontor,
            oppfølgingsenhet = expectedOrgenhet,
            fritattKandidatsok = expectedFritattKandidatsok,
            synligForArbeidsgiver = expectedSynligForArbeidsgiverSok,
            synligForVeileder = expectedSynligForVeilederSok,
            oppstartKode = expectedOppstartKode,
            veilederNavIdent = expectedVeilederIdent,
            veilederVisningsnavn = expectedVeilederVisningsnavn,
            veilederEpost = expectedVeilederEpost,
            utdanning = listOf(
                TestUtdanning(
                    fraTidspunkt = expectedUtdanningFraDato.toString(),
                    tilTidspunkt = expectedUtdanningTilDato.toString(),
                    laerested = expectedUtdanningUtdannelsessted,
                    nuskodeGrad = expectedUtdanningNusKode,
                    utdanningsretning = expectedUtdanningAlternativGrad,
                    utdannelseYrkestatus = expectedUtdanningYrkestatus,
                    beskrivelse = expectedUtdanningBeskrivelse,
                    autorisasjon = true
                )
            ),
            fagdokumentasjon = listOf(
                TestFagdokumentasjon(
                    type = expectedFagdokumentasjonType,
                    tittel = expectedFagdokumentasjonTittel,
                    beskrivelse = expectedFagdokumentasjonBeskrivelse
                )
            ),
            yrkeserfaring = listOf(
                TestYrkeserfaring(
                    fraTidspunkt = expectedYrkeserfaringFraDato.toString(),
                    tilTidspunkt = expectedYrkeserfaringTilDato.toString(),
                    arbeidsgiver = expectedYrkeserfaringArbeidsgiver,
                    styrkkode = expectedYrkeserfaringStyrkKode,
                    stillingstittel = expectedYrkeserfaringStillingstittel,
                    stillingstittelFritekst = expectedYrkeserfaringAlternativStillingstittel,
                    ikkeAktueltForFremtiden = expectedYrkeserfaringUtelukketForFremtiden,
                    sted = expectedYrkeserfaringSted,
                    beskrivelse = expectedYrkeserfaringBeskrivelse,
                    janzzKonseptid = "Ikke brukt"
                )
            ),
            kompetanse = expectedKompetanser,
            sprak = listOf(
                TestSprak(
                    navn = expectedSprakKodeTekst,
                    ferdighetMuntlig = expectedSprakFerdighetMuntlig,
                    ferdighetSkriftlig = expectedSprakFerdighetSkriftlig
                )
            ),
            godkjenning = listOf(
                TestGodkjenning(
                    tittel = expectedGodkjenningerTittel,
                    utsteder = expectedGodkjenningerUtsteder,
                    gjennomfoert = expectedGodkjenningerGjennomfoert,
                    utloeper = expectedGodkjenningerUtloeper,
                    konseptId = expectedGodkjenningerKonseptId
                )
            ),
            annenErfaring = listOf(
                TestAnnenErfaring(
                    fraTidspunkt = expectedAnnenErfaringFraDato.toString(),
                    tilTidspunkt = expectedAnnenErfaringTilDato.toString(),
                    beskrivelse = expectedAnnenErfaringBeskrivelse,
                    rolle = expectedAnnenErfaringRolle
                )
            ),
            sertifikat = listOf(
                TestSertifikat(
                    tittel = expectedSertifikatTittel,
                    sertifikatnavn = expectedSertifikatNavn,
                    sertifikatnavnFritekst = expectedSertifikatnavnFritekst,
                    konseptId = expectedSertifikatKonseptId,
                    utsteder = expectedSertifikatUtsteder,
                    gjennomfoert = expectedSertifikatGjennomfoert,
                    utloeper = expectedSertifikatUtloeper
                )
            ),
            forerkort = listOf(
                TestForerkort(
                    fraTidspunkt = expectedForerkortFraDato,
                    utloeper = expectedForerkortTilDato,
                    klasse = expectedForerkortKodeKlasse,
                    klasseBeskrivelse = expectedForerkortKodeKlasse,
                )
            ),
            kurs = listOf(
                TestKurs(
                    tittel = expectedKursTittel,
                    utsteder = expectedKursArrangor,
                    varighetEnhet = expectedKursOmfangEnhet,
                    varighet = expectedKursOmfangVerdi,
                    tidspunkt = expectedKursTilDato
                )
            ),
            geografiJobbonsker = listOf(
                TestGeografiJobbonsker(
                    kode = expectedGeografiJobbonskerGeografiKode,
                    sted = expectedGeografiJobbonskerGeografiKodeTekst
                )
            ),
            jobbProfilstillinger = listOf(expectedYrkeJobbonskerStyrkBeskrivelse, "Ekstra yrkebeskrivelse"),
            omfangJobbonsker = listOf(expectedOmfangJobbonskerOmfangKode),
            ansettelsesformJobbonsker = listOf(expectedAnsettelsesformJobbonskerAnsettelsesformKode),
            arbeidstidsordningJobbonsker = listOf(expectedArbeidstidsordningJobbonskerArbeidstidsordningKode),
            arbeidsdagerJobbonsker = listOf(expectedArbeidsdagerJobbonskerArbeidsdagerKode),
            arbeidstidJobbonsker = listOf(expectedArbeidstidJobbonskerArbeidstidKode),
        )

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(melding)

        waitForCount(1)
        val cv = client.get({ req ->
            req.index(esIndex).id(expectedKandidatnr)
        }, JsonNode::class.java)
        assertThat(cv.found()).isTrue
        assertThat(cv.id()).isEqualTo(expectedKandidatnr)
        val cvJson = cv.source()!!
        assertThat(cvJson["aktorId"].asText()).isEqualTo(expectedAktorId)
        assertThat(cvJson["fodselsnummer"].asText()).isEqualTo(expectedFodselsnummer)
        assertThat(cvJson["fornavn"].asText()).isEqualTo(expectedFornavn)
        assertThat(cvJson["etternavn"].asText()).isEqualTo(expectedEtternavn)
        assertThat(cvJson["fodselsdato"].asText()).isEqualTo(expectedFodselsdato.toString())
        assertThat(cvJson["fodselsdatoErDnr"].asBoolean()).isEqualTo(expectedFodselsdatoErDnr)
        assertThat(cvJson["formidlingsgruppekode"].asText()).isEqualTo(expectedFormidlingsgruppekode)
        assertThat(cvJson["epostadresse"].asText()).isEqualTo(expectedEpostadresse)
        assertThat(cvJson["mobiltelefon"].isNull).isTrue
        assertThat(cvJson["telefon"].asText()).isEqualTo(expectedTelefon)
        assertThat(cvJson["statsborgerskap"].isNull).isTrue
        assertThat(cvJson["harKontaktinformasjon"].asBoolean()).isTrue
        assertThat(cvJson["kandidatnr"].asText()).isEqualTo(expectedKandidatnr)
        assertThat(cvJson["beskrivelse"].asText()).isEqualTo(expectedBeskrivelse)
        assertThat(cvJson["samtykkeStatus"].asText()).isEqualTo("G")
        assertThat(cvJson["samtykkeDato"].asText()).datoEquals(expectedSamtykkeDato)
        assertThat(cvJson["adresselinje1"].asText()).isEqualTo(expectedAdresselinje1)
        assertThat(cvJson["adresselinje2"].asText()).isEqualTo("")
        assertThat(cvJson["adresselinje3"].asText()).isEqualTo("")
        assertThat(cvJson["postnummer"].asText()).isEqualTo(expectedPostnummer)
        assertThat(cvJson["poststed"].asText()).isEqualTo(expectedPoststed)
        assertThat(cvJson["landkode"].isNull).isTrue
        assertThat(cvJson["kommunenummer"].asInt()).isEqualTo(expectedKommunenummer)
        assertThat(cvJson["tidsstempel"].asText()).datoEquals(expectedTidsstempel)
        assertThat(cvJson["kommunenummerkw"].asInt()).isEqualTo(expectedKommunenummer)
        assertThat(cvJson["doed"].asBoolean()).isEqualTo(expectedDoed)
        assertThat(cvJson["frKode"].asText()).isEqualTo("0")
        assertThat(cvJson["kvalifiseringsgruppekode"].asText()).isEqualTo(expectedKvalifiseringsgruppekode)
        assertThat(cvJson["hovedmaalkode"].asText()).isEqualTo(expectedHovedmaalkode)
        assertThat(cvJson["hovedmal"].asText()).isEqualTo(expectedHovedmal)
        assertThat(cvJson["innsatsgruppe"].asText()).isEqualTo(expectedInnsatsgruppe)
        assertThat(cvJson["navkontor"].asText()).isEqualTo(expectedNavkontor)
        assertThat(cvJson["orgenhet"].asText()).isEqualTo(expectedOrgenhet)
        assertThat(cvJson["fritattKandidatsok"].asBoolean()).isEqualTo(expectedFritattKandidatsok)
        assertThat(cvJson["fritattAgKandidatsok"].asBoolean()).isEqualTo(expectedFritattAgKandidatsok)
        assertThat(cvJson["synligForArbeidsgiverSok"].asBoolean()).isEqualTo(expectedSynligForArbeidsgiverSok)
        assertThat(cvJson["synligForVeilederSok"].asBoolean()).isEqualTo(expectedSynligForVeilederSok)
        assertThat(cvJson["oppstartKode"].asText()).isEqualTo(expectedOppstartKode)
        assertThat(cvJson["kommunenummerstring"].asText()).isEqualTo(expectedKommunenummerstring)
        assertThat(cvJson["veilederIdent"].asText()).isEqualTo(expectedVeilederIdent.lowercase())
        assertThat(cvJson["veilederVisningsnavn"].asText()).isEqualTo(expectedVeilederVisningsnavn)
        assertThat(cvJson["veilederEpost"].asText()).isEqualTo(expectedVeilederEpost)
        assertThat(cvJson["fylkeNavn"].asText()).isEqualTo(expectedFylkeNavn)
        assertThat(cvJson["kommuneNavn"].asText()).isEqualTo(expectedKommuneNavn)
        assertThat(cvJson["utdanning"][0]["fraDato"].asText()).datoEquals(expectedUtdanningFraDato)
        assertThat(cvJson["utdanning"][0]["tilDato"].asText()).datoEquals(expectedUtdanningTilDato)
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asText()).isEqualTo(expectedUtdanningUtdannelsessted)
        assertThat(cvJson["utdanning"][0]["nusKode"].asText()).isEqualTo(expectedUtdanningNusKode)
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asText()).isEqualTo(expectedUtdanningAlternativGrad)
        assertThat(cvJson["utdanning"][0]["yrkestatus"].asText()).isEqualTo(expectedUtdanningYrkestatus)
        assertThat(cvJson["utdanning"][0]["beskrivelse"].asText()).isEqualTo(expectedUtdanningBeskrivelse)
        assertThat(cvJson["fagdokumentasjon"][0]["type"].asText()).isEqualTo(expectedFagdokumentasjonType)
        assertThat(cvJson["fagdokumentasjon"][0]["tittel"].asText()).isEqualTo(expectedFagdokumentasjonTittel)
        assertThat(cvJson["fagdokumentasjon"][0]["beskrivelse"].asText()).isEqualTo(expectedFagdokumentasjonBeskrivelse)
        assertThat(cvJson["yrkeserfaring"][0]["fraDato"].asText()).datoEquals(expectedYrkeserfaringFraDato)
        assertThat(cvJson["yrkeserfaring"][0]["tilDato"].asText()).datoEquals(expectedYrkeserfaringTilDato)
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedYrkeserfaringArbeidsgiver)
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedYrkeserfaringStyrkKode)
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asText()).isEqualTo(expectedYrkeserfaringStillingstittel)
        assertThat(cvJson["yrkeserfaring"][0]["stillingstitlerForTypeahead"][0].asText()).isEqualTo(expectedYrkeserfaringStillingstitlerForTypeahead)
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asText()).isEqualTo(expectedYrkeserfaringAlternativStillingstittel)
        assertThat(cvJson["yrkeserfaring"][0]["organisasjonsnummer"].isNull).isTrue
        assertThat(cvJson["yrkeserfaring"][0]["naceKode"].isNull).isTrue
        assertThat(cvJson["yrkeserfaring"][0]["utelukketForFremtiden"].asBoolean()).isFalse
        assertThat(cvJson["yrkeserfaring"][0]["sokeTitler"].map(JsonNode::asText))
            .isEqualTo(listOf(expectedYrkeserfaringStillingstitlerForTypeahead, expectedYrkeserfaringSokeTitler, expectedYrkeserfaringStillingstittel))
        assertThat(cvJson["yrkeserfaring"][0]["sted"].asText()).isEqualTo(expectedYrkeserfaringSted)
        assertThat(cvJson["yrkeserfaring"][0]["beskrivelse"].asText()).isEqualTo(expectedYrkeserfaringBeskrivelse)
        expectedKompetanser.forEachIndexed { i, kompetanse ->
            assertThat(cvJson["kompetanseObj"][i]["fraDato"].isNull).isTrue
            assertThat(cvJson["kompetanseObj"][i]["kompKode"].isNull).isTrue
            assertThat(cvJson["kompetanseObj"][i]["kompKodeNavn"].asText()).isEqualTo(kompetanse)
            assertThat(cvJson["kompetanseObj"][i]["alternativtNavn"].asText()).isEqualTo(kompetanse)
            assertThat(cvJson["kompetanseObj"][i]["beskrivelse"].asText()).isEqualTo("")
            assertThat(cvJson["kompetanseObj"][i]["sokeNavn"].map(JsonNode::asText)).isEqualTo(ontologiKompetanse + kompetanse)
        }
        assertThat(cvJson["annenerfaringObj"][0]["fraDato"].asText()).datoEquals(expectedAnnenErfaringFraDato)
        assertThat(cvJson["annenerfaringObj"][0]["tilDato"].asText()).datoEquals(expectedAnnenErfaringTilDato)
        assertThat(cvJson["annenerfaringObj"][0]["beskrivelse"].asText()).isEqualTo(expectedAnnenErfaringBeskrivelse)
        assertThat(cvJson["annenerfaringObj"][0]["rolle"].asText()).isEqualTo(expectedAnnenErfaringRolle)
        assertThat(cvJson["sertifikatObj"][0]["sertifikatKodeNavn"].asText()).isEqualTo(expectedSertifikatNavn)
        assertThat(cvJson["sertifikatObj"][0]["alternativtNavn"].asText()).isEqualTo(expectedSertifikatnavnFritekst)
        assertThat(cvJson["sertifikatObj"][0]["sertifikatKode"].asText()).isEqualTo(expectedSertifikatKonseptId)
        assertThat(cvJson["sertifikatObj"][0]["utsteder"].asText()).isEqualTo(expectedSertifikatUtsteder)
        assertThat(cvJson["sertifikatObj"][0]["fraDato"].asText()).datoEquals(expectedSertifikatGjennomfoert)
        assertThat(cvJson["sertifikatObj"][0]["tilDato"].asText()).datoEquals(expectedSertifikatUtloeper)
        assertThat(cvJson["forerkort"][0]["fraDato"].asText()).datoEquals(expectedForerkortFraDato)
        assertThat(cvJson["forerkort"][0]["tilDato"].asText()).datoEquals(expectedForerkortTilDato)
        assertThat(cvJson["forerkort"][0]["forerkortKode"].isNull).isTrue
        assertThat(cvJson["forerkort"][0]["forerkortKodeKlasse"].asText()).isEqualTo(expectedForerkortKodeKlasse)
        assertThat(cvJson["forerkort"][0]["alternativKlasse"].isNull).isTrue
        assertThat(cvJson["forerkort"][0]["utsteder"].isNull).isTrue
        assertThat(cvJson["sprak"][0]["fraDato"].isNull).isTrue
        assertThat(cvJson["sprak"][0]["sprakKode"].isNull).isTrue
        assertThat(cvJson["sprak"][0]["sprakKodeTekst"].asText()).isEqualTo(expectedSprakKodeTekst)
        assertThat(cvJson["sprak"][0]["alternativTekst"].asText()).isEqualTo(expectedSprakKodeTekst)
        assertThat(cvJson["sprak"][0]["beskrivelse"].asText()).isEqualTo("Muntlig: $expectedSprakFerdighetMuntlig Skriftlig: $expectedSprakFerdighetSkriftlig")
        assertThat(cvJson["sprak"][0]["ferdighetMuntlig"].asText()).isEqualTo(expectedSprakFerdighetMuntlig)
        assertThat(cvJson["sprak"][0]["ferdighetSkriftlig"].asText()).isEqualTo(expectedSprakFerdighetSkriftlig)
        assertThat(cvJson["kursObj"][0]["tittel"].asText()).isEqualTo(expectedKursTittel)
        assertThat(cvJson["kursObj"][0]["arrangor"].asText()).isEqualTo(expectedKursArrangor)
        assertThat(cvJson["kursObj"][0]["omfangEnhet"].asText()).isEqualTo(expectedKursOmfangEnhet)
        assertThat(cvJson["kursObj"][0]["omfangVerdi"].asInt()).isEqualTo(expectedKursOmfangVerdi)
        assertThat(cvJson["kursObj"][0]["tilDato"].asText()).datoEquals(expectedKursTilDato)
        assertThat(cvJson["geografiJobbonsker"][0]["geografiKode"].asText()).isEqualTo(expectedGeografiJobbonskerGeografiKode)
        assertThat(cvJson["geografiJobbonsker"][0]["geografiKodeTekst"].asText()).isEqualTo(expectedGeografiJobbonskerGeografiKodeTekst)
        assertThat(cvJson["yrkeJobbonskerObj"][0]["styrkKode"].isNull).isTrue
        assertThat(cvJson["yrkeJobbonskerObj"][0]["styrkBeskrivelse"].asText()).isEqualTo(expectedYrkeJobbonskerStyrkBeskrivelse)
        assertThat(cvJson["yrkeJobbonskerObj"][0]["primaertJobbonske"].asBoolean()).isFalse
        assertThat(cvJson["yrkeJobbonskerObj"][0]["sokeTitler"].map { it.asText() })
            .isEqualTo(listOf(expectedYrkeserfaringStillingstitlerForTypeahead, expectedYrkeserfaringSokeTitler, expectedYrkeJobbonskerStyrkBeskrivelse))
        assertThat(cvJson["omfangJobbonskerObj"][0]["omfangKode"].asText()).isEqualTo(expectedOmfangJobbonskerOmfangKode)
        assertThat(cvJson["omfangJobbonskerObj"][0]["omfangKodeTekst"].asText()).isEqualTo(expectedOmfangJobbonskerOmfangKodeTekst)
        assertThat(cvJson["ansettelsesformJobbonskerObj"][0]["ansettelsesformKode"].asText()).isEqualTo(expectedAnsettelsesformJobbonskerAnsettelsesformKode)
        assertThat(cvJson["ansettelsesformJobbonskerObj"][0]["ansettelsesformKodeTekst"].asText()).isEqualTo(expectedAnsettelsesformJobbonskerAnsettelsesformKodeTekst)
        assertThat(cvJson["arbeidstidsordningJobbonskerObj"][0]["arbeidstidsordningKode"].asText()).isEqualTo(expectedArbeidstidsordningJobbonskerArbeidstidsordningKode)
        assertThat(cvJson["arbeidstidsordningJobbonskerObj"][0]["arbeidstidsordningKodeTekst"].asText()).isEqualTo(expectedArbeidstidsordningJobbonskerArbeidstidsordningKodeTekst)
        assertThat(cvJson["arbeidsdagerJobbonskerObj"][0]["arbeidsdagerKode"].asText()).isEqualTo(expectedArbeidsdagerJobbonskerArbeidsdagerKode)
        assertThat(cvJson["arbeidsdagerJobbonskerObj"][0]["arbeidsdagerKodeTekst"].asText()).isEqualTo(expectedArbeidsdagerJobbonskerArbeidsdagerKodeTekst)
        assertThat(cvJson["arbeidstidJobbonskerObj"][0]["arbeidstidKode"].asText()).isEqualTo(expectedArbeidstidJobbonskerArbeidstidKode)
        assertThat(cvJson["arbeidstidJobbonskerObj"][0]["arbeidstidKodeTekst"].asText()).isEqualTo(expectedArbeidstidJobbonskerArbeidstidKodeTekst)
        assertThat(cvJson["godkjenninger"][0]["tittel"].asText()).isEqualTo(expectedGodkjenningerTittel)
        assertThat(cvJson["godkjenninger"][0]["utsteder"].asText()).isEqualTo(expectedGodkjenningerUtsteder)
        assertThat(cvJson["godkjenninger"][0]["gjennomfoert"].asText()).datoEquals(expectedGodkjenningerGjennomfoert)
        assertThat(cvJson["godkjenninger"][0]["utloeper"].asText()).datoEquals(expectedGodkjenningerUtloeper)
        assertThat(cvJson["godkjenninger"][0]["konseptId"].asText()).isEqualTo(expectedGodkjenningerKonseptId)
        assertThat(cvJson["perioderMedInaktivitet"]["startdatoForInnevarendeInaktivePeriode"].asText()).datoEquals(expectedPerioderMedInaktivitetStartdatoForInnevarendeInaktivePeriode)
        assertThat(cvJson["perioderMedInaktivitet"]["sluttdatoerForInaktivePerioderPaToArEllerMer"][0].asText()).datoEquals(expectedPerioderMedInaktivitetSluttdatoForInnevarendeInaktivePeriode)
    }

    private fun assertIngenIIndekser() {
        waitForCount(0)
    }

    private fun assertEnKandidatMedKandidatnr(expectedKandidatnr: String) {
        waitForCount(1)
        val cv = client.get({ req ->
            req.index(esIndex).id(expectedKandidatnr)
        }, EsCv::class.java)
        assertThat(cv.found()).isTrue
        assertThat(cv.id()).isEqualTo(expectedKandidatnr)
        assertThat(cv.source()?.indekseringsnøkkel()).isEqualTo(expectedKandidatnr)
    }
    private fun waitForCount(expected: Long) {
        EsTestUtils.sleepForAsyncES()
        // Keep existing stronger refresh/flush semantics via utils
        val deadline = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < deadline) {
            client.indices().refresh { it.index(esIndex) }
            EsTestUtils.flush(client, esIndex)
            val current = client.count().count()
            if (current == expected) return
            Thread.sleep(200)
        }
        client.indices().refresh { it.index(esIndex) }
        EsTestUtils.flush(client, esIndex)
        assertThat(client.count().count()).isEqualTo(expected)
    }
}

private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

private fun AbstractStringAssert<*>.datoEquals(expected: YearMonth) {
    datoEquals(expected.atDay(1))
}

private fun AbstractStringAssert<*>.datoEquals(expected: LocalDate) {
    isEqualTo(expected.atStartOfDay(ZoneId.of("Europe/Oslo")).format(format))
}

private fun AbstractStringAssert<*>.datoEquals(expected: OffsetDateTime) {
    isEqualTo(expected.atZoneSameInstant(ZoneId.of("Europe/Oslo")).format(format))
}

private fun sleepForAsyncES() {
    EsTestUtils.sleepForAsyncES()
}