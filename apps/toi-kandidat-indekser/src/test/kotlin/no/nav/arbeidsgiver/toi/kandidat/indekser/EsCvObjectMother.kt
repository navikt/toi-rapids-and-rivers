package no.nav.arbeidsgiver.toi.kandidat.indekser

import no.nav.arbeid.pam.kodeverk.ansettelse.Ansettelsesform
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidsdager
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidstid
import no.nav.arbeid.pam.kodeverk.ansettelse.Arbeidstidsordning
import no.nav.arbeid.pam.kodeverk.ansettelse.Omfang
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsAnsettelsesformJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsArbeidsdagerJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsArbeidstidJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsArbeidstidsordningJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsForerkort
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsGeografiJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsKompetanse
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsKurs
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsOmfangJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsSertifikat
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsSprak
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsUtdanning
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsVerv
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeJobbonsker
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsYrkeserfaring
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Set

const val anleggsmaskinfører = "Anleggsmaskinfører"
const val ikval = "IKVAL"

object EsCvObjectMother {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val LOGGER = LoggerFactory.getLogger(EsCvObjectMother::class.java)
    private val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

    fun nteAktorId(n: Int) = (900000000000L + n * 1000L).toString()

    private fun fraIsoDato(string: String?) = sdf.parse(string)/*try {
        if (string == null) null else sdf.parse(string)
    } catch (e: java.text.ParseException) {
        LOGGER.error("Feilet å parse $string", e)
        null
    }*/

    fun antallDagerTilbakeFraNow(antallDager: Int) =
        Date.from(LocalDateTime.now().minusDays(antallDager.toLong()).atZone(ZoneId.systemDefault()).toInstant())

    private fun fodselsdatoForAlder(alder: Int) =
        LocalDate.now().minusYears(alder.toLong()).format(formatter)

    private fun nåMinusÅr(antallÅr: Int): Date {
        val c = Calendar.getInstance()
        c.add(Calendar.YEAR, -antallÅr)
        return c.getTime()
    }

    fun giveMeEsCv(): EsCv {
        val utdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "Otta vgs. Otta",
            "355211", "Mekaniske fag, grunnkurs", "GK maskin/mekaniker"
        )

        val utdanning1 = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Høyskolen i Gjøvik", "786595", "Master i sikkerhet", "Master i sikkerhet"
        )


        val utdanningsListe = mutableListOf<EsUtdanning>()
        utdanningsListe.add(utdanning)
        utdanningsListe.add(utdanning1)
        val yrkeserfaring1 = EsYrkeserfaring(
            fraIsoDato("2000-01-01"), fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø", "8341.01", "Anleggsmaskindrifter",
            setOf("Anleggsmaskindrifter", "Anleggsmaskinoperatør"), "maskinkjører og maskintransport", "YRKE_ORGNR",
            "YRKE_NACEKODE", false, listOf(), "Oslo")

        val yrkeserfaring2 = EsYrkeserfaring(
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

        val yrkeserfaring3 = EsYrkeserfaring(
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

        val yrkeserfaring4 = EsYrkeserfaring(
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

        val yrkeserfaring5 = EsYrkeserfaring(
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

        val yrkeserfaring6 = EsYrkeserfaring(
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

        val sertifikat1 = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat2 = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat3 = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe= listOf(sertifikat1, sertifikat2, sertifikat3)

        val forerkort1 = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = EsForerkort(
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

        val verv = EsVerv(
            fraIsoDato("2000-01-15"),
            fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe= listOf(verv)

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
            "1960-01-01",
            false,
            "JOBBS",
            "unnasluntrer@mailinator.com",
            "(+47) 22334455",
            "12345678",
            "NO",
            "1L",
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
            vervListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            omfangJobbonskerList,
            ansettelsesforholdJobbonskerListe,
            arbeidstidsordningJobbonskerListe,
            arbeidsdagerJobbonskerList,
            arbeidstidJobbonskerList,
            emptyList(),
            emptyList()
        )
    }

    private fun esKurs1() = EsKurs(
        "Akseloppretting", "Easy-Laser", null,
        null, fraIsoDato("2012-12-01")
    )

    private fun esKurs2() = EsKurs(
        "Varme arbeider Sertifikat",
        "Norsk brannvernforening", "ÅR", 5, fraIsoDato("2015-06-01")
    )

    private fun esKurs3() = EsKurs(
        "Flensarbeid for Norsk Olje og Gass",
        "Torqlite Europa a/s", "ÅR", 4, fraIsoDato("2016-02-01")
    )

    private fun esKurs4() = EsKurs(
        "Varme arbeider EsSertifikat",
        "Norsk brannvernforening", "ÅR", 5, fraIsoDato("2015-06-01")
    )

    private fun esKurs5() = EsKurs("Spring Boot", "Spring-folkene", "ÅR", 5, fraIsoDato("2015-06-01"))


    fun giveMeEsCv2(): EsCv {
        val EsUtdanning =
            EsUtdanning(
                fraIsoDato("1999-08-20"),
                fraIsoDato("2002-06-20"),
                "Hamar Katedralskole",
                "296647",
                "Studiespesialisering",
                "Studiespesialisering med realfag"
            )

        val utdanningListe = listOf(EsUtdanning)
        val yrkeserfaring1 = EsYrkeserfaring(
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

        val yrkeserfaring2 = EsYrkeserfaring(
            fraIsoDato("2003-01-01"), fraIsoDato("2003-07-01"),
            "Programvarefabrikken Førde", "5746.07", "Systemutvikler",
            setOf("Systemutvikler", "Java-utvikler"),
            "Utvikling av nytt kandidatsøk", "YRKE_ORGNR", "YRKE_NACEKODE", false, emptyList(), "Oslo")

        val yrkeserfaring3 = EsYrkeserfaring(
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

        val yrkeserfaring4 = EsYrkeserfaring(
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

        val yrkeserfaring5 = EsYrkeserfaring(
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

        val yrkeserfaring6 = EsYrkeserfaring(
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

        val EsSertifikat1 = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2 = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3 = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe = listOf(EsSertifikat1, EsSertifikat2, EsSertifikat3)

        val forerkort1 = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2 = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3 = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4 = EsForerkort(
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

        val verv = EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "EsVerv organisasjon",
                "verv tittel"
            )

        val vervListe = listOf(verv)

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
            vervListe,
            geografiJobbonskerListe,
            yrkeJobbonskerListe,
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )
    }

    fun giveMeEsCv3(): EsCv {
        val esUtdanning: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Norges Naturvitenskapelige Universitet", "456375", "Sosiologi", "Sosiologi"
        )

        val utdanningListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningListe.add(esUtdanning)
        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-02-01"),
            "Butikken i nærheten",
            "1010.01",
            "Butikkmedarbeider",
            Set.< E > of < E ? > ("Butikkmedarbeider"),
            "Butikkmedarbeider i Førde",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"), fraIsoDato("2003-02-01"),
            "Butikken i nærheten", "1010.01", "Butikkmedarbeider(dagligvarer)",
            Set.< E > of < E ? > ("Butikkmedarbeider(dagligvarer)", "Butikkmedarbeider"
        ), "Butikkmedarbeider(dagligvarer)", "YRKE_ORGNR", "YRKE_NACEKODE", false, kotlin.collections.mutableListOf<T?>(), "Oslo")

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "Butikken langt unna",
            "1010.01",
            "Butikkmedarbeider(trevare)",
            Set.< E > of < E ? > ("Butikkmedarbeider(trevare)"),
            "Butikkmedarbeider(trevare)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Butikken",
            "4561.03",
            "Butikkmedarbeider(elektronikk)",
            Set.< E > of < E ? > ("Butikkmedarbeider(elektronikk)"),
            "Butikkmedarbeider(elektronikk)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2016-06-01"),
                fraIsoDato("2016-07-01"),
                "Tvkanalen TV?",
                "5684.05",
                "Presentør",
                Set.< E > of < E ? > ("Presentør"),
                "Presentør",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                kotlin.collections.mutableListOf<T?>(),
                "Oslo"
            )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            null,
            "NLI  Grenland",
            "5684.05",
            "Nyhetsanker",
            Set.< E > of < E ? > ("Nyhetsanker"),
            "Nyhetsanker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringListe.add(yrkeserfaring1)
        yrkeserfaringListe.add(yrkeserfaring2)
        yrkeserfaringListe.add(yrkeserfaring3)
        yrkeserfaringListe.add(yrkeserfaring4)
        yrkeserfaringListe.add(yrkeserfaring5)
        yrkeserfaringListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "152424",
                "Presentør",
                null,
                null,
                kotlin.collections.mutableListOf<T?>()
            )

        val kompetanse2: EsKompetanse =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "152424",
                "Nyhetsanker",
                "Nyhetsanker",
                null,
                kotlin.collections.mutableListOf<T?>()
            )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "566895", "Butikkmedarbeider",
            "Butikkmedarbeider", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "566895",
            "Butikkmedarbeider(trevare)", "Butikkmedarbeider(trevare)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseListe: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseListe.add(kompetanse1)
        kompetanseListe.add(kompetanse2)
        kompetanseListe.add(kompetanse3)
        kompetanseListe.add(kompetanse4)


        val EsSertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Truckførerbevis", "Truckførerbevis", ""
        )
        val EsSertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat4: EsSertifikat = EsSertifikat(
            fraIsoDato("1999-01-01"), null, "L2.7000",
            "Truckførerbevis T1 Lavtløftende plukktruck, palletruck m/perm. førerplass",
            "Truckførerbevis T1 Lavtløftende plukktruck, palletruck m/perm. førerplass", ""
        )

        val esSertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        esSertifikatListe.add(EsSertifikat1)
        esSertifikatListe.add(EsSertifikat2)
        esSertifikatListe.add(EsSertifikat3)
        esSertifikatListe.add(EsSertifikat4)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2: EsForerkort = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "T - Traktor", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort2)
        forerkortListe.add(forerkort3)
        forerkortListe.add(forerkort4)


        val sprak: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "87392", "Engelsk", "English", "Flytende")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs4()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "EsVerv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Bergen", "NO12.1201")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)

        val yrkeJobbonsker: EsYrkeJobbonsker =
            EsYrkeJobbonsker("Yrke jobb ønskeStyrk Kode", "Ordfører", true, kotlin.collections.mutableListOf<T?>())

        val yrkeJobbonsker1: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Barnehageassistent",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonsker2: EsYrkeJobbonsker =
            EsYrkeJobbonsker("Yrke jobb ønskeStyrk Kode", "Tester", true, kotlin.collections.mutableListOf<T?>())

        val yrkeJobbonsker3: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Konsulent (data)",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)
        yrkeJobbonskerListe.add(yrkeJobbonsker1)
        yrkeJobbonskerListe.add(yrkeJobbonsker2)
        yrkeJobbonskerListe.add(yrkeJobbonsker3)

        val esCv: EsCv = EsCv(
            nteAktorId(3),
            "04265983651",
            "HANS",
            "NORDMANN",
            "1955-11-04",
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
            "Drammen"
        )
        esCv.addUtdanning(utdanningListe)
        esCv.addYrkeserfaring(yrkeserfaringListe)
        esCv.addKompetanse(kompetanseListe)
        esCv.addSertifikat(esSertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeEsCv4(): EsCv {
        val EsUtdanning: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Norges Naturvitenskapelige Universitet", "456375", "Bygg og anlegg",
            "Bygg og anlegg"
        )

        val utdanningListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningListe.add(EsUtdanning)
        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2002-01-01"),
            "Jokah",
            "1010.01",
            "Butikkmedarbeider",
            Set.< E > of < E ? > ("Butikkmedarbeider"),
            "Butikkmedarbeider",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-04-01"),
            "Nærbutikkern",
            "1010.01",
            "Butikkmedarbeider(klesbutikk)",
            Set.< E > of < E ? > ("Butikkmedarbeider(klesbutikk)"),
            "Butikkmedarbeider(klebutikk)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2003-04-01"),
                fraIsoDato("2003-07-01"),
                "Tv tv tv",
                "5684.05",
                "Nyhetspresentør",
                Set.< E > of < E ? > ("Nyhetspresentør"),
                "Nyhetspresentør",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                kotlin.collections.mutableListOf<T?>(),
                "Oslo"
            )

        val yrkeserfaring4: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2005-08-01"),
                fraIsoDato("2016-07-01"),
                "Vard Group,avd.Brevik",
                "5684.05",
                "Hallovert",
                Set.< E > of < E ? > ("Hallovert"),
                "Hallovert",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                kotlin.collections.mutableListOf<T?>(),
                "Oslo"
            )

        val yrkeserfaring5: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2016-06-01"),
                fraIsoDato("2017-04-01"),
                "DN teater",
                "5124.46",
                "Skuespiller",
                Set.< E > of < E ? > ("Skuespiller"),
                "Skuespiller",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                kotlin.collections.mutableListOf<T?>(),
                "Oslo"
            )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            null,
            "Dukketeateret Rena",
            "5124.46",
            "Skuespiller(dukketeater)",
            Set.< E > of < E ? > ("Skuespiller(dukketeater)"),
            "Skuespiller(dukketeater)",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringListe.add(yrkeserfaring1)
        yrkeserfaringListe.add(yrkeserfaring2)
        yrkeserfaringListe.add(yrkeserfaring3)
        yrkeserfaringListe.add(yrkeserfaring4)
        yrkeserfaringListe.add(yrkeserfaring5)
        yrkeserfaringListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "152424",
                "Hallovert",
                null,
                null,
                kotlin.collections.mutableListOf<T?>()
            )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "566895", "Butikkmedarbeider",
            "Butikkmedarbeider", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"),
            "564646",
            "Butikkmedarbeider(klesbutikk)",
            "Butikkmedarbeider(klesbutikk)",
            null,
            kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse =
            EsKompetanse(
                fraIsoDato("2016-03-14"),
                "506",
                "Skuespiller",
                "Skuespiller",
                null,
                kotlin.collections.mutableListOf<T?>()
            )

        val kompetanseListe: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseListe.add(kompetanse1)
        kompetanseListe.add(kompetanse2)
        kompetanseListe.add(kompetanse3)
        kompetanseListe.add(kompetanse4)


        val EsSertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        EsSertifikatListe.add(EsSertifikat1)
        EsSertifikatListe.add(EsSertifikat2)
        EsSertifikatListe.add(EsSertifikat3)

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkort5: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "S - Snøscooter", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort4)
        forerkortListe.add(forerkort5)


        val sprak: EsSprak = EsSprak(fraIsoDato("2012-12-01"), "78985", "Tysk", "German", "Begynner")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs4()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "EsVerv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Oslo", "NO03.0301")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Akershus", "NO02")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Bærum", "NO02.1219")

        val geografiJobbonsker3: EsGeografiJobbonsker = EsGeografiJobbonsker("Norge", "NO")

        val geografiJobbonsker4: EsGeografiJobbonsker = EsGeografiJobbonsker("Karasjok", "NO02.2021")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)
        geografiJobbonskerListe.add(geografiJobbonsker3)
        geografiJobbonskerListe.add(geografiJobbonsker4)

        val yrkeJobbonsker: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Butikkmedarbeider",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonsker1: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Butikkmedarbeider",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)
        yrkeJobbonskerListe.add(yrkeJobbonsker1)

        val esCv: EsCv = EsCv(
            nteAktorId(4),
            "09568410230",
            "HANNE",
            "NORDMANN",
            "2002-06-04",
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
            "Oslo"
        )
        esCv.addUtdanning(utdanningListe)
        esCv.addYrkeserfaring(yrkeserfaringListe)
        esCv.addKompetanse(kompetanseListe)
        esCv.addSertifikat(EsSertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeEsCv5(): EsCv {
        val utdanningListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()


        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-02-01"),
            "Bankhvelvet BBL",
            "4865.75",
            "Bankhvelvoperatør",
            Set.< E > of < E ? > ("Bankhvelvoperatør"),
            "Bankhvelvoperatør",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2003-01-01"),
                fraIsoDato("2003-02-01"),
                "Proggehula",
                "5746.07",
                "Progger",
                Set.< E > of < E ? > ("Progger"),
                "Progger",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                kotlin.collections.mutableListOf<T?>(),
                "Oslo"
            )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "Test a.a.s",
            "6859.02",
            "Tester",
            Set.< E > of < E ? > ("Tester"),
            "Tester",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2005-08-01"),
                fraIsoDato("2005-09-01"),
                "K.O. kranservice",
                "8342.01",
                "Kranoperatør",
                Set.< E > of < E ? > ("Kranoperatør"),
                "Kranoperatør",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                java.util.List.< E > of < E ? > ("Operatør av kran"),
                "Oslo"
            )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-06-02"),
            "Lang transport A.S.",
            "8332.03",
            "Lastebil- \"Lastebil- og trailersjåførog trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- \"Lastebil- og trailersjåførog trailersjåfør"),
            "Sjåfør kl. 3",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato(null),
            "Mekken mekk",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringListe.add(yrkeserfaring1)
        yrkeserfaringListe.add(yrkeserfaring2)
        yrkeserfaringListe.add(yrkeserfaring3)
        yrkeserfaringListe.add(yrkeserfaring4)
        yrkeserfaringListe.add(yrkeserfaring5)
        yrkeserfaringListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid(type 2 kran)", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid spesielt", "Mekanisk arbeid spesielt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3220201",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseListe: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseListe.add(kompetanse1)
        kompetanseListe.add(kompetanse2)
        kompetanseListe.add(kompetanse3)
        kompetanseListe.add(kompetanse4)


        val EsSertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        EsSertifikatListe.add(EsSertifikat1)
        EsSertifikatListe.add(EsSertifikat2)
        EsSertifikatListe.add(EsSertifikat3)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort4)


        val sprak: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78983", "Dansk", "Danish", "Uforståelig")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs =
            esKurs5()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "EsVerv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Harstad", "NO19.1903")

        val geografiJobbonsker1: EsGeografiJobbonsker = EsGeografiJobbonsker("Sunnhordaland", "NO12.2200")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Tromsø", "NO19.1902")

        val geografiJobbonsker3: EsGeografiJobbonsker = EsGeografiJobbonsker("Jessheim", "NO02.0219")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)
        geografiJobbonskerListe.add(geografiJobbonsker3)

        val yrkeJobbonsker: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Industrimekaniker",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonsker1: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Lastebilsjåfør",
                true,
                kotlin.collections.mutableListOf<T?>("Trailersjåffis")
            )

        val yrkeJobbonsker2: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Butikkmedarbeider",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonsker3: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Konsulent (bank)",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)
        yrkeJobbonskerListe.add(yrkeJobbonsker1)
        yrkeJobbonskerListe.add(yrkeJobbonsker2)
        yrkeJobbonskerListe.add(yrkeJobbonsker3)

        val esCv: EsCv = EsCv(
            nteAktorId(5),
            "03050316895",
            "BOB",
            "NORDMANN",
            "1964-09-01",
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
            null
        )
        esCv.addUtdanning(utdanningListe)
        esCv.addYrkeserfaring(yrkeserfaringListe)
        esCv.addKompetanse(kompetanseListe)
        esCv.addSertifikat(EsSertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeCvUtenKompetanse(): EsCv {
        val utdanningListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()


        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-02-01"),
            "Bankhvelvet BBL",
            "4865.75",
            "Bankhvelvoperatør",
            Set.< E > of < E ? > ("Bankhvelvoperatør"),
            "Bankhvelvoperatør",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2003-01-01"),
                fraIsoDato("2003-02-01"),
                "Proggehula",
                "5746.07",
                "Progger",
                Set.< E > of < E ? > ("Progger"),
                "Progger",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                kotlin.collections.mutableListOf<T?>(),
                "Oslo"
            )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "Test a.a.s",
            "6859.02",
            "Tester",
            Set.< E > of < E ? > ("Tester"),
            "Tester",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring =
            EsYrkeserfaring(
                fraIsoDato("2005-08-01"),
                fraIsoDato("2005-09-01"),
                "K.O. kranservice",
                "8342.01",
                "Kranoperatør",
                Set.< E > of < E ? > ("Kranoperatør"),
                "Kranoperatør",
                "YRKE_ORGNR",
                "YRKE_NACEKODE",
                false,
                kotlin.collections.mutableListOf<T?>(),
                "Oslo"
            )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-06-02"),
            "Lang transport A.S.",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 3",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-11-01"),
            "Mekken mekk",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringListe.add(yrkeserfaring1)
        yrkeserfaringListe.add(yrkeserfaring2)
        yrkeserfaringListe.add(yrkeserfaring3)
        yrkeserfaringListe.add(yrkeserfaring4)
        yrkeserfaringListe.add(yrkeserfaring5)
        yrkeserfaringListe.add(yrkeserfaring6)

        val kompetanseListe: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()


        val EsSertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val EsSertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val EsSertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        EsSertifikatListe.add(EsSertifikat1)
        EsSertifikatListe.add(EsSertifikat2)
        EsSertifikatListe.add(EsSertifikat3)

        val forerkort3: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort3)


        val sprak: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78983", "Dansk", "Danish", "Uforståelig")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs =
            esKurs5()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "EsVerv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Harstad", "NO19.1903")

        val geografiJobbonsker1: EsGeografiJobbonsker = EsGeografiJobbonsker("Nordland", "NO18")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Tromsø", "NO19.1902")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Industrimekaniker",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonsker1: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Lastebilsjåfør",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonsker2: EsYrkeJobbonsker =
            EsYrkeJobbonsker(
                "Yrke jobb ønskeStyrk Kode",
                "Butikkmedarbeider",
                true,
                kotlin.collections.mutableListOf<T?>()
            )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)
        yrkeJobbonskerListe.add(yrkeJobbonsker1)
        yrkeJobbonskerListe.add(yrkeJobbonsker2)

        val esCv: EsCv = EsCv(
            nteAktorId(5),
            "03050316895",
            "BOB",
            "NORDMANN",
            "1964-09-01",
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
            Date(),
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
            "Lier"
        )
        esCv.addUtdanning(utdanningListe)
        esCv.addYrkeserfaring(yrkeserfaringListe)
        esCv.addKompetanse(kompetanseListe)
        esCv.addSertifikat(EsSertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeEsCvMedFeil(): EsCv {
        val utdanning: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "Otta vgs. Otta",
            "355211", "Mekaniske fag, grunnkurs", "GK maskin/mekaniker"
        )

        val utdanning2: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"),
            "Høyskolen i Gjøvik", null, null, "Master i sikkerhet"
        )


        val utdanningsListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningsListe.add(utdanning)
        utdanningsListe.add(utdanning2)
        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            kotlin.collections.mutableSetOf<E?>(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringsListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringsListe.add(yrkeserfaring1)
        yrkeserfaringsListe.add(yrkeserfaring2)
        yrkeserfaringsListe.add(yrkeserfaring3)
        yrkeserfaringsListe.add(yrkeserfaring4)
        yrkeserfaringsListe.add(yrkeserfaring5)
        yrkeserfaringsListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseList: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseList.add(kompetanse1)
        kompetanseList.add(kompetanse2)
        kompetanseList.add(kompetanse3)
        kompetanseList.add(kompetanse4)


        val sertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        sertifikatListe.add(sertifikat1)
        sertifikatListe.add(sertifikat2)
        sertifikatListe.add(sertifikat3)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2: EsForerkort = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort2)


        val sprak1: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak1)
        sprakListe.add(sprak2)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs2()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, kotlin.collections.mutableListOf<T?>()
        )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)

        val esCv: EsCv = EsCv(
            nteAktorId(1),
            "02016012345",
            "OLA",
            "NORDMANN",
            "1960-01-01",
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
            Date(),
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
            null
        )
        esCv.addUtdanning(utdanningsListe)
        esCv.addYrkeserfaring(yrkeserfaringsListe)
        esCv.addKompetanse(kompetanseList)
        esCv.addSertifikat(sertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }


    fun giveMeEsCv6(): EsCv {
        val utdanning2: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningsListe.add(utdanning2)

        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            kotlin.collections.mutableSetOf<E?>(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringsListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringsListe.add(yrkeserfaring1)
        yrkeserfaringsListe.add(yrkeserfaring2)
        yrkeserfaringsListe.add(yrkeserfaring3)
        yrkeserfaringsListe.add(yrkeserfaring4)
        yrkeserfaringsListe.add(yrkeserfaring5)
        yrkeserfaringsListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseList: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseList.add(kompetanse1)
        kompetanseList.add(kompetanse2)
        kompetanseList.add(kompetanse3)
        kompetanseList.add(kompetanse4)

        val sertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        sertifikatListe.add(sertifikat1)
        sertifikatListe.add(sertifikat2)
        sertifikatListe.add(sertifikat3)
        sertifikatListe.add(sertifikat4)
        sertifikatListe.add(sertifikat5)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)

        val sprak1: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak1)
        sprakListe.add(sprak2)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs2()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Voss", "NO12.2001")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, kotlin.collections.mutableListOf<T?>()
        )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)

        val esCv: EsCv = EsCv(
            nteAktorId(6),
            "01016034215",
            "OLA",
            "NORDMANN",
            "1960-01-01",
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
            null
        )
        esCv.addUtdanning(utdanningsListe)
        esCv.addYrkeserfaring(yrkeserfaringsListe)
        esCv.addKompetanse(kompetanseList)
        esCv.addSertifikat(sertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeCvForDoedPerson(): EsCv {
        val utdanning2: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningsListe.add(utdanning2)

        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            kotlin.collections.mutableSetOf<E?>(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringsListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringsListe.add(yrkeserfaring1)
        yrkeserfaringsListe.add(yrkeserfaring2)
        yrkeserfaringsListe.add(yrkeserfaring3)
        yrkeserfaringsListe.add(yrkeserfaring4)
        yrkeserfaringsListe.add(yrkeserfaring5)
        yrkeserfaringsListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseList: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseList.add(kompetanse1)
        kompetanseList.add(kompetanse2)
        kompetanseList.add(kompetanse3)
        kompetanseList.add(kompetanse4)

        val sertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        sertifikatListe.add(sertifikat1)
        sertifikatListe.add(sertifikat2)
        sertifikatListe.add(sertifikat3)
        sertifikatListe.add(sertifikat4)
        sertifikatListe.add(sertifikat5)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2: EsForerkort = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort2)
        forerkortListe.add(forerkort3)
        forerkortListe.add(forerkort4)


        val sprak1: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak1)
        sprakListe.add(sprak2)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs2()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, kotlin.collections.mutableListOf<T?>()
        )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)

        val esCv: EsCv = EsCv(
            nteAktorId(7),
            "01016034215",
            "OLA",
            "NORDMANN",
            "1960-01-01",
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
            Date(),
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
            null
        )
        esCv.addUtdanning(utdanningsListe)
        esCv.addYrkeserfaring(yrkeserfaringsListe)
        esCv.addKompetanse(kompetanseList)
        esCv.addSertifikat(sertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeCvForKode6(): EsCv {
        val utdanning2: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningsListe.add(utdanning2)

        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            kotlin.collections.mutableSetOf<E?>(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringsListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringsListe.add(yrkeserfaring1)
        yrkeserfaringsListe.add(yrkeserfaring2)
        yrkeserfaringsListe.add(yrkeserfaring3)
        yrkeserfaringsListe.add(yrkeserfaring4)
        yrkeserfaringsListe.add(yrkeserfaring5)
        yrkeserfaringsListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseList: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseList.add(kompetanse1)
        kompetanseList.add(kompetanse2)
        kompetanseList.add(kompetanse3)
        kompetanseList.add(kompetanse4)

        val sertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        sertifikatListe.add(sertifikat1)
        sertifikatListe.add(sertifikat2)
        sertifikatListe.add(sertifikat3)
        sertifikatListe.add(sertifikat4)
        sertifikatListe.add(sertifikat5)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2: EsForerkort = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort2)
        forerkortListe.add(forerkort3)
        forerkortListe.add(forerkort4)


        val sprak1: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak1)
        sprakListe.add(sprak2)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs2()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, kotlin.collections.mutableListOf<T?>()
        )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)

        val esCv: EsCv = EsCv(
            nteAktorId(8),
            "01016034215",
            "OLA",
            "NORDMANN",
            "1960-01-01",
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
            Date(),
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
            null
        )
        esCv.addUtdanning(utdanningsListe)
        esCv.addYrkeserfaring(yrkeserfaringsListe)
        esCv.addKompetanse(kompetanseList)
        esCv.addSertifikat(sertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeCvForKode7(): EsCv {
        val utdanning2: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningsListe.add(utdanning2)

        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            kotlin.collections.mutableSetOf<E?>(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industri'mekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringsListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringsListe.add(yrkeserfaring1)
        yrkeserfaringsListe.add(yrkeserfaring2)
        yrkeserfaringsListe.add(yrkeserfaring3)
        yrkeserfaringsListe.add(yrkeserfaring4)
        yrkeserfaringsListe.add(yrkeserfaring5)
        yrkeserfaringsListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseList: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseList.add(kompetanse1)
        kompetanseList.add(kompetanse2)
        kompetanseList.add(kompetanse3)
        kompetanseList.add(kompetanse4)

        val sertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        sertifikatListe.add(sertifikat1)
        sertifikatListe.add(sertifikat2)
        sertifikatListe.add(sertifikat3)
        sertifikatListe.add(sertifikat4)
        sertifikatListe.add(sertifikat5)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2: EsForerkort = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort2)
        forerkortListe.add(forerkort3)
        forerkortListe.add(forerkort4)


        val sprak1: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak1)
        sprakListe.add(sprak2)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs2()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, kotlin.collections.mutableListOf<T?>()
        )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)

        val esCv: EsCv = EsCv(
            nteAktorId(9),
            "01016034215",
            "OLA",
            "NORDMANN",
            "1960-01-01",
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
            Date(),
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
            null
        )
        esCv.addUtdanning(utdanningsListe)
        esCv.addYrkeserfaring(yrkeserfaringsListe)
        esCv.addKompetanse(kompetanseList)
        esCv.addSertifikat(sertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeCvFritattForKandidatsok(): EsCv {
        val utdanning2: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningsListe.add(utdanning2)

        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            kotlin.collections.mutableSetOf<E?>(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-01-01"),
            fraIsoDato("2003-02-01"),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringsListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringsListe.add(yrkeserfaring1)
        yrkeserfaringsListe.add(yrkeserfaring2)
        yrkeserfaringsListe.add(yrkeserfaring3)
        yrkeserfaringsListe.add(yrkeserfaring4)
        yrkeserfaringsListe.add(yrkeserfaring5)
        yrkeserfaringsListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseList: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseList.add(kompetanse1)
        kompetanseList.add(kompetanse2)
        kompetanseList.add(kompetanse3)
        kompetanseList.add(kompetanse4)

        val sertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        sertifikatListe.add(sertifikat1)
        sertifikatListe.add(sertifikat2)
        sertifikatListe.add(sertifikat3)
        sertifikatListe.add(sertifikat4)
        sertifikatListe.add(sertifikat5)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2: EsForerkort = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort2)
        forerkortListe.add(forerkort3)
        forerkortListe.add(forerkort4)


        val sprak1: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak1)
        sprakListe.add(sprak2)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs2()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, kotlin.collections.mutableListOf<T?>()
        )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)

        val esCv: EsCv = EsCv(
            nteAktorId(10),
            "01016034215",
            "OLA",
            "NORDMANN",
            "1960-01-01",
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
            Date(),
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
            null
        )
        esCv.addUtdanning(utdanningsListe)
        esCv.addYrkeserfaring(yrkeserfaringsListe)
        esCv.addKompetanse(kompetanseList)
        esCv.addSertifikat(sertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeCvFritattForAgKandidatsok(): EsCv {
        val utdanning2: EsUtdanning = EsUtdanning(
            fraIsoDato("1988-08-20"), fraIsoDato("1989-06-20"), "UiO", "838838",
            "Sosialantropologiske fag", "Sosialantropologi gr. fag"
        )


        val utdanningsListe: java.util.ArrayList<EsUtdanning?> = java.util.ArrayList<EsUtdanning?>()
        utdanningsListe.add(utdanning2)

        val yrkeserfaring1: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2000-01-01"),
            fraIsoDato("2000-01-10"),
            "Stentransport, Kragerø",
            "8341.01",
            "",
            kotlin.collections.mutableSetOf<E?>(),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring2: EsYrkeserfaring = EsYrkeserfaring(
            nåMinusÅr(11),
            nåMinusÅr(6),
            "AF-Pihl, Hammerfest",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring3: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2003-04-01"),
            fraIsoDato("2003-05-01"),
            "O.K. Hagalia, Slependen",
            "8342.01",
            anleggsmaskinfører,
            Set.< E > of < E ? > (anleggsmaskinfører),
            "maskinkjører og maskintransport",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring4: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2005-08-01"),
            fraIsoDato("2005-09-01"),
            "Vard Group,avd.Brevik",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring5: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaring6: EsYrkeserfaring = EsYrkeserfaring(
            fraIsoDato("2017-10-01"),
            fraIsoDato("2017-12-01"),
            "NLI  Grenland",
            "7233.03",
            "Industrimekaniker",
            Set.< E > of < E ? > ("Industrimekaniker"),
            "Industrimekaniker",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )

        val yrkeserfaringsListe: java.util.ArrayList<EsYrkeserfaring?> = java.util.ArrayList<EsYrkeserfaring?>()
        yrkeserfaringsListe.add(yrkeserfaring1)
        yrkeserfaringsListe.add(yrkeserfaring2)
        yrkeserfaringsListe.add(yrkeserfaring3)
        yrkeserfaringsListe.add(yrkeserfaring4)
        yrkeserfaringsListe.add(yrkeserfaring5)
        yrkeserfaringsListe.add(yrkeserfaring6)

        val kompetanse1: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3020813",
            "Maskin- og kranførerarbeid", null, null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse2: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "3281301",
            "Mekanisk arbeid generelt", "Mekanisk arbeid generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse3: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "506",
            "Landtransport generelt", "Landtransport generelt", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanse4: EsKompetanse = EsKompetanse(
            fraIsoDato("2016-03-14"), "212", "Industri (bransje)",
            "Mekanisk industri (bransje)", null, kotlin.collections.mutableListOf<T?>()
        )

        val kompetanseList: java.util.ArrayList<EsKompetanse?> = java.util.ArrayList<EsKompetanse?>()
        kompetanseList.add(kompetanse1)
        kompetanseList.add(kompetanse2)
        kompetanseList.add(kompetanse3)
        kompetanseList.add(kompetanse4)

        val sertifikat1: EsSertifikat = EsSertifikat(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val sertifikat2: EsSertifikat = EsSertifikat(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val sertifikat3: EsSertifikat = EsSertifikat(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 12 tonn", ""
        )
        val sertifikat4: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )
        val sertifikat5: EsSertifikat = EsSertifikat(
            fraIsoDato("1995-01-01"), null, "A1.6820",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn",
            "Yrkesbevis anleggsmaskinførere: Arb.klar maskin over 6 tonn", ""
        )

        val sertifikatListe: java.util.ArrayList<EsSertifikat?> = java.util.ArrayList<EsSertifikat?>()
        sertifikatListe.add(sertifikat1)
        sertifikatListe.add(sertifikat2)
        sertifikatListe.add(sertifikat3)
        sertifikatListe.add(sertifikat4)
        sertifikatListe.add(sertifikat5)

        val forerkort1: EsForerkort = EsForerkort(
            fraIsoDato("1994-08-01"), null, "V1.6050",
            "A - Tung motorsykkel", null, ""
        )

        val forerkort2: EsForerkort = EsForerkort(
            fraIsoDato("1991-01-01"), null, "V1.6070",
            "BE - Personbil med tilhenger", null, ""
        )

        val forerkort3: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6110",
            "CE - Lastebil med tilhenger", null, ""
        )

        val forerkort4: EsForerkort = EsForerkort(
            fraIsoDato("1996-02-01"), fraIsoDato("2020-12-01"), "V1.6145",
            "DE - Buss med tilhenger", null, ""
        )

        val forerkortListe: java.util.ArrayList<EsForerkort?> = java.util.ArrayList<EsForerkort?>()
        forerkortListe.add(forerkort1)
        forerkortListe.add(forerkort2)
        forerkortListe.add(forerkort3)
        forerkortListe.add(forerkort4)


        val sprak1: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(skriftlig)", "Norwegian", "Morsmål")

        val sprak2: EsSprak =
            EsSprak(fraIsoDato("2012-12-01"), "78874", "Norsk(muntlig)", "Norwegian", "Morsmål")

        val sprakListe: java.util.ArrayList<EsSprak?> = java.util.ArrayList<EsSprak?>()
        sprakListe.add(sprak1)
        sprakListe.add(sprak2)

        val kurs1: EsKurs = esKurs1()

        val kurs2: EsKurs = esKurs2()

        val kurs3: EsKurs = esKurs3()


        val kursListe: java.util.ArrayList<EsKurs?> = java.util.ArrayList<EsKurs?>()
        kursListe.add(kurs1)
        kursListe.add(kurs2)
        kursListe.add(kurs3)

        val verv: EsVerv =
            EsVerv(
                fraIsoDato("2000-01-15"),
                fraIsoDato("2001-01-15"),
                "Verv organisasjon",
                "verv tittel"
            )

        val vervListe: java.util.ArrayList<EsVerv?> = java.util.ArrayList<EsVerv?>()
        vervListe.add(verv)

        val geografiJobbonsker: EsGeografiJobbonsker = EsGeografiJobbonsker("Hamar", "NO04.0403")

        val geografiJobbonsker1: EsGeografiJobbonsker =
            EsGeografiJobbonsker("Lillehammer", "NO05.0501")

        val geografiJobbonsker2: EsGeografiJobbonsker = EsGeografiJobbonsker("Hedmark", "NO04")

        val geografiJobbonskerListe: java.util.ArrayList<EsGeografiJobbonsker?> =
            java.util.ArrayList<EsGeografiJobbonsker?>()
        geografiJobbonskerListe.add(geografiJobbonsker)
        geografiJobbonskerListe.add(geografiJobbonsker1)
        geografiJobbonskerListe.add(geografiJobbonsker2)

        val yrkeJobbonsker: EsYrkeJobbonsker = EsYrkeJobbonsker(
            "Yrke jobb ønskeStyrk Kode",
            "Yrke jobb ønske Styrk beskrivelse", true, kotlin.collections.mutableListOf<T?>()
        )

        val yrkeJobbonskerListe: java.util.ArrayList<EsYrkeJobbonsker?> = java.util.ArrayList<EsYrkeJobbonsker?>()
        yrkeJobbonskerListe.add(yrkeJobbonsker)

        val esCv: EsCv = EsCv(
            nteAktorId(11),
            "01016034215",
            "OLA",
            "NORDMANN",
            "1960-01-01",
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
            null
        )
        esCv.addUtdanning(utdanningsListe)
        esCv.addYrkeserfaring(yrkeserfaringsListe)
        esCv.addKompetanse(kompetanseList)
        esCv.addSertifikat(sertifikatListe)
        esCv.addForerkort(forerkortListe)
        esCv.addSprak(sprakListe)
        esCv.addKurs(kursListe)
        esCv.addVerv(vervListe)
        esCv.addGeografiJobbonske(geografiJobbonskerListe)
        esCv.addYrkeJobbonske(yrkeJobbonskerListe)
        return esCv
    }

    fun giveMeYrkeserfaring(): EsYrkeserfaring {
        return EsYrkeserfaring(
            fraIsoDato("2016-06-01"),
            fraIsoDato("2016-07-01"),
            "MTM anlegg",
            "8332.03",
            "Lastebil- og trailersjåfør",
            Set.< E > of < E ? > ("Lastebil- og trailersjåfør"),
            "Sjåfør kl. 2",
            "YRKE_ORGNR",
            "YRKE_NACEKODE",
            false,
            kotlin.collections.mutableListOf<T?>(),
            "Oslo"
        )
    }

    fun giveMeUtdanning(): EsUtdanning {
        return EsUtdanning(
            fraIsoDato("1988-08-20"),
            fraIsoDato("1989-06-20"),
            "UiO",
            "838838",
            "Sosialantropologiske fag",
            "Sosialantropologi gr. fag"
        )
    }

    fun giveMeFørerkort(): EsForerkort {
        return EsForerkort(fraIsoDato("1994-08-01"), null, "V1.6050", "A - Tung motorsykkel", null, "")
    }

    fun giveMeKurs(): EsKurs {
        return esKurs1()
    }

    fun giveMeFagdokumentasjon(): EsFagdokumentasjon {
        return EsFagdokumentasjon("anyType", "anyTittel", "anyBeskrivelse")
    }

    fun giveMeAnnenErfaring(): EsAnnenErfaring {
        return EsAnnenErfaring(
            fraIsoDato("2005-01-01"),
            fraIsoDato("2010-12-31"),
            "anyBeskrivelse"
        )
    }

    fun giveMeGodkjenning(): EsGodkjenning {
        return EsGodkjenning(
            "anyTittel",
            "anyUtsteder",
            fraIsoDato("2020-06-01"),
            fraIsoDato("2050-01-01"),
            "anyKonseptId"
        )
    }
}
