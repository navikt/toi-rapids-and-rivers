package no.nav.arbeidsgiver.toi.kandidat.indekser

import java.time.LocalDate

fun synlighet(erSynlig: Boolean = true, ferdigBeregnet: Boolean = true) = """
        "synlighet": {
            "erSynlig": $erSynlig,
            "ferdigBeregnet": $ferdigBeregnet
        },
    """.trimIndent()

data class TestUtdanning(
    val laerested: String,
    val beskrivelse: String,
    val utdanningsretning: String,
    val autorisasjon: Boolean,
    val fraTidspunkt: String,
    val tilTidspunkt: String?,
    val nuskodeGrad: String,
    val utdannelseYrkestatus: String?
)
data class TestFagdokumentasjon(
    val type: String,
    val tittel: String,
    val beskrivelse: String?
)
data class TestYrkeserfaring(
    val stillingstittel: String,
    val styrkkode: String,
    val arbeidsgiver: String,
    val sted: String,
    val beskrivelse: String,
    val fraTidspunkt: String,
    val tilTidspunkt: String?,
    val stillingstittelFritekst: String,
    val janzzKonseptid: String,
    val ikkeAktueltForFremtiden: Boolean
)
data class TestKompetanse(
    val navn: String,
    val nivaa: String
)
data class TestAnnenErfaring(
    val beskrivelse: String,
    val rolle: String,
    val fraTidspunkt: String?,
    val tilTidspunkt: String?
)
data class TestSertifikat(
    val tittel: String,
    val sertifikatnavn: String?,
    val utsteder: String,
    val gjennomfoert: LocalDate,
    val utloeper: LocalDate?,
    val konseptId: String?,
    val sertifikatnavnFritekst: String?
)
data class TestForerkort(
    val klasse: String,
    val utloeper: LocalDate?,
    val klasseBeskrivelse: String,
    val fraTidspunkt: LocalDate?
)
data class TestKurs(
    val tittel: String,
    val utsteder: String,
    val tidspunkt: LocalDate,
    val varighet: Int,
    val varighetEnhet: String
)
data class TestGeografiJobbonsker(
    val sted: String,
    val kode: String
)
fun ontologiDel(
    kompetansenavn: Map<String, Pair<List<String>, List<String>>> = emptyMap(),
    stillingstittel: Map<String, Pair<List<String>, List<String>>> = emptyMap()
) = """
    {
        "ontologi": {
            "kompetansenavn": {
                ${kompetansenavn.entries.joinToString { (k,v) -> v.let { (synonymer, merGenerell) ->
                   """
                    "$k": {
                        "synonymer": [
                            ${synonymer.joinToString { "\"${it.trim()}\"" }}
                        ],
                        "merGenerell": [
                            ${merGenerell.joinToString { "\"${it.trim()}\"" }}
                        ]
                    }
                   """.trimIndent()
                }}
                }
            },
            "stillingstittel": {
                ${stillingstittel.entries.joinToString { (k,v) -> v.let { (synonymer, merGenerell) ->
                   """
                    "$k": {
                        "synonymer": [
                            ${synonymer.joinToString { "\"${it.trim()}\"" }}
                        ],
                        "merGenerell": [
                            ${merGenerell.joinToString { "\"${it.trim()}\"" }}
                        ]
                    }
                   """.trimIndent()
                }}
            }
        }
    }
""".trimIndent()
fun rapidMelding(
    synlighetJson: String?,
    behovsListe: List<String>? = null,
    organisasjonsenhetsnavn: String? = null,
    hullICv: String? = null,
    ontologi: String? = null,
    sluttAvHendelseskjede: Boolean? = null,
    kandidatnr: String = "CG133309",
    aktørId: String = "123",
    oppfølgingsenhet: String = "1234",
    formidlingsgruppe: String = "ARBS",
    kvalifiseringsgruppe: String = "IVURD",
    oppfølgingsinformasjonHovedmaal: String = "SKAFFEA",
    orgenhet: String = "1234",
    siste14AvedtakHovedmaal: String = "SKAFFEB",
    siste14AInnsatsgruppe: String = "BATT",
    fritattKandidatsok: Boolean = false,
    meldingstype: String = "ENDRE",
    endretAv: String = "SYSTEMBRUKER",
    opprettetCv: String = "1578319832.314",
    fødselsnummer: String = "10108000398",
    fødselsdato: LocalDate = LocalDate.of(1980, 10, 10),
    fornavn: String = "Aremark M.",
    etternavn: String = "Testfamilien",
    epostadresse: String = "julenissen@nordpolen.no",
    telefonnummer: String = "12345678",
    gateadresse: String = "Rådhuset 5",
    postnummer: String = "1798",
    poststed: String = "AREMARK",
    kommunenr: String = "3012",
    sistendret: String = "1652184804.44",
    sammendrag: String = "Jeg er en dyktig hjernekirurg som kan skrive sykt gode journaler i word.",
    synligForArbeidsgiver: Boolean = true,
    synligForVeileder: Boolean = true,
    aktoerId: String = "1000096233942",
    oppstartKode: String = "SNART",
    veilederNavIdent: String = "Z123456",
    veilederVisningsnavn: String = "Per Veiviser",
    veilederFornavn: String = "Per",
    veilederEtternavn: String = "Veiviser",
    veilederEpost: String = "veileder@nav.no",
    utdanning: List<TestUtdanning> = listOf(
        TestUtdanning(
            laerested = "Fagskolen Øst",
            beskrivelse = "",
            utdanningsretning = "Tømrermester",
            autorisasjon = false,
            fraTidspunkt = "2021-10",
            tilTidspunkt = null,
            nuskodeGrad = "5",
            utdannelseYrkestatus = null
        ),
        TestUtdanning(
            laerested = "Drammen",
            beskrivelse = "",
            utdanningsretning = "Ekspert",
            autorisasjon = false,
            fraTidspunkt = "2020-04",
            tilTidspunkt = null,
            nuskodeGrad = "7",
            utdannelseYrkestatus = "INGEN"
        ),
        TestUtdanning(
            laerested = "test2",
            beskrivelse = "asdlkjfa",
            utdanningsretning = "",
            autorisasjon = false,
            fraTidspunkt = "1990-09",
            tilTidspunkt = null,
            nuskodeGrad = "4",
            utdannelseYrkestatus = "INGEN"
        )
    ),
    fagdokumentasjon: List<TestFagdokumentasjon> = listOf(
        TestFagdokumentasjon(
            type = "SVENNEBREV_FAGBREV",
            tittel = "Fagbrev industriell matproduksjon",
            beskrivelse = null
        ),
        TestFagdokumentasjon(
            type = "MESTERBREV",
            tittel = "Mesterbrev baker",
            beskrivelse = null
        ),
        TestFagdokumentasjon(
            type = "SVENNEBREV_FAGBREV",
            tittel = "Svennebrev baker",
            beskrivelse = null
        )
    ),
    yrkeserfaring: List<TestYrkeserfaring> = listOf(
        TestYrkeserfaring(
            stillingstittel = "Lege",
            styrkkode = "2211",
            arbeidsgiver = "Sykehuset Mjøsa",
            sted = "Mjøsa",
            beskrivelse = "Operere og reparere",
            fraTidspunkt = "2007-08",
            tilTidspunkt = null,
            stillingstittelFritekst = "",
            janzzKonseptid = "21468",
            ikkeAktueltForFremtiden = false
        ),
        TestYrkeserfaring(
            stillingstittel = "Pianolærer",
            styrkkode = "2354",
            arbeidsgiver = "Piano-skolen",
            sted = "",
            beskrivelse = "",
            fraTidspunkt = "2004-09",
            tilTidspunkt = "2006-09",
            stillingstittelFritekst = "",
            janzzKonseptid = "29031",
            ikkeAktueltForFremtiden = false
        ),
        TestYrkeserfaring(
            stillingstittel = "Baker",
            styrkkode = "7512",
            arbeidsgiver = "Bollebakeren",
            sted = "Espa",
            beskrivelse = "Bakte boller",
            fraTidspunkt = "2004-09",
            tilTidspunkt = "2006-07",
            stillingstittelFritekst = "",
            janzzKonseptid = "19765",
            ikkeAktueltForFremtiden = false
        )
    ),
    kompetanse: List<String> = listOf(
        "Sirkusestetikk",
        "Sirkusvokabular",
        "Definere riggebehov for sirkuskunster",
        "Kontrollere sirkusrigging før fremføring",
        "Servicearbeid"
    ),
    annenErfaring: List<TestAnnenErfaring> = listOf(
        TestAnnenErfaring(
            beskrivelse = "Trente barn og unge i fotball",
            rolle = "Fotballtrener",
            fraTidspunkt = "2010-03",
            tilTidspunkt = "2012-11"
        )
    ),
    sertifikat: List<TestSertifikat> = listOf(
        TestSertifikat(
            tittel = "Bollebaker-sertifikat",
            sertifikatnavn = null,
            utsteder = "Espa bolle-service",
            gjennomfoert = LocalDate.of(2002, 2, 1),
            utloeper = LocalDate.of(2080, 6, 1),
            konseptId = null,
            sertifikatnavnFritekst = "Bollebaker-sertifikat"
        )
    ),
    forerkort: List<TestForerkort> = listOf(
        TestForerkort(
            klasse = "A1",
            utloeper = null,
            klasseBeskrivelse = "Lett motorsykkel",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "A2",
            utloeper = null,
            klasseBeskrivelse = "Mellomtung motorsykkel",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "S",
            utloeper = null,
            klasseBeskrivelse = "Snøscooter",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "T",
            utloeper = null,
            klasseBeskrivelse = "Traktor",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "BE",
            utloeper = null,
            klasseBeskrivelse = "Personbil med tilhenger",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "B",
            utloeper = null,
            klasseBeskrivelse = "Personbil",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "C1",
            utloeper = null,
            klasseBeskrivelse = "Lett lastebil",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "AM",
            utloeper = null,
            klasseBeskrivelse = "Moped",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "A",
            utloeper = null,
            klasseBeskrivelse = "Tung motorsykkel",
            fraTidspunkt = null
        ),
        TestForerkort(
            klasse = "D",
            utloeper = LocalDate.of(2019, 8, 14),
            klasseBeskrivelse = "Buss",
            fraTidspunkt = LocalDate.of(2014, 8, 14)
        ),
        TestForerkort(
            klasse = "D1E",
            utloeper = LocalDate.of(3000, 12, 13),
            klasseBeskrivelse = "Minibuss med tilhenger",
            fraTidspunkt = LocalDate.of(1989, 12, 13)
        ),
        TestForerkort(
            klasse = "C1E",
            utloeper = LocalDate.of(2020, 12, 12),
            klasseBeskrivelse = "Lett lastebil med tilhenger",
            fraTidspunkt = LocalDate.of(1989, 12, 13)
        ),
        TestForerkort(
            klasse = "D1",
            utloeper = LocalDate.of(2020, 12, 13),
            klasseBeskrivelse = "Minibuss",
            fraTidspunkt = LocalDate.of(1989, 12, 13)
        )
    ),
    kurs: List<TestKurs> = listOf(
        TestKurs(
            tittel = "Førstehjelpskurs",
            utsteder = "Norsk luftambulanse",
            tidspunkt = LocalDate.of(2013, 2, 18),
            varighet = 5,
            varighetEnhet = "TIME"
        ),
        TestKurs(
            tittel = "Word-kurs",
            utsteder = "Itkursagenten",
            tidspunkt = LocalDate.of(2012, 5, 1),
            varighet = 4,
            varighetEnhet = "UKE"
        )
    ),
    geografiJobbonsker: List<TestGeografiJobbonsker> = listOf(
        TestGeografiJobbonsker(
            sted = "Oslo",
            kode = "NO03"
        ),
        TestGeografiJobbonsker(
            sted = "Vestnes",
            kode = "NO15.1535"
        )
    ),
    jobbProfilstillinger: List<String> = listOf(
        "Sjåfør",
        "Sirkustekniker",
        "Produktsjef kjøretøy"
    ),
    omfangJobbonsker: List<String> = listOf("HELTID"),
    ansettelsesformJobbonsker: List<String> = listOf("FAST"),
    arbeidstidsordningJobbonsker: List<String> = listOf("SKIFT"),
    arbeidsdagerJobbonsker: List<String> = listOf(),
    arbeidstidJobbonsker: List<String> = listOf(),
): String = """
        {
          "@event_name": "arbeidsmarked-cv",
          "aktørId": "$aktørId",
          "oppfølgingsinformasjon": {
                "oppfolgingsenhet":"$oppfølgingsenhet",
                "formidlingsgruppe": "$formidlingsgruppe",
                "kvalifiseringsgruppe": "$kvalifiseringsgruppe",
                "hovedmaal": "$oppfølgingsinformasjonHovedmaal",
                "orgenhet": "$orgenhet"
          },
          "siste14avedtak": {
            "hovedmal": "$siste14AvedtakHovedmaal",
            "innsatsgruppe": "$siste14AInnsatsgruppe"
          },
          "fritattKandidatsøk": {
            "fritattKandidatsok": $fritattKandidatsok
          },
          "arbeidsmarkedCv": {
            "meldingstype": "$meldingstype",
            "endretAv": "$endretAv",
            "opprettCv": null,
            "endreCv": {
              "cv": {
                "opprettet": $opprettetCv,
                "fodselsnummer": "$fødselsnummer",
                "foedselsdato": [
                  ${fødselsdato.year},
                  ${fødselsdato.monthValue},
                    ${fødselsdato.dayOfMonth}
                ],
                "fornavn": "$fornavn",
                "etternavn": "$etternavn",
                "epost": "$epostadresse",
                "telefon": "$telefonnummer",
                "gateadresse": "$gateadresse",
                "postnummer": "$postnummer",
                "poststed": "$poststed",
                "kommunenr": "$kommunenr",
                "sammendrag": "$sammendrag",
                "foererkort": {
                  "klasse": ${forerkort.joinToString(prefix = "[", postfix = "]") {
                        """
                        {
                            "klasse": "${it.klasse}",
                            "utloeper": ${it.utloeper?.let { "[${it.year}, ${it.monthValue}, ${it.dayOfMonth}]" }},
                            "klasseBeskrivelse": "${it.klasseBeskrivelse}",
                            "fraTidspunkt": ${it.fraTidspunkt?.let { "[${it.year}, ${it.monthValue}, ${it.dayOfMonth}]" }}
                        }
                        """.trimIndent()
                    }
                  }
                },
                "arbeidserfaring": ${yrkeserfaring.joinToString(prefix = "[", postfix = "]") {
                        """
                        {
                            "stillingstittel": "${it.stillingstittel}",
                            "styrkkode": "${it.styrkkode}",
                            "arbeidsgiver": "${it.arbeidsgiver}",
                            "sted": "${it.sted}",
                            "beskrivelse": "${it.beskrivelse}",
                            "fraTidspunkt": "${it.fraTidspunkt}",
                            "tilTidspunkt": ${it.tilTidspunkt?.let { "\"$it\"" }},
                            "stillingstittelFritekst": "${it.stillingstittelFritekst}",
                            "janzzKonseptid": "${it.janzzKonseptid}",
                            "ikkeAktueltForFremtiden": ${it.ikkeAktueltForFremtiden}
                        }
                        """.trimIndent()
                    }},
                "utdannelse": ${utdanning.joinToString(prefix = "[", postfix = "]") { 
                        """
                        {
                            "laerested": "${it.laerested}",
                            "beskrivelse": "${it.beskrivelse}",
                            "utdanningsretning": "${it.utdanningsretning}",
                            "autorisasjon": ${it.autorisasjon},
                            "fraTidspunkt": "${it.fraTidspunkt}",
                            "tilTidspunkt": ${it.tilTidspunkt?.let { "\"$it\"" }},
                            "nuskodeGrad": "${it.nuskodeGrad}",
                            "utdannelseYrkestatus": ${it.utdannelseYrkestatus?.let { "\"$it\"" }}
                        }
                        """.trimIndent()
                    }},
                "fagdokumentasjon": ${fagdokumentasjon.joinToString(prefix = "[", postfix = "]") { 
                        """
                        {
                            "type": "${it.type}",
                            "tittel": "${it.tittel}",
                            "beskrivelse": ${it.beskrivelse?.let { "\"$it\"" }}
                        }
                        """.trimIndent()
                    }},
                "godkjenninger": [
                  {
                    "tittel": "Taxiløyve",
                    "utsteder": "Viken fylkeskommune",
                    "gjennomfoert": [
                      2019,
                      12,
                      18
                    ],
                    "utloeper": null,
                    "konseptId": "1903811"
                  },
                  {
                    "tittel": "Autorisasjon som lege",
                    "utsteder": "Legeautorisasjonstjenesten",
                    "gjennomfoert": [
                      2005,
                      4,
                      1
                    ],
                    "utloeper": [
                      2060,
                      6,
                      1
                    ],
                    "konseptId": "381393"
                  }
                ],
                "kurs": ${kurs.joinToString(prefix = "[", postfix = "]") {
                        """
                        {
                            "tittel": "${it.tittel}",
                            "utsteder": "${it.utsteder}",
                            "tidspunkt": [
                                ${it.tidspunkt.year},
                                ${it.tidspunkt.monthValue},
                                ${it.tidspunkt.dayOfMonth}
                            ],
                            "varighet": ${it.varighet},
                            "varighetEnhet": "${it.varighetEnhet}"
                        }
                        """.trimIndent()
                }},
                "sertifikat": ${sertifikat.joinToString(prefix = "[", postfix = "]") {
                        """
                        {
                            "tittel": "${it.tittel}",
                            "sertifikatnavn": ${it.sertifikatnavn?.let { "\"$it\"" }},
                            "sertifikatnavnFritekst": ${it.sertifikatnavnFritekst?.let { "\"$it\"" }},
                            "konseptId": ${it.konseptId?.let { "\"$it\"" }},
                            "utsteder": "${it.utsteder}",
                            "gjennomfoert": [
                                ${it.gjennomfoert.year},
                                ${it.gjennomfoert.monthValue},
                                ${it.gjennomfoert.dayOfMonth}
                            ],
                            "utloeper": ${it.utloeper?.let { """[${it.year},${it.monthValue},${it.dayOfMonth}]""" }}
                        }
                        """.trimIndent()
                    }
                },
                "spraakferdigheter": [
                  {
                    "spraaknavn": "Norsk",
                    "iso3kode": "nor",
                    "muntlig": "FOERSTESPRAAK",
                    "skriftlig": "FOERSTESPRAAK"
                  }
                ],
                "annenErfaring": ${annenErfaring.joinToString(prefix = "[", postfix = "]") { 
                        """
                        {
                            "beskrivelse": "${it.beskrivelse}",
                            "rolle": "${it.rolle}",
                            "fraTidspunkt": ${it.fraTidspunkt?.let { "\"$it\"" }},
                            "tilTidspunkt": ${it.tilTidspunkt?.let { "\"$it\"" }}
                        }
                        """.trimIndent()
                    }},
                "oppstartKode": null,
                "synligForArbeidsgiver": $synligForArbeidsgiver,
                "synligForVeileder": $synligForVeileder,
                "arenaKandidatnr": "$kandidatnr",
                "aktoerId": "$aktoerId",
                "sistEndret": $sistendret,
                "cvId": "76a38a8c-16df-41dd-965b-e8b4ef404251"
              }
            },
            "slettCv": null,
            "opprettJobbprofil": null,
            "endreJobbprofil": {
              "jobbprofil": {
                "opprettet": 1615290716.205,
                "stillinger": ${jobbProfilstillinger.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "kompetanser": ${kompetanse.joinToString(prefix = "[", postfix = "]") { "\"${it}\"" }},
                "stillingkladder": [],
                "geografi": ${geografiJobbonsker.joinToString(prefix = "[", postfix = "]") {
                        """
                        {
                            "sted": "${it.sted}",
                            "kode": "${it.kode}"
                        }
                        """.trimIndent()
                    }
                },
                "ansettelsesformer": ${ansettelsesformJobbonsker.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "arbeidstider": ${arbeidstidJobbonsker.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "arbeidsdager": ${arbeidsdagerJobbonsker.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "arbeidstidsordninger": ${arbeidstidsordningJobbonsker.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "omfang": ${omfangJobbonsker.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
                "oppstartKode": "$oppstartKode",
                "aktoerId": "$aktoerId",
                "sistEndret": 1652184804.347,
                "jobbprofilId": "668794"
              }
            },
            "slettJobbprofil": null,
            "aktoerId": "1000096233942",
            "sistEndret": 1652184804.44
          },
          "veileder": {
             "aktorId":"123",
             "veilederId":"$veilederNavIdent",
             "tilordnet":"2021-11-19T13:18:03.307756228",
             "veilederinformasjon":{
                "navIdent":"$veilederNavIdent",
                "visningsNavn":"$veilederVisningsnavn",
                "fornavn":"$veilederFornavn",
                "etternavn":"$veilederEtternavn",
                "epost":"$veilederEpost"
             }
          },
          ${synlighetJson ?: ""}
          ${
    if (behovsListe == null) "" else """"@behov": ${
        behovsListe.joinToString(
            ",",
            "[",
            "]"
        ) { """"$it"""" }
    },"""
}
          ${organisasjonsenhetsnavn?.let { """"organisasjonsenhetsnavn": "$it",""" } ?: ""}
          ${hullICv?.let { """"hullICv": "$it",""" } ?: ""}
          ${ontologi?.let { """"ontologi": "$it",""" } ?: ""}
          "system_read_count": 1,
          "system_participating_services": [
            {
              "service": "toi-arbeidsmarked-cv",
              "instance": "toi-arbeidsmarked-cv-58849d5f86-7qffs",
              "time": "2021-11-19T10:53:59.163725026"
            },
            {
              "service": "toi-sammenstille-kandidat",
              "instance": "toi-sammenstille-kandidat-85b9d49b9c-fctpx",
              "time": "2021-11-19T13:18:03.307756227"
            }
          ]
          ${if(sluttAvHendelseskjede==null) "" else """, "@slutt_av_hendelseskjede": $sluttAvHendelseskjede"""}
        }
    """.trimIndent()