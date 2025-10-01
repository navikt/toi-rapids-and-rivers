package no.nav.arbeidsgiver.toi.kandidat.indekser

import no.nav.arbeid.pam.kodeverk.ansettelse.Ansettelsesform
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidsdager
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidstid
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidstidsordning
import no.nav.arbeid.pam.kodeverk.ansettelse.Omfang
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
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsSertifikat
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsSprak
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsUtdanning
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeserfaring
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.UtdannelseYrkestatus
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

const val anleggsmaskinfører = "Anleggsmaskinfører"
const val ikval = "IKVAL"

object EsCvObjectMother {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val LOGGER = LoggerFactory.getLogger(EsCvObjectMother::class.java)
    private val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

    fun nteAktorId(n: Int) = (900000000000L + n * 1000L).toString()

    private fun fraIsoDato(string: String) = LocalDate.parse(string).atStartOfDay().atOffset(ZoneOffset.UTC)

    fun antallDagerTilbakeFraNow(antallDager: Int) = OffsetDateTime.now(ZoneOffset.UTC).minusDays(antallDager.toLong())

    private fun fodselsdatoForAlder(alder: Int) = LocalDate.now().minusYears(alder.toLong())

    private fun nåMinusÅr(antallÅr: Int) = OffsetDateTime.now().minusYears(antallÅr.toLong())

    fun giveMeEsCv(kandidatnr: String = "1L"): EsCv {
        val utdanning = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "Otta vgs. Otta",
            "355211", "Mekaniske fag, grunnkurs", "GK maskin/mekaniker"
        )

        val utdanning1 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Høyskolen i Gjøvik", "786595", "Master i sikkerhet", "Master i sikkerhet"
        )


        val utdanningsListe = mutableListOf<EsUtdanning>()
        utdanningsListe.add(utdanning)
        utdanningsListe.add(utdanning1)
        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"), fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø", "8341.01", "Anleggsmaskindrifter",
            setOf("Anleggsmaskindrifter", "Anleggsmaskinoperatør"), "maskinkjører og maskintransport", "YRKE_ORGNR",
            "YRKE_NACEKODE", false, listOf(), "Oslo")

        val yrkeserfaring2 = lagEsYrkeserfaring(
            nåMinusÅr(20),
            nåMinusÅr(9),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf ("Anleggsmaskindrifter"),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            listOf(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf("Anleggsmaskindrifter"),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            listOf(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            listOf(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            listOf(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            listOf(),
            "Oslo"
        )

        val yrkeserfaringsListe =
            listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, listOf()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, listOf()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, listOf()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, listOf()
        )

        val kompetanseList = listOf(
            kompetanse1, kompetanse2, kompetanse3, kompetanse4
        )

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe= listOf(sertifikat1, sertifikat2, sertifikat3)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak1 = EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 = EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()


        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Oslo", "NO03")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, listOf()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        val omfangJobbonsker = EsOmfangJobbonsker(Omfang.HELTID.id, Omfang.HELTID.defaultTekst())

        val omfangJobbonskerList = listOf(omfangJobbonsker)

        val ansettelsesforholdJobbonsker = EsAnsettelsesformJobbonsker(
            Ansettelsesform.FAST.id, Ansettelsesform.FAST.defaultTekst()
        )

        val ansettelsesforholdJobbonskerListe = listOf(ansettelsesforholdJobbonsker)

        val arbeidstidsordningJobbonsker = EsArbeidstidsordningJobbonsker(
            Arbeidstidsordning.SKIFT.id,
            Arbeidstidsordning.SKIFT.defaultTekst()
        )

        val arbeidstidsordningJobbonskerListe = listOf(arbeidstidsordningJobbonsker)

        val arbeidstidJobbonsker = EsArbeidstidJobbonsker(
            Arbeidstid.DAGTID.id, Arbeidstid.DAGTID.defaultTekst()
        )
        val arbeidstidJobbonskerList = listOf(arbeidstidJobbonsker)


        val arbeidsdagerJobbonskerLoerdag = EsArbeidsdagerJobbonsker(
            Arbeidsdager.LOERDAG.id, Arbeidsdager.LOERDAG.defaultTekst()
        )
        val arbeidsdagerJobbonskerSoendag: EsArbeidsdagerJobbonsker = EsArbeidsdagerJobbonsker(
            Arbeidsdager.SOENDAG.id, Arbeidsdager.SOENDAG.defaultTekst()
        )

        val arbeidsdagerJobbonskerList = listOf(arbeidsdagerJobbonskerLoerdag, arbeidsdagerJobbonskerSoendag)

        return EsCv(
            nteAktorId(1),
            "01016012345",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "JOBBS",
            "unnasluntrer@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            kandidatnr,
            "hererjeg",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            5001,
            false,
            antallDagerTilbakeFraNow(0),
            301,
            java.lang.Boolean.FALSE,
            null,
            ikval,
            null,
            "SKAFFE_ARBEID",
            "SPESIELT_TILPASSET_INNSATS",
            "0220 NAV Asker",
            "0220",
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            false,
            "LEDIG_NAA",
            "5001",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            "Viken",
            "Lier",
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            omfangJobbonskerList,
            ansettelsesforholdJobbonskerListe,
            arbeidstidsordningJobbonskerListe,
            arbeidsdagerJobbonskerList,
            arbeidstidJobbonskerList,
            emptyList(),
            null
        )
    }

    private fun lagEsYrkeserfaring(
        fraDato: OffsetDateTime,
        tilDato: OffsetDateTime?,
        arbeidsgiver: String,
        styrkKode: String,
        stillingstittel: String,
        stillingstitlerForTypeahead: Set<String>,
        alternativStillingstittel: String,
        organisasjonsnummer: String,
        naceKode: String,
        utelukketForFremtiden: Boolean,
        sokeTitler: List<String>,
        sted: String
    ) = EsYrkeserfaring(YearMonth.from(fraDato), tilDato?.let(YearMonth::from), arbeidsgiver, styrkKode, stillingstittel, stillingstitlerForTypeahead, alternativStillingstittel, organisasjonsnummer, naceKode, utelukketForFremtiden, sokeTitler, sted)

    private fun lagEsUtdanning(
        fraDato: OffsetDateTime,
        tilDato: OffsetDateTime,
        utdannelsessted: String,
        nusKode: String?,
        nuskodeGrad: String?,
        alternativgrad: String,
    ) = EsUtdanning(YearMonth.from(fraDato), YearMonth.from(tilDato), utdannelsessted, nusKode, alternativgrad)

    fun lagEsAnnenErfaring(
        fraDato: OffsetDateTime,
        tilDato: OffsetDateTime,
        beskrivelse: String,
        rolle: String? = null
    ) = EsAnnenErfaring(YearMonth.from(fraDato), YearMonth.from(tilDato), beskrivelse, rolle)

    fun lagEsSertifikat(
        fraDato: OffsetDateTime,
        tilDato: OffsetDateTime?,
        konseptId: String,
        tittel: String,
        beskrivelse: String?,
        utsteder: String
    ) = EsSertifikat(LocalDate.from(fraDato), tilDato?.let(LocalDate::from), konseptId, tittel, beskrivelse, utsteder)

    fun lagEsForerkort(
        fraDato: OffsetDateTime,
        tilDato: OffsetDateTime?,
        konseptId: String,
        tittel: String,
        beskrivelse: String?,
        utsteder: String
    ) = EsForerkort(fraDato, tilDato, konseptId, tittel, beskrivelse, utsteder)

    fun lagEsKurs(
        tittel: String,
        utsteder: String,
        gyldighetsperiodeEnhet: String?,
        gyldighetsperiodeLengde: Int?,
        gjennomfoertDato: OffsetDateTime
    ) = EsKurs(tittel, utsteder, gyldighetsperiodeEnhet, gyldighetsperiodeLengde, LocalDate.from(gjennomfoertDato))

    fun lagEsGodkjenning(
        tittel: String,
        utsteder: String,
        gjennomfoert: OffsetDateTime,
        utloeper: OffsetDateTime?,
        konseptId: String
    ) = EsGodkjenning(tittel, utsteder, LocalDate.from(gjennomfoert), LocalDate.from(utloeper), konseptId)

    private fun esKurs1() = lagEsKurs(
        "Akseloppretting", "Easy-Laser", null,
        null, fraIsoDato("2012-12-01")
    )

    private fun esKurs2() = lagEsKurs(
        "Varme arbeider Sertifikat",
        "Norsk brannvernforening", "ÅR", 5, fraIsoDato("2015-06-01")
    )

    private fun esKurs3() = lagEsKurs(
        "Flensarbeid for Norsk Olje og Gass",
        "Torqlite Europa a/s", "ÅR", 4, fraIsoDato("2016-02-01")
    )

    private fun esKurs4() = lagEsKurs(
        "Varme arbeider EsSertifikat",
        "Norsk brannvernforening", "ÅR", 5, fraIsoDato("2015-06-01")
    )

    private fun esKurs5() = lagEsKurs("Spring Boot", "Spring-folkene", "ÅR", 5, fraIsoDato("2015-06-01"))


    fun giveMeEsCv2(): EsCv {
        val EsUtdanning =
            lagEsUtdanning(
                fraIsoDato("1999-08-20"),
                fraIsoDato("2002-06-20"),
                "Hamar Katedralskole",
                "296647",
                "Studiespesialisering",
                "Studiespesialisering med realfag"
            )

        val utdanningListe = listOf(EsUtdanning)
        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2002-01-01"),
            "Kodesentralen Vardø",
            "5746.07",
            "Programvareutvikler",
            setOf("Programvareutvikler"),
            "Fullstackutvikler",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"), fraIsoDato("2003-07-01"),
            "Programvarefabrikken Førde", "5746.07", "Systemutvikler",
            setOf("Systemutvikler", "Java-utvikler"),
            "Utvikling av nytt kandidatsøk", "YRKE_ORGNR", "YRKE_NACEKODE", false, emptyList(), "Oslo")

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "Tjenestetest Norge",
            "6859.02",
            "Systemtester",
            setOf("Systemtester"),
            "Automatiske tester av nytt kandidatsøk",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2006-07-01"),
            "lagerarbeiderne L. H.",
            "8659.03",
            "Lagermedarbeider",
            setOf("Lagermedarbeider"),
            "Lagermedarbeider",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2017-04-01"),
            "lagerarbeiderne L. H.",
            "8659.03",
            "Truckfører lager",
            setOf("Truckfører lager"),
            "Stortruck",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
                fraIsoDato("2017-10-01"),
                fraIsoDato("2019-09-26"),
                "Awesome coders AS",
                "5746.07",
                "Javautvikler",
                setOf("Javautvikler"),
                "Javautvikler",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                emptyList(),
                "Oslo"
            )

        val yrkeserfaringListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
                fraIsoDato("2016-03-14"),
                "265478",
                "Javautvikler",
                null,
                null,
                emptyList()
            )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"),
            "265478",
            "Programvareutvikler",
            "Programvareutvikler",
            null,
            emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "475136", "Lagermedarbeider",
            "Lagermedarbeider", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
                fraIsoDato("2016-03-14"),
                "501",
                "Truckfører",
                "Truckfører",
                null,
                emptyList()
            )

        val kompetanseListe = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val EsSertifikat1 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe = listOf(EsSertifikat1, EsSertifikat2, EsSertifikat3)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak = EsSprak(fraIsoDato("2012-12-01"), "19093", "Norsk", "Norwegian", "Flytende")

        val sprakListe = listOf(sprak)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs4()

        val kurs3: EsKurs = esKurs3()


        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Stavanger", "NO11.1103")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Førde", "NO14.1432")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Javaprogrammerer",
                true,
                listOf()
            )

        val yrkeJobbonsker1 =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Programvareutvikler",
                true,
                emptyList()
            )

        val yrkeJobbonsker2 =
            EsYrkeJobbonsker("Yrke jobb ønskeStyrk Kode", "Bonde", true, listOf())
        val yrkeJobbonsker3 =
            EsYrkeJobbonsker("1010.01", "Butikkmedarbeider", true, listOf())

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker, yrkeJobbonsker1, yrkeJobbonsker2, yrkeJobbonsker3)

        return EsCv(
            nteAktorId(2),
            "05236984567",
            "KARI",
            "NORDMANN",
            fodselsdatoForAlder(39),
            false,
            "PARBS",
            "unnasluntrer2@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            "2L",
            "Dette er beskrivelsen av hva jeg har gjort i min yrkeskarriere",
            "J",
            fraIsoDato("2016-05-30"),
            "Dinvei 2",
            "",
            "",
            "1337",
            "HUSKER IKKE",
            "NO",
            301,
            false,
            antallDagerTilbakeFraNow(1),
            401,
            java.lang.Boolean.FALSE,
            null,
            ikval,
            null,
            null,
            null,
            "0316 NAV Gamle Oslo",
            "0316",
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            true,
            null,
            "0401",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            "Viken",
            "Lier",
            utdanningListe,
            emptyList(),
            yrkeserfaringListe,
            kompetanseListe,
            emptyList(),
            EsSertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeEsCv3(): EsCv {
        val esUtdanning: EsUtdanning = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Norges Naturvitenskapelige Universitet", "456375", "Sosiologi", "Sosiologi"
        )

        val utdanningListe = listOf(esUtdanning)
        val yrkeserfaring1: EsYrkeserfaring = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-02-01"),
            "Butikken i nærheten",
            "1010.01",
            "Butikkmedarbeider",
            setOf("Butikkmedarbeider"),
            "Butikkmedarbeider i Førde",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"), fraIsoDato("2003-02-01"),
            "Butikken i nærheten", "1010.01", "Butikkmedarbeider(dagligvarer)",
            setOf("Butikkmedarbeider(dagligvarer)", "Butikkmedarbeider")
            , "Butikkmedarbeider(dagligvarer)", "YRKE_ORGNR", "YRKE_NACEKODE", false, emptyList(), "Oslo")

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "Butikken langt unna",
            "1010.01",
            "Butikkmedarbeider(trevare)",
            setOf("Butikkmedarbeider(trevare)"),
            "Butikkmedarbeider(trevare)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Butikken",
            "4561.03",
            "Butikkmedarbeider(elektronikk)",
            setOf("Butikkmedarbeider(elektronikk)"),
            "Butikkmedarbeider(elektronikk)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 =
            lagEsYrkeserfaring(
                fraIsoDato("2016-06-01"),
                fraIsoDato("2016-07-01"),
                "Tvkanalen TV?",
                "5684.05",
                "Presentør",
                setOf("Presentør"),
                "Presentør",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                emptyList(),
                "Oslo"
            )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            null,
            "NLI  Grenland",
            "5684.05",
            "Nyhetsanker",
            setOf("Nyhetsanker"),
            "Nyhetsanker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringListe = listOf(
            yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6
        )

        val kompetanse1 =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "152424",
                "Presentør",
                null,
                null,
                emptyList()
            )

        val kompetanse2 =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "152424",
                "Nyhetsanker",
                "Nyhetsanker",
                null,
                emptyList()
            )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "566895", "Butikkmedarbeider",
            "Butikkmedarbeider", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "566895",
            "Butikkmedarbeider(trevare)", "Butikkmedarbeider(trevare)", null, emptyList()
        )

        val kompetanseListe = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val EsSertifikat1 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Truckførerbevis", "Truckførerbevis", ""
        )
        val EsSertifikat2 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat4 = lagEsSertifikat(
            fraIsoDato("1999-01-01"), null, "L2.7000",
            "Truckførerbevis T1 Lavtløftende plukktruck, palletruck m/perm. førerplass",
            "Truckførerbevis T1 Lavtløftende plukktruck, palletruck m/perm. førerplass", ""
        )

        val esSertifikatListe = listOf(EsSertifikat1, EsSertifikat2, EsSertifikat3, EsSertifikat4)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "T - Traktor", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak =
            EsSprak(fraIsoDato("2012-12-01"), "87392", "Engelsk", "English", "Flytende")

        val sprakListe = listOf(sprak)

        val kurs1 = esKurs1()

        val kurs2 = esKurs4()

        val kurs3 = esKurs3()


        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonsker1 =
            EsGeografiJobbonsker("Bergen", "NO12.1201")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1)

        val yrkeJobbonsker =
            EsYrkeJobbonsker("Yrke jobb ønskeStyrk Kode", "Ordfører", true, emptyList())

        val yrkeJobbonsker1 =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Barnehageassistent",
                true,
                emptyList()
            )

        val yrkeJobbonsker2 =
            EsYrkeJobbonsker("Yrke jobb ønskeStyrk Kode", "Tester", true, emptyList())

        val yrkeJobbonsker3 =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Konsulent (data)",
                true,
                emptyList()
            )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker, yrkeJobbonsker1, yrkeJobbonsker2, yrkeJobbonsker3)

        return  EsCv(
            nteAktorId(3),
            "04265983651",
            "HANS",
            "NORDMANN",
            LocalDate.parse("1955-11-04"),
            false,
            "RARBS",
            "alltidmed@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            "3L",
            "Jeg jobber like godt selvstendig som i team",
            "J",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "2323",
            "INGEBERG",
            "NO",
            301,
            false,
            antallDagerTilbakeFraNow(7),
            301,
            java.lang.Boolean.FALSE,
            null,
            "VARIG",
            null,
            null,
            null,
            "0220 NAV Asker",
            "0220",
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            true,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            "Viken",
            "Drammen",
            utdanningListe,
            emptyList(),
            yrkeserfaringListe,
            kompetanseListe,
            emptyList(),
            esSertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeEsCv4(): EsCv {
        val EsUtdanning: EsUtdanning = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Norges Naturvitenskapelige Universitet", "456375", "Bygg og anlegg",
            "Bygg og anlegg"
        )

        val utdanningListe = listOf(EsUtdanning)
        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2002-01-01"),
            "Jokah",
            "1010.01",
            "Butikkmedarbeider",
            setOf("Butikkmedarbeider"),
            "Butikkmedarbeider",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-04-01"),
            "Nærbutikkern",
            "1010.01",
            "Butikkmedarbeider(klesbutikk)",
            setOf("Butikkmedarbeider(klesbutikk)"),
            "Butikkmedarbeider(klebutikk)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 =
            lagEsYrkeserfaring(
                fraIsoDato("2003-04-01"),
                fraIsoDato("2003-07-01"),
                "Tv tv tv",
                "5684.05",
                "Nyhetspresentør",
                setOf("Nyhetspresentør"),
                "Nyhetspresentør",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                emptyList(),
                "Oslo"
            )

        val yrkeserfaring4 =
            lagEsYrkeserfaring(
                fraIsoDato("2005-08-01"),
                fraIsoDato("2016-07-01"),
                "Vard Group,avd.Brevik",
                "5684.05",
                "Hallovert",
                setOf("Hallovert"),
                "Hallovert",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                emptyList(),
                "Oslo"
            )

        val yrkeserfaring5 =
            lagEsYrkeserfaring(
                fraIsoDato("2016-06-01"),
                fraIsoDato("2017-04-01"),
                "DN teater",
                "5124.46",
                "Skuespiller",
                setOf("Skuespiller"),
                "Skuespiller",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                emptyList(),
                "Oslo"
            )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            null,
            "Dukketeateret Rena",
            "5124.46",
            "Skuespiller(dukketeater)",
            setOf("Skuespiller(dukketeater)"),
            "Skuespiller(dukketeater)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "152424",
                "Hallovert",
                null,
                null,
                emptyList()
            )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "566895", "Butikkmedarbeider",
            "Butikkmedarbeider", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"),
            "564646",
            "Butikkmedarbeider(klesbutikk)",
            "Butikkmedarbeider(klesbutikk)",
            null,
            emptyList()
        )

        val kompetanse4 =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "506",
                "Skuespiller",
                "Skuespiller",
                null,
                emptyList()
            )

        val kompetanseListe = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val EsSertifikat1 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe = listOf(EsSertifikat1, EsSertifikat2, EsSertifikat3)

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkort5 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "S - Snøscooter", null, ""
        )

        val forerkortListe = listOf(forerkort4, forerkort5)

        val sprak = EsSprak(fraIsoDato("2012-12-01"), "78985", "Tysk", "German", "Begynner")

        val sprakListe = listOf(sprak)

        val kurs1 = esKurs1()

        val kurs2 = esKurs4()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Oslo", "NO03.0301")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Akershus", "NO02")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Bærum", "NO02.1219")

        val geografiJobbonsker3 = EsGeografiJobbonsker("Norge", "NO")

        val geografiJobbonsker4 = EsGeografiJobbonsker("Karasjok", "NO02.2021")

        val geografiJobbonskerListe = listOf(
            geografiJobbonsker,
            geografiJobbonsker1,
            geografiJobbonsker2,
            geografiJobbonsker3,
            geografiJobbonsker4
        )

        val yrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Butikkmedarbeider",
                true,
                emptyList()
            )

        val yrkeJobbonsker1: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Butikkmedarbeider",
                true,
                emptyList()
            )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker, yrkeJobbonsker1)

        return EsCv(
            nteAktorId(4),
            "09568410230",
            "HANNE",
            "NORDMANN",
            LocalDate.parse("2002-06-04"),
            false,
            "ARBS",
            "erjegmed@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            "4L",
            "",
            "J",
            fraIsoDato("2016-05-30"),
            "Noensvei 1",
            "",
            "",
            "9730",
            "KARASJOK",
            "NO",
            2021,
            false,
            antallDagerTilbakeFraNow(8),
            2021,
            java.lang.Boolean.FALSE,
            null,
            "VURDI",
            null,
            null,
            null,
            "0602 NAV Drammen",
            "0602",
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            true,
            null,
            "2021",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            "Viken",
            "Oslo",
            utdanningListe,
            emptyList(),
            yrkeserfaringListe,
            kompetanseListe,
            emptyList(),
            EsSertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeEsCv5(): EsCv {
        val utdanningListe = emptyList<EsUtdanning>()

        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-02-01"),
            "Bankhvelvet BBL",
            "4865.75",
            "Bankhvelvoperatør",
            setOf("Bankhvelvoperatør"),
            "Bankhvelvoperatør",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 =
            lagEsYrkeserfaring(
                fraIsoDato("2003-01-01"),
                fraIsoDato("2003-02-01"),
                "Proggehula",
                "5746.07",
                "Progger",
                setOf("Progger"),
                "Progger",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                emptyList(),
                "Oslo"
            )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "Test a.a.s",
            "6859.02",
            "Tester",
            setOf("Tester"),
            "Tester",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 =
            lagEsYrkeserfaring(
                fraIsoDato("2005-08-01"),
                fraIsoDato("2005-09-01"),
                "K.O. kranservice",
                "8342.01",
                "Kranoperatør",
                setOf("Kranoperatør"),
                "Kranoperatør",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                listOf("Operatør av kran"),
                "Oslo"
            )

        val yrkeserfaring5: EsYrkeserfaring = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-06-02"),
            "Lang transport A.S.",
            "8332.03",
            "Lastebil- \"Lastebil- og trailersjåførog trailersjåfør",
            setOf("Lastebil- \"Lastebil- og trailersjåførog trailersjåfør"),
            "Sjåfør kl. 3",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            null,
            "Mekken mekk",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringListe =
            listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid(type 2 kran)", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid spesielt", "Mekanisk arbeid spesielt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3220201",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseListe = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val EsSertifikat1 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe = listOf(EsSertifikat1, EsSertifikat2, EsSertifikat3)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort4)

        val sprak =
            EsSprak(fraIsoDato("2012-12-01"), "78983", "Dansk", "Danish", "Uforståelig")

        val sprakListe = listOf(sprak)

        val kurs1 = esKurs1()

        val kurs2 = esKurs5()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Harstad", "NO19.1903")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Sunnhordaland", "NO12.2200")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Tromsø", "NO19.1902")

        val geografiJobbonsker3 = EsGeografiJobbonsker("Jessheim", "NO02.0219")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2, geografiJobbonsker3)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Industrimekaniker",
            true,
            emptyList()
        )

        val yrkeJobbonsker1 = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Lastebilsjåfør",
            true,
            emptyList()
        )

        val yrkeJobbonsker2 = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Butikkmedarbeider",
            true,
            emptyList()
        )

        val yrkeJobbonsker3 = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Konsulent (bank)",
            true,
            emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker, yrkeJobbonsker1, yrkeJobbonsker2, yrkeJobbonsker3)

        return EsCv(
            nteAktorId(5),
            "03050316895",
            "BOB",
            "NORDMANN",
            LocalDate.parse("1964-09-01"),
            false,
            "ARBS",
            "bobob@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            "5L",
            "",
            "J",
            fraIsoDato("2016-05-30"),
            "Minvei 90",
            "",
            "",
            "0219",
            "Bærum",
            "NO",
            219,
            false,
            antallDagerTilbakeFraNow(10),
            219,
            java.lang.Boolean.FALSE,
            null,
            null,
            null,
            null,
            null,
            "0215 NAV Drøbak",
            "0215",
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            true,
            null,
            "0219",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningListe,
            emptyList(),
            yrkeserfaringListe,
            kompetanseListe,
            emptyList(),
            EsSertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeCvUtenKompetanse(): EsCv {
        val utdanningListe = emptyList<EsUtdanning>()


        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-02-01"),
            "Bankhvelvet BBL",
            "4865.75",
            "Bankhvelvoperatør",
            setOf("Bankhvelvoperatør"),
            "Bankhvelvoperatør",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "Proggehula",
            "5746.07",
            "Progger",
            setOf("Progger"),
            "Progger",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "Test a.a.s",
            "6859.02",
            "Tester",
            setOf("Tester"),
            "Tester",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "K.O. kranservice",
            "8342.01",
            "Kranoperatør",
            setOf("Kranoperatør"),
            "Kranoperatør",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-06-02"),
            "Lang transport A.S.",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 3",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-11-01"),
            "Mekken mekk",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanseListe = emptyList<EsKompetanse>()

        val EsSertifikat1 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe = listOf(EsSertifikat1, EsSertifikat2, EsSertifikat3)

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort3)

        val sprak =
            EsSprak(fraIsoDato("2012-12-01"), "78983", "Dansk", "Danish", "Uforståelig")

        val sprakListe = listOf(sprak)

        val kurs1 = esKurs1()

        val kurs2 = esKurs5()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Harstad", "NO19.1903")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Nordland", "NO18")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Tromsø", "NO19.1902")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Industrimekaniker",
            true,
            emptyList()
        )

        val yrkeJobbonsker1 = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Lastebilsjåfør",
            true,
            emptyList()
        )

        val yrkeJobbonsker2 = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Butikkmedarbeider",
            true,
            emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker, yrkeJobbonsker1, yrkeJobbonsker2)

        return EsCv(
            nteAktorId(5),
            "03050316895",
            "BOB",
            "NORDMANN",
            LocalDate.parse("1964-09-01"),
            false,
            "ARBS",
            "bobob@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            "5L",
            "",
            "J",
            fraIsoDato("2016-05-30"),
            "Minvei 90",
            "",
            "",
            "0565",
            "OSLO",
            "NO",
            301,
            false,
            OffsetDateTime.now(),
            301,
            java.lang.Boolean.FALSE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            true,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            "Viken",
            "Lier",
            utdanningListe,
            emptyList(),
            yrkeserfaringListe,
            kompetanseListe,
            emptyList(),
            EsSertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeEsCvMedFeil(): EsCv {
        val utdanning = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "Otta vgs. Otta",
            "355211", "Mekaniske fag, grunnkurs", "GK maskin/mekaniker"
        )

        val utdanning2 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Høyskolen i Gjøvik", null, null, "Master i sikkerhet"
        )


        val utdanningsListe = listOf(utdanning, utdanning2)
        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            emptySet(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringsListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseList = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe = listOf(sertifikat1, sertifikat2, sertifikat3)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2)

        val sprak1 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1 =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        return EsCv(
            nteAktorId(1),
            "02016012345",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "ARBS",
            "unnasluntrer@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            "1L",
            "",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            301,
            false,
            OffsetDateTime.now(),
            301,
            java.lang.Boolean.FALSE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            true,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }


    fun giveMeEsCv6(): EsCv {
        val utdanning2 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe = listOf(utdanning2)

        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            emptySet(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            listOf(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringsListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseList = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe = listOf(sertifikat1, sertifikat2, sertifikat3, sertifikat4, sertifikat5)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkortListe = listOf(forerkort1)

        val sprak1 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()


        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Voss", "NO12.2001")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        return EsCv(
            nteAktorId(6),
            "01016034215",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "ARBS",
            "22339155@mailinator.com",
            "(+47) 22339155",
            "22339155",
            "NO",
            "6L",
            "",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            301,
            false,
            antallDagerTilbakeFraNow(10),
            301,
            java.lang.Boolean.FALSE,
            "5",
            null,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            true,
            true,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeCvForDoedPerson(): EsCv {
        val utdanning2 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe = listOf(utdanning2)

        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            emptySet(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringsListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseList = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe = listOf(sertifikat1, sertifikat2, sertifikat3, sertifikat4, sertifikat5)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak1 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        return EsCv(
            nteAktorId(7),
            "01016034215",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "ARBS",
            "22339155@mailinator.com",
            "(+47) 22339155",
            "22339155",
            "NO",
            "7L",
            "",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            301,
            false,
            OffsetDateTime.now(),
            301,
            java.lang.Boolean.TRUE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            false,
            false,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeCvForKode6(): EsCv {
        val utdanning2 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe = listOf(utdanning2)

        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            emptySet(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringsListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseList = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe = listOf(sertifikat1, sertifikat2, sertifikat3, sertifikat4, sertifikat5)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak1 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1 =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        return EsCv(
            nteAktorId(8),
            "01016034215",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "ARBS",
            "22339155@mailinator.com",
            "(+47) 22339155",
            "22339155",
            "NO",
            "8L",
            "",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            301,
            false,
            OffsetDateTime.now(),
            301,
            java.lang.Boolean.FALSE,
            "6",
            null,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            false,
            false,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeCvForKode7(): EsCv {
        val utdanning2 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe = listOf(utdanning2)

        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            emptySet(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industri'mekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringsListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseList = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe = listOf(sertifikat1, sertifikat2, sertifikat3, sertifikat4, sertifikat5)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak1 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        return EsCv(
            nteAktorId(9),
            "01016034215",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "ARBS",
            "22339155@mailinator.com",
            "(+47) 22339155",
            "22339155",
            "NO",
            "9L",
            "",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            301,
            false,
            OffsetDateTime.now(),
            301,
            java.lang.Boolean.FALSE,
            "7",
            null,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.FALSE,
            java.lang.Boolean.FALSE,
            false,
            false,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeCvFritattForKandidatsok(): EsCv {
        val utdanning2 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe = listOf(utdanning2)

        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            emptySet(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringsListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseList = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe = listOf(sertifikat1, sertifikat2, sertifikat3, sertifikat4, sertifikat5)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak1 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        return EsCv(
            nteAktorId(10),
            "01016034215",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "ARBS",
            "22339155@mailinator.com",
            "(+47) 22339155",
            "22339155",
            "NO",
            "10L",
            "",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            301,
            false,
            OffsetDateTime.now(),
            301,
            java.lang.Boolean.FALSE,
            null,
            ikval,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.TRUE,
            java.lang.Boolean.FALSE,
            false,
            false,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeCvFritattForAgKandidatsok(): EsCv {
        val utdanning2 = lagEsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe = listOf(utdanning2)

        val yrkeserfaring1 = lagEsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            emptySet(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring2 = lagEsYrkeserfaring(
            nåMinusÅr(11),
            nåMinusÅr(6),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring3 = lagEsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            setOf(anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring4 = lagEsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring5 = lagEsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            setOf("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaring6 = lagEsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            setOf("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            emptyList(),
            "Oslo"
        )

        val yrkeserfaringsListe = listOf(yrkeserfaring1, yrkeserfaring2, yrkeserfaring3, yrkeserfaring4, yrkeserfaring5, yrkeserfaring6)

        val kompetanse1 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, emptyList()
        )

        val kompetanse2 = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, emptyList()
        )

        val kompetanse3 = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, emptyList()
        )

        val kompetanse4 = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, emptyList()
        )

        val kompetanseList = listOf(kompetanse1, kompetanse2, kompetanse3, kompetanse4)

        val sertifikat1 = lagEsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2 = lagEsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3 = lagEsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5 = lagEsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe = listOf(sertifikat1, sertifikat2, sertifikat3, sertifikat4, sertifikat5)

        val forerkort1 = lagEsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = lagEsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = lagEsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe = listOf(forerkort1, forerkort2, forerkort3, forerkort4)

        val sprak1 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2 =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe = listOf(sprak1, sprak2)

        val kurs1 = esKurs1()

        val kurs2 = esKurs2()

        val kurs3 = esKurs3()

        val kursListe = listOf(kurs1, kurs2, kurs3)

        val geografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1 = EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2 = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe = listOf(geografiJobbonsker, geografiJobbonsker1, geografiJobbonsker2)

        val yrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, emptyList()
        )

        val yrkeJobbonskerListe = listOf(yrkeJobbonsker)

        return EsCv(
            nteAktorId(11),
            "01016034215",
            "OLA",
            "NORDMANN",
            LocalDate.parse("1960-01-01"),
            false,
            "ARBS",
            "22339155@mailinator.com",
            "(+47) 22339155",
            "22339155",
            "NO",
            "11L",
            "",
            "N",
            fraIsoDato("2016-05-30"),
            "Minvei 1",
            "",
            "",
            "0654",
            "OSLO",
            "NO",
            301,
            false,
            antallDagerTilbakeFraNow(8),
            301,
            java.lang.Boolean.FALSE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            java.lang.Boolean.FALSE,
            java.lang.Boolean.TRUE,
            false,
            true,
            null,
            "0301",
            "H149390",
            "Olle Svenske",
            "olle@svenske.se",
            null,
            null,
            utdanningsListe,
            emptyList(),
            yrkeserfaringsListe,
            kompetanseList,
            emptyList(),
            sertifikatListe,
            forerkortListe,
            sprakListe,
            kursListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            null
        )
    }

    fun giveMeYrkeserfaring() = lagEsYrkeserfaring(
        fraIsoDato("2016-06-01"),
        fraIsoDato("2016-07-01"),
        "MTM anlegg",
        "8332.03",
        "Lastebil- og trailersjåfør",
        setOf("Lastebil- og trailersjåfør"),
        "Sjåfør kl. 2",
        "YRKE_ORGNR",
        "YRKE_NACEKODE",
        false,
        emptyList(),
        "Oslo"
    )

    fun giveMeUtdanning() = lagEsUtdanning(
        fraIsoDato("1988-08-20"),
        fraIsoDato("1989-06-20"),
        "UiO",
        "838838",
        "Sosialantropologiske fag",
        "Sosialantropologi gr. fag"
    )

    fun giveMeFørerkort() =
        lagEsForerkort(fraIsoDato("1994-08-01"), null, "V1.6050", "A - Tung motorsykkel", null, "")

    fun giveMeKurs() = esKurs1()

    fun giveMeFagdokumentasjon() = EsFagdokumentasjon("anyType", "anyTittel", "anyBeskrivelse")

    fun giveMeAnnenErfaring() = lagEsAnnenErfaring(
        fraIsoDato("2005-01-01"),
        fraIsoDato("2010-12-31"),
        "anyBeskrivelse"
    )

    fun giveMeGodkjenning() = lagEsGodkjenning(
        "anyTittel",
        "anyUtsteder",
        fraIsoDato("2020-06-01"),
        fraIsoDato("2050-01-01"),
        "anyKonseptId"
    )
}
