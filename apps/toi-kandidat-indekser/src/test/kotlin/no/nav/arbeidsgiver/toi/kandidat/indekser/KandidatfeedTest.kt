package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidsdager
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsAnnenErfaring
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsAnsettelsesformJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsArbeidsdagerJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsArbeidstidJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsArbeidstidsordningJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsFagdokumentasjon
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsForerkort
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsGeografiJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsGodkjenning
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsKompetanse
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsKurs
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsOmfangJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsPerioderMedInaktivitet
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsSertifikat
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsSprak
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsUtdanning
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsVerv
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeserfaring
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.UtdannelseYrkestatus
import no.nav.toi.TestRapid
import org.apache.hc.core5.http.HttpHost
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth


@Testcontainers
class KandidatfeedTest {
    companion object {
        private val esIndex = "kandidatfeed"
        @Container
        private var elasticsearch: ElasticsearchContainer =
            ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.3")
                .withExposedPorts(9200)
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
        private lateinit var testEsClient: ESClient
        private lateinit var client: OpenSearchClient
    }

    @BeforeEach
    fun setUp() {
        testEsClient = ESClient(elasticsearch.httpHostAddress, esIndex, "kandidat", "kandidat")
        client = OpenSearchClient(
            ApacheHttpClient5TransportBuilder.builder(
                HttpHost.create(elasticsearch.httpHostAddress)
            ).build())
    }

    @Test
    fun `Melding med kun CV og aktørId vil ikke opprette kandidat i ES`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = "")

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        assertIngenIIndekser()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til true men dekte behov ikke eksisterer på meldingen skal ikke kandidat legges i ES`() {
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true))

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingSynlig)

        assertIngenIIndekser()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til false men dekte behov ikke eksisterer skal kandidat enda slettes i ES`() {
        val kandidatnr = "CG133309"

        testEsClient.lagreEsCv(EsCvObjectMother.giveMeEsCv(kandidatnr = kandidatnr))

        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true), kandidatnr = kandidatnr)

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingUsynlig)

        assertIngenIIndekser()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet og har dekte behov skal kandidat legges til i ES`() {
        assertIngenIIndekser()
        val tomJson = """{}"""
        val expectedKandidatnr = "CG133310"
        val meldingSynlig = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            hullICv = tomJson,
            ontologi = tomJson,
            kandidatnr = expectedKandidatnr
        )
        val meldingUsynlig = rapidMelding(
            synlighet(erSynlig = false, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            hullICv = tomJson,
            ontologi = tomJson,
            kandidatnr = expectedKandidatnr
        )

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingSynlig)
        testrapid.sendTestMessage(meldingUsynlig)

        assertEnKandidatMedKandidatnr(expectedKandidatnr)
    }

    @Test
    fun `Meldinger der synlighet ikke er ferdig beregnet skal ikke kandidat legges i ES`() {
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = false))

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)
        testrapid.sendTestMessage(meldingSynlig)

        assertIngenIIndekser()
    }

    @Test
    fun `UsynligKandidatfeedLytter leser ikke melding om slutt_av_hendelseskjede er true`() {
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true), sluttAvHendelseskjede = true)

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingUsynlig)

        assertIngenIIndekser()
        assertThat(testrapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `SynligKandidatfeedLytter leser ikke melding om slutt_av_hendelseskjede er true`() {
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", hullICv = "{}", ontologi = "{}", sluttAvHendelseskjede = true)
        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(rapidMelding)

        assertIngenIIndekser()
        assertThat(testrapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `UsynligKandidatfeedLytter legger tilbake melding med slutt_av_hendelseskjede satt til true`() {
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
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", hullICv = "{}", ontologi = "{}")

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
        val tomJson = """{}"""
        val expectedKandidatnr = "CG133310"
        val expectedAktorId = "987654321"
        val expectedFodselsnummer = "12345678910"
        val expectedFornavn = "Ola"
        val expectedEtternavn = "Nordmann"
        val expectedFodselsdato = LocalDate.of(1990, 1, 1)
        val expectedFodselsdatoErDnr = false
        val expectedFormidlingsgruppekode = "ARBS"
        val expectedEpostadresse = "test@epost.no"
        val expectedMobiltelefon = "12345678"
        val expectedTelefon = "87654321"
        val expectedStatsborgerskap = "NOR"
        val expectedBeskrivelse = "Dette er en beskrivelse"
        val expectedSamtykkeStatus = "GITT"
        val expectedSamtykkeDato = LocalDate.of(2023, 1, 1)
        val expectedAdresselinje1 = "Adresse 1"
        val expectedAdresselinje2 = "Adresse 2"
        val expectedAdresselinje3 = "Adresse 3"
        val expectedPostnummer = "0123"
        val expectedPoststed = "Oslo"
        val expectedLandkode = "NO"
        val expectedKommunenummer = 1234
        val expectedTidsstempel = OffsetDateTime.now()
        val expectedDoed = false
        val expectedFrKode = "FR1234"
        val expectedKvalifiseringsgruppekode = "IVURD"
        val expectedHovedmaalkode = "SKAFFEA"
        val expectedHovedmal = "Skaffe arbeid"
        val expectedInnsatsgruppe = "STANDARD_INNSATS"
        val expectedNavkontor = "0123"
        val expectedOrgenhet = "0123 ASKER"
        val expectedFritattKandidatsok = false
        val expectedFritattAgKandidatsok = false
        val expectedSynligForArbeidsgiverSok = true
        val expectedSynligForVeilederSok = true
        val expectedOppstartKode = "KODE123"
        val expectedKommunenummerstring = "1234"
        val expectedVeilederIdent = "X123456"
        val expectedVeilederVisningsnavn = "Ola Nordmann"
        val expectedVeilederEpost = "veilder@nav.no"
        val expectedFylkeNavn = "Viken"
        val expectedKommuneNavn = "Asker"
        val expectedUtdanningFraDato = LocalDate.of(2010, 1, 1)
        val expectedUtdanningTilDato = LocalDate.of(2014, 1, 1)
        val expectedUtdanningUtdannelsessted = "Universitetet i Oslo"
        val expectedUtdanningNusKode = "123456"
        val expectedUtdanningAlternativGrad = "Master"
        val expectedUtdanningYrkestatus = UtdannelseYrkestatus.MESTERBREV.name
        val expectedUtdanningBeskrivelse = "Dette er en utdanning"
        val expectedFagdokumentasjonType = "VITNEMAL"
        val expectedFagdokumentasjonTittel = "Tittel på fagdokumentasjon"
        val expectedFagdokumentasjonBeskrivelse = "Dette er en fagdokumentasjon"
        val expectedYrkeserfaringFraDato = LocalDate.of(2015, 1, 1)
        val expectedYrkeserfaringTilDato = LocalDate.of(2020, 1, 1)
        val expectedYrkeserfaringArbeidsgiver = "Arbeidsgiver AS"
        val expectedYrkeserfaringStyrkKode = "1111"
        val expectedYrkeserfaringStillingstittel = "Javautvikler"
        val expectedYrkeserfaringStillingstitlerForTypeahead = "Javautvikler"
        val expectedYrkeserfaringAlternativStillingstittel = "Java programmerer"
        val expectedYrkeserfaringOrganisasjonsnummer = "123456789"
        val expectedYrkeserfaringNaceKode = "62.020"
        val expectedYrkeserfaringUtelukketForFremtiden = false
        val expectedYrkeserfaringSokeTitler = "Javautvikler"
        val expectedYrkeserfaringSted = "Oslo"
        val expectedYrkeserfaringBeskrivelse = "Dette er en yrkeserfaring"
        val expectedKompetanser = listOf("Utvikling", "Kaffedrikking")
        val expectedAnnenErfaringFraDato = LocalDate.of(2020, 1, 1)
        val expectedAnnenErfaringTilDato = LocalDate.of(2021, 1, 1)
        val expectedAnnenErfaringBeskrivelse = "Dette er en annen erfaring"
        val expectedAnnenErfaringRolle = "Rolle"
        val expectedSertifikatTittel = "Organisasjon"
        val expectedSertifikatNavn = "Førerkort klasse B"
        val expectedSertifikatnavnFritekst = "Førerkort klasse B fritekst"
        val expectedSertifikatKonseptId = "K123"
        val expectedSertifikatUtsteder = "Statens vegvesen"
        val expectedSertifikatGjennomfoert = LocalDate.of(2020, 1, 1)
        val expectedSertifikatUtloeper = LocalDate.of(2025, 1, 1)
        val expectedForerkortFraDato = OffsetDateTime.now().minusYears(1)
        val expectedForerkortTilDato = OffsetDateTime.now().plusYears(4)
        val expectedForerkortKode = "B"
        val expectedForerkortKodeKlasse = "B - Bil"
        val expectedForerkortAlternativKlasse = "Bil klasse"
        val expectedForerkortUtsteder = "Statens vegvesen"
        val expectedSprakFraDato = LocalDate.of(2020, 2, 1)
        val expectedSprakKode = "B"
        val expectedSprakKodeTekst = "Bokmål"
        val expectedSprakAlternativTekst = "Bokmål alternativ"
        val expectedSprakBeskrivelse = "Dette er et språk"
        val expectedSprakFerdighetMuntlig = "MUNTLIG_GODT"
        val expectedSprakFerdighetSkriftlig = "SKRIFTLIG_GODT"
        val expectedKursTittel = "Dette er et kurs"
        val expectedKursArrangor = "NAV"
        val expectedKursOmfangEnhet = "DAGER"
        val expectedKursOmfangVerdi = 5
        val expectedKursTilDato = LocalDate.of(2020, 3, 1)
        val expectedVervFraDato = LocalDate.of(2020, 4, 1)
        val expectedVervTilDato = LocalDate.of(2021, 4, 1)
        val expectedVervOrganisasjon = "Organisasjon"
        val expectedVervTittel = "Tittel på verv"
        val expectedGeografiJobbonskerGeografiKode = "0301"
        val expectedGeografiJobbonskerGeografiKodeTekst = "Oslo"
        val expectedYrkeJobbonskerStyrkKode = "1111"
        val expectedYrkeJobbonskerStyrkBeskrivelse = "Javautvikler"
        val expectedYrkeJobbonskerPrimaertJobbonske = true
        val expectedYrkeJobbonskerSokeTitler = "Javautvikler"
        val expectedOmfangJobbonskerOmfangKode = "100"
        val expectedOmfangJobbonskerOmfangKodeTekst = "100 %"
        val expectedAnsettelsesformJobbonskerAnsettelsesformKode = "FAST"
        val expectedAnsettelsesformJobbonskerAnsettelsesformKodeTekst = "Fast"
        val expectedArbeidstidsordningJobbonskerArbeidstidsordningKode = "DAG"
        val expectedArbeidstidsordningJobbonskerArbeidstidsordningKodeTekst = "Dag"
        val expectedArbeidsdagerJobbonskerArbeidsdagerKode = Arbeidsdager.UKEDAGER.name
        val expectedArbeidsdagerJobbonskerArbeidsdagerKodeTekst = "Ukedager"
        val expectedArbeidstidJobbonskerArbeidstidKode = "HELTID"
        val expectedArbeidstidJobbonskerArbeidstidKodeTekst = "Heltid"
        val expectedGodkjenningerTittel = "Dette er en godkjenning"
        val expectedGodkjenningerUtsteder = "Utsteder"
        val expectedGodkjenningerGjennomfoert = LocalDate.of(2020, 5, 1)
        val expectedGodkjenningerUtloeper = LocalDate.of(2025, 5, 1)
        val expectedGodkjenningerKonseptId = "KON123"
        val expectedPerioderMedInaktivitetStartdatoForInnevarendeInaktivePeriode = LocalDate.of(2023, 1, 1)
        val expectedPerioderMedInaktivitetSluttdatoForInnevarendeInaktivePeriode = LocalDate.of(2024, 1, 1)

        val melding = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            hullICv = tomJson,
            ontologi = tomJson,
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
            kommunenr = expectedKommunenummer.toString(),
            sistendret = Instant.from(expectedTidsstempel).toEpochMilli().let { "${it/1000}.${it%1000}" },
            kvalifiseringsgruppe = expectedKvalifiseringsgruppekode,
            oppfølgingsinformasjonHovedmaal = expectedHovedmaalkode,
            siste14AvedtakHovedmaal = expectedHovedmal,
            siste14AInnsatsgruppe = expectedInnsatsgruppe,
            organisasjonsenhetsnavn = expectedNavkontor,
            orgenhet = expectedOrgenhet,
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
                    fraTidspunkt = expectedForerkortFraDato.toString(),
                    tilTidspunkt = expectedForerkortTilDato.toString(),
                    klasse = expectedForerkortKode,
                    klasseBeskrivelse = expectedForerkortKodeKlasse,
                    klasseFritekst = expectedForerkortAlternativKlasse,
                    utsteder = expectedForerkortUtsteder
                )
            ),
        )

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(melding)

        assertThat(client.count().count()).isEqualTo(1)
        val cv = client.get({ req ->
            req.index(esIndex).id("123")
        }, EsCv::class.java)
        assertThat(cv.found()).isTrue
        assertThat(cv.index()).isEqualTo(expectedKandidatnr)
        val cvJson = jacksonObjectMapper().readTree(cv.toJsonString())
        assertThat(cvJson["aktorId"].asText()).isEqualTo(expectedAktorId)
        assertThat(cvJson["fodselsnummer"].asText()).isEqualTo(expectedFodselsnummer)
        assertThat(cvJson["fornavn"].asText()).isEqualTo(expectedFornavn)
        assertThat(cvJson["etternavn"].asText()).isEqualTo(expectedEtternavn)
        assertThat(cvJson["fodselsdato"].asText()).isEqualTo(expectedFodselsdato.toString())
        assertThat(cvJson["fodselsdatoErDnr"].asBoolean()).isEqualTo(expectedFodselsdatoErDnr)
        assertThat(cvJson["formidlingsgruppekode"].asText()).isEqualTo(expectedFormidlingsgruppekode)
        assertThat(cvJson["epostadresse"].asText()).isEqualTo(expectedEpostadresse)
        assertThat(cvJson["mobiltelefon"].asText()).isEqualTo(expectedMobiltelefon)
        assertThat(cvJson["telefon"].asText()).isEqualTo(expectedTelefon)
        assertThat(cvJson["statsborgerskap"].asText()).isEqualTo(expectedStatsborgerskap)
        assertThat(cvJson["kandidatnr"].asText()).isEqualTo(expectedKandidatnr)
        assertThat(cvJson["beskrivelse"].asText()).isEqualTo(expectedBeskrivelse)
        assertThat(cvJson["samtykkeStatus"].asText()).isEqualTo(expectedSamtykkeStatus)
        assertThat(cvJson["samtykkeDato"].asText()).isEqualTo(expectedSamtykkeDato.toString())
        assertThat(cvJson["adresselinje1"].asText()).isEqualTo(expectedAdresselinje1)
        assertThat(cvJson["adresselinje2"].asText()).isEqualTo(expectedAdresselinje2)
        assertThat(cvJson["adresselinje3"].asText()).isEqualTo(expectedAdresselinje3)
        assertThat(cvJson["postnummer"].asText()).isEqualTo(expectedPostnummer)
        assertThat(cvJson["poststed"].asText()).isEqualTo(expectedPoststed)
        assertThat(cvJson["landkode"].asText()).isEqualTo(expectedLandkode)
        assertThat(cvJson["kommunenummer"].asInt()).isEqualTo(expectedKommunenummer)
        assertThat(cvJson["tidsstempel"].asText()).isEqualTo(expectedTidsstempel.toString())
        assertThat(cvJson["kommunenummerkw"].asInt()).isEqualTo(expectedKommunenummer)
        assertThat(cvJson["doed"].asBoolean()).isEqualTo(expectedDoed)
        assertThat(cvJson["frKode"].asText()).isEqualTo(expectedFrKode)
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
        assertThat(cvJson["veilederIdent"].asText()).isEqualTo(expectedVeilederIdent)
        assertThat(cvJson["veilederVisningsnavn"].asText()).isEqualTo(expectedVeilederVisningsnavn)
        assertThat(cvJson["veilederEpost"].asText()).isEqualTo(expectedVeilederEpost)
        assertThat(cvJson["fylkeNavn"].asText()).isEqualTo(expectedFylkeNavn)
        assertThat(cvJson["kommuneNavn"].asText()).isEqualTo(expectedKommuneNavn)
        assertThat(cvJson["utdanning"][0]["fraDato"].asText()).isEqualTo(expectedUtdanningFraDato.toString())
        assertThat(cvJson["utdanning"][0]["tilDato"].asText()).isEqualTo(expectedUtdanningTilDato.toString())
        assertThat(cvJson["utdanning"][0]["utdannelsessted"].asText()).isEqualTo(expectedUtdanningUtdannelsessted)
        assertThat(cvJson["utdanning"][0]["nusKode"].asText()).isEqualTo(expectedUtdanningNusKode)
        assertThat(cvJson["utdanning"][0]["alternativGrad"].asText()).isEqualTo(expectedUtdanningAlternativGrad)
        assertThat(cvJson["utdanning"][0]["yrkestatus"].asText()).isEqualTo(expectedUtdanningYrkestatus)
        assertThat(cvJson["utdanning"][0]["beskrivelse"].asText()).isEqualTo(expectedUtdanningBeskrivelse)
        assertThat(cvJson["fagdokumentasjon"][0]["type"].asText()).isEqualTo(expectedFagdokumentasjonType)
        assertThat(cvJson["fagdokumentasjon"][0]["tittel"].asText()).isEqualTo(expectedFagdokumentasjonTittel)
        assertThat(cvJson["fagdokumentasjon"][0]["beskrivelse"].asText()).isEqualTo(expectedFagdokumentasjonBeskrivelse)
        assertThat(cvJson["yrkeserfaring"][0]["fraDato"].asText()).isEqualTo(expectedYrkeserfaringFraDato.toString())
        assertThat(cvJson["yrkeserfaring"][0]["tilDato"].asText()).isEqualTo(expectedYrkeserfaringTilDato.toString())
        assertThat(cvJson["yrkeserfaring"][0]["arbeidsgiver"].asText()).isEqualTo(expectedYrkeserfaringArbeidsgiver)
        assertThat(cvJson["yrkeserfaring"][0]["styrkKode"].asText()).isEqualTo(expectedYrkeserfaringStyrkKode)
        assertThat(cvJson["yrkeserfaring"][0]["stillingstittel"].asText()).isEqualTo(expectedYrkeserfaringStillingstittel)
        assertThat(cvJson["yrkeserfaring"][0]["stillingstitlerForTypeahead"][0].asText()).isEqualTo(expectedYrkeserfaringStillingstitlerForTypeahead)
        assertThat(cvJson["yrkeserfaring"][0]["alternativStillingstittel"].asText()).isEqualTo(expectedYrkeserfaringAlternativStillingstittel)
        assertThat(cvJson["yrkeserfaring"][0]["organisasjonsnummer"].asText()).isEqualTo(expectedYrkeserfaringOrganisasjonsnummer)
        assertThat(cvJson["yrkeserfaring"][0]["naceKode"].asText()).isEqualTo(expectedYrkeserfaringNaceKode)
        assertThat(cvJson["yrkeserfaring"][0]["utelukketForFremtiden"].asBoolean()).isEqualTo(expectedYrkeserfaringUtelukketForFremtiden)
        assertThat(cvJson["yrkeserfaring"][0]["sokeTitler"][0].asText()).isEqualTo(expectedYrkeserfaringSokeTitler)
        assertThat(cvJson["yrkeserfaring"][0]["sted"].asText()).isEqualTo(expectedYrkeserfaringSted)
        assertThat(cvJson["yrkeserfaring"][0]["beskrivelse"].asText()).isEqualTo(expectedYrkeserfaringBeskrivelse)
        assertThat(cvJson["kompetanse"][0]["fraDato"].isNull).isTrue
        assertThat(cvJson["kompetanse"][0]["kompKode"].isNull).isTrue
        assertThat(cvJson["kompetanse"][0]["kompKodeNavn"].asText()).isEqualTo(expectedKompetanser)
        assertThat(cvJson["kompetanse"][0]["alternativtNavn"].asText()).isEqualTo(expectedKompetanser)
        assertThat(cvJson["kompetanse"][0]["beskrivelse"].asText()).isEqualTo("")
        assertThat(cvJson["kompetanse"][0]["sokeTitler"].asText()).isEqualTo(listOf(""))
        assertThat(cvJson["annenErfaring"][0]["fraDato"].asText()).isEqualTo(expectedAnnenErfaringFraDato.toString())
        assertThat(cvJson["annenErfaring"][0]["tilDato"].asText()).isEqualTo(expectedAnnenErfaringTilDato.toString())
        assertThat(cvJson["annenErfaring"][0]["beskrivelse"].asText()).isEqualTo(expectedAnnenErfaringBeskrivelse)
        assertThat(cvJson["annenErfaring"][0]["rolle"].asText()).isEqualTo(expectedAnnenErfaringRolle)
        assertThat(cvJson["sertifikat"][0]["tittel"].asText()).isEqualTo(expectedSertifikatTittel)
        assertThat(cvJson["sertifikat"][0]["sertifikatnavn"].asText()).isEqualTo(expectedSertifikatNavn)
        assertThat(cvJson["sertifikat"][0]["sertifikatnavnFritekst"].asText()).isEqualTo(expectedSertifikatnavnFritekst)
        assertThat(cvJson["sertifikat"][0]["konseptId"].asText()).isEqualTo(expectedSertifikatKonseptId)
        assertThat(cvJson["sertifikat"][0]["utsteder"].asText()).isEqualTo(expectedSertifikatUtsteder)
        assertThat(cvJson["sertifikat"][0]["gjennomfoert"].asText()).isEqualTo(expectedSertifikatGjennomfoert.toString())
        assertThat(cvJson["sertifikat"][0]["utloeper"].asText()).isEqualTo(expectedSertifikatUtloeper.toString())
        assertThat(cvJson["forerkort"][0]["fraDato"].asText()).isEqualTo(expectedForerkortFraDato.toString())
        assertThat(cvJson["forerkort"][0]["tilDato"].asText()).isEqualTo(expectedForerkortTilDato.toString())
        assertThat(cvJson["forerkort"][0]["forerkortKode"].isNull).isTrue
        assertThat(cvJson["forerkort"][0]["forerkortKodeKlasse"].asText()).isEqualTo(expectedForerkortKodeKlasse)
        assertThat(cvJson["forerkort"][0]["alternativKlasse"].asText()).isEqualTo(expectedForerkortAlternativKlasse)
        assertThat(cvJson["forerkort"][0]["utsteder"].asText()).isEqualTo(expectedForerkortUtsteder)
        assertThat(cvJson["sprak"][0]["fraDato"].asText()).isEqualTo(expectedSprakFraDato.toString())
        assertThat(cvJson["sprak"][0]["sprakKode"].asText()).isEqualTo(expectedSprakKode)
        assertThat(cvJson["sprak"][0]["sprakKodeTekst"].asText()).isEqualTo(expectedSprakKodeTekst)
        assertThat(cvJson["sprak"][0]["alternativTekst"].asText()).isEqualTo(expectedSprakAlternativTekst)
        assertThat(cvJson["sprak"][0]["beskrivelse"].asText()).isEqualTo(expectedSprakBeskrivelse)
        assertThat(cvJson["sprak"][0]["ferdighetMuntlig"].asText()).isEqualTo(expectedSprakFerdighetMuntlig)
        assertThat(cvJson["sprak"][0]["ferdighetSkriftlig"].asText()).isEqualTo(expectedSprakFerdighetSkriftlig)
        assertThat(cvJson["kurs"][0]["tittel"].asText()).isEqualTo(expectedKursTittel)
        assertThat(cvJson["kurs"][0]["arrangor"].asText()).isEqualTo(expectedKursArrangor)
        assertThat(cvJson["kurs"][0]["omfangEnhet"].asText()).isEqualTo(expectedKursOmfangEnhet)
        assertThat(cvJson["kurs"][0]["omfangVerdi"].asInt()).isEqualTo(expectedKursOmfangVerdi)
        assertThat(cvJson["kurs"][0]["tilDato"].asText()).isEqualTo(expectedKursTilDato.toString())
        assertThat(cvJson["verv"][0]["fraDato"].asText()).isEqualTo(expectedVervFraDato.toString())
        assertThat(cvJson["verv"][0]["tilDato"].asText()).isEqualTo(expectedVervTilDato.toString())
        assertThat(cvJson["verv"][0]["organisasjon"].asText()).isEqualTo(expectedVervOrganisasjon)
        assertThat(cvJson["verv"][0]["tittel"].asText()).isEqualTo(expectedVervTittel)
        assertThat(cvJson["geografiJobbonsker"][0]["geografiKode"].asText()).isEqualTo(expectedGeografiJobbonskerGeografiKode)
        assertThat(cvJson["geografiJobbonsker"][0]["geografiKodeTekst"].asText()).isEqualTo(expectedGeografiJobbonskerGeografiKodeTekst)
        assertThat(cvJson["yrkeJobbonsker"][0]["styrkKode"].asText()).isEqualTo(expectedYrkeJobbonskerStyrkKode)
        assertThat(cvJson["yrkeJobbonsker"][0]["styrkBeskrivelse"].asText()).isEqualTo(expectedYrkeJobbonskerStyrkBeskrivelse)
        assertThat(cvJson["yrkeJobbonsker"][0]["primaertJobbonske"].asBoolean()).isEqualTo(expectedYrkeJobbonskerPrimaertJobbonske)
        assertThat(cvJson["yrkeJobbonsker"][0]["sokeTitler"][0].asText()).isEqualTo(expectedYrkeJobbonskerSokeTitler)
        assertThat(cvJson["omfangJobbonsker"][0]["omfangKode"].asText()).isEqualTo(expectedOmfangJobbonskerOmfangKode)
        assertThat(cvJson["omfangJobbonsker"][0]["omfangKodeTekst"].asText()).isEqualTo(expectedOmfangJobbonskerOmfangKodeTekst)
        assertThat(cvJson["ansettelsesformJobbonsker"][0]["ansettelsesformKode"].asText()).isEqualTo(expectedAnsettelsesformJobbonskerAnsettelsesformKode)
        assertThat(cvJson["ansettelsesformJobbonsker"][0]["ansettelsesformKodeTekst"].asText()).isEqualTo(expectedAnsettelsesformJobbonskerAnsettelsesformKodeTekst)
        assertThat(cvJson["arbeidstidsordningJobbonsker"][0]["arbeidstidsordningKode"].asText()).isEqualTo(expectedArbeidstidsordningJobbonskerArbeidstidsordningKode)
        assertThat(cvJson["arbeidstidsordningJobbonsker"][0]["arbeidstidsordningKodeTekst"].asText()).isEqualTo(expectedArbeidstidsordningJobbonskerArbeidstidsordningKodeTekst)
        assertThat(cvJson["arbeidsdagerJobbonsker"][0]["arbeidsdagerKode"].asText()).isEqualTo(expectedArbeidsdagerJobbonskerArbeidsdagerKode)
        assertThat(cvJson["arbeidsdagerJobbonsker"][0]["arbeidsdagerKodeTekst"].asText()).isEqualTo(expectedArbeidsdagerJobbonskerArbeidsdagerKodeTekst)
        assertThat(cvJson["arbeidstidJobbonsker"][0]["arbeidstidKode"].asText()).isEqualTo(expectedArbeidstidJobbonskerArbeidstidKode)
        assertThat(cvJson["arbeidstidJobbonsker"][0]["arbeidstidKodeTekst"].asText()).isEqualTo(expectedArbeidstidJobbonskerArbeidstidKodeTekst)
        assertThat(cvJson["godkjenninger"][0]["tittel"].asText()).isEqualTo(expectedGodkjenningerTittel)
        assertThat(cvJson["godkjenninger"][0]["utsteder"].asText()).isEqualTo(expectedGodkjenningerUtsteder)
        assertThat(cvJson["godkjenninger"][0]["gjennomfoert"].asText()).isEqualTo(expectedGodkjenningerGjennomfoert.toString())
        assertThat(cvJson["godkjenninger"][0]["utloeper"].asText()).isEqualTo(expectedGodkjenningerUtloeper.toString())
        assertThat(cvJson["godkjenninger"][0]["konseptId"].asText()).isEqualTo(expectedGodkjenningerKonseptId)
        assertThat(cvJson["perioderMedInaktivitet"][0]["startdatoForInnevarendeInaktivePeriode"].asText()).isEqualTo(expectedPerioderMedInaktivitetStartdatoForInnevarendeInaktivePeriode.toString())
        assertThat(cvJson["perioderMedInaktivitet"][0]["sluttdatoerForInaktivePerioderPaToArEllerMer"][0].asText()).isEqualTo(expectedPerioderMedInaktivitetSluttdatoForInnevarendeInaktivePeriode.toString())
    }

    private fun assertIngenIIndekser() {
        assertThat(client.count().count()).isEqualTo(0)
    }

    private fun assertEnKandidatMedKandidatnr(expectedKandidatnr: String) {
        assertThat(client.count().count()).isEqualTo(1)
        val cv = client.get({ req ->
            req.index(esIndex).id("123")
        }, EsCv::class.java)
        assertThat(cv.found()).isTrue
        assertThat(cv.index()).isEqualTo(expectedKandidatnr)
        assertThat(jacksonObjectMapper().readTree(cv.toJsonString())["kandidatnr"]).isEqualTo(expectedKandidatnr)
    }
}