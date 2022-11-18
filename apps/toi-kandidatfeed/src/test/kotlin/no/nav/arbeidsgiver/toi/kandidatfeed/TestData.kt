package no.nav.arbeidsgiver.toi.kandidatfeed

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun synlighet(erSynlig: Boolean = true, ferdigBeregnet: Boolean = true) = """
        "synlighet": {
            "erSynlig": $erSynlig,
            "ferdigBeregnet": $ferdigBeregnet
        },
    """.trimIndent()

fun rapidMelding(
    synlighetJson: String?,
    behovsListe: List<String>? = null,
    organisasjonsenhetsnavn: String? = null,
    hullICv: String? = null,
    ontologi: String? = null
): String = """
        {
          "@event_name": "cv.sammenstilt",
          "aktørId": "123",
          "oppfølgingsinformasjon": {
                "oppfolgingsenhet":"1234"
          },
          "arbeidsmarkedCv": {
            "meldingstype": "ENDRE",
            "endretAv": "SYSTEMBRUKER",
            "opprettCv": null,
            "endreCv": {
              "cv": {
                "opprettet": 1578319832.314,
                "fodselsnummer": "10108000398",
                "foedselsdato": [
                  1980,
                  10,
                  10
                ],
                "fornavn": "Aremark M.",
                "etternavn": "Testfamilien",
                "epost": "julenissen@nordpolen.no",
                "telefon": "12345678",
                "gateadresse": "Rådhuset 5",
                "postnummer": "1798",
                "poststed": "AREMARK",
                "kommunenr": "3012",
                "sammendrag": "Jeg er en dyktig hjernekirurg som kan skrive sykt gode journaler i word.",
                "foererkort": {
                  "klasse": [
                    {
                      "klasse": "A1",
                      "utloeper": null,
                      "klasseBeskrivelse": "Lett motorsykkel",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "A2",
                      "utloeper": null,
                      "klasseBeskrivelse": "Mellomtung motorsykkel",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "S",
                      "utloeper": null,
                      "klasseBeskrivelse": "Snøscooter",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "T",
                      "utloeper": null,
                      "klasseBeskrivelse": "Traktor",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "BE",
                      "utloeper": null,
                      "klasseBeskrivelse": "Personbil med tilhenger",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "B",
                      "utloeper": null,
                      "klasseBeskrivelse": "Personbil",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "C1",
                      "utloeper": null,
                      "klasseBeskrivelse": "Lett lastebil",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "AM",
                      "utloeper": null,
                      "klasseBeskrivelse": "Moped",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "A",
                      "utloeper": null,
                      "klasseBeskrivelse": "Tung motorsykkel",
                      "fraTidspunkt": null
                    },
                    {
                      "klasse": "D",
                      "utloeper": [
                        2019,
                        8,
                        14
                      ],
                      "klasseBeskrivelse": "Buss",
                      "fraTidspunkt": [
                        2014,
                        8,
                        14
                      ]
                    },
                    {
                      "klasse": "D1E",
                      "utloeper": [
                        3000,
                        12,
                        13
                      ],
                      "klasseBeskrivelse": "Minibuss med tilhenger",
                      "fraTidspunkt": [
                        1989,
                        12,
                        13
                      ]
                    },
                    {
                      "klasse": "C1E",
                      "utloeper": [
                        2020,
                        12,
                        12
                      ],
                      "klasseBeskrivelse": "Lett lastebil med tilhenger",
                      "fraTidspunkt": [
                        1989,
                        12,
                        13
                      ]
                    },
                    {
                      "klasse": "D1",
                      "utloeper": [
                        2020,
                        12,
                        13
                      ],
                      "klasseBeskrivelse": "Minibuss",
                      "fraTidspunkt": [
                        1989,
                        12,
                        13
                      ]
                    },
                    {
                      "klasse": "C1E",
                      "utloeper": [
                        2020,
                        12,
                        13
                      ],
                      "klasseBeskrivelse": "Lett lastebil med tilhenger",
                      "fraTidspunkt": [
                        1989,
                        12,
                        13
                      ]
                    }
                  ]
                },
                "arbeidserfaring": [
                  {
                    "stillingstittel": "Lege",
                    "styrkkode": "2211",
                    "arbeidsgiver": "Sykehuset Mjøsa",
                    "sted": "Mjøsa",
                    "beskrivelse": "Operere og reparere",
                    "fraTidspunkt": "2007-08",
                    "tilTidspunkt": null,
                    "stillingstittelFritekst": "",
                    "janzzKonseptid": "21468",
                    "ikkeAktueltForFremtiden": false
                  },
                  {
                    "stillingstittel": "Pianolærer",
                    "styrkkode": "2354",
                    "arbeidsgiver": "Piano-skolen",
                    "sted": "",
                    "beskrivelse": "",
                    "fraTidspunkt": "2004-09",
                    "tilTidspunkt": "2006-09",
                    "stillingstittelFritekst": "",
                    "janzzKonseptid": "29031",
                    "ikkeAktueltForFremtiden": false
                  },
                  {
                    "stillingstittel": "Baker",
                    "styrkkode": "7512",
                    "arbeidsgiver": "Bollebakeren",
                    "sted": "Espa",
                    "beskrivelse": "Bakte boller",
                    "fraTidspunkt": "2004-09",
                    "tilTidspunkt": "2006-07",
                    "stillingstittelFritekst": "",
                    "janzzKonseptid": "19765",
                    "ikkeAktueltForFremtiden": false
                  }
                ],
                "utdannelse": [
                  {
                    "laerested": "Fagskolen Øst",
                    "beskrivelse": "",
                    "utdanningsretning": "Tømrermester",
                    "autorisasjon": false,
                    "fraTidspunkt": "2021-10",
                    "tilTidspunkt": null,
                    "nuskodeGrad": "5",
                    "utdannelseYrkestatus": null
                  },
                  {
                    "laerested": "Drammen",
                    "beskrivelse": "",
                    "utdanningsretning": "Ekspert",
                    "autorisasjon": false,
                    "fraTidspunkt": "2020-04",
                    "tilTidspunkt": null,
                    "nuskodeGrad": "7",
                    "utdannelseYrkestatus": "INGEN"
                  },
                  {
                    "laerested": "test2",
                    "beskrivelse": "asdlkjfa",
                    "utdanningsretning": "",
                    "autorisasjon": false,
                    "fraTidspunkt": "1990-09",
                    "tilTidspunkt": null,
                    "nuskodeGrad": "4",
                    "utdannelseYrkestatus": "INGEN"
                  }
                ],
                "fagdokumentasjon": [
                  {
                    "type": "SVENNEBREV_FAGBREV",
                    "tittel": "Fagbrev industriell matproduksjon",
                    "beskrivelse": null
                  },
                  {
                    "type": "MESTERBREV",
                    "tittel": "Mesterbrev baker",
                    "beskrivelse": null
                  },
                  {
                    "type": "SVENNEBREV_FAGBREV",
                    "tittel": "Svennebrev baker",
                    "beskrivelse": null
                  }
                ],
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
                "kurs": [
                  {
                    "tittel": "Førstehjelpskurs",
                    "utsteder": "Norsk luftambulanse",
                    "tidspunkt": [
                      2013,
                      2,
                      18
                    ],
                    "varighet": 5,
                    "varighetEnhet": "TIME"
                  },
                  {
                    "tittel": "Word-kurs",
                    "utsteder": "Itkursagenten",
                    "tidspunkt": [
                      2012,
                      5,
                      1
                    ],
                    "varighet": 4,
                    "varighetEnhet": "UKE"
                  }
                ],
                "sertifikat": [
                  {
                    "tittel": "Bollebaker-sertifikat",
                    "sertifikatnavn": null,
                    "utsteder": "Espa bolle-service",
                    "gjennomfoert": [
                      2002,
                      2,
                      1
                    ],
                    "utloeper": [
                      2080,
                      6,
                      1
                    ],
                    "konseptId": null,
                    "sertifikatnavnFritekst": "Bollebaker-sertifikat"
                  }
                ],
                "spraakferdigheter": [
                  {
                    "spraaknavn": "Norsk",
                    "iso3kode": "nor",
                    "muntlig": "FOERSTESPRAAK",
                    "skriftlig": "FOERSTESPRAAK"
                  }
                ],
                "annenErfaring": [
                  {
                    "beskrivelse": "",
                    "rolle": "Fotballtrener",
                    "fraTidspunkt": null,
                    "tilTidspunkt": null
                  }
                ],
                "oppstartKode": null,
                "synligForArbeidsgiver": false,
                "synligForVeileder": false,
                "arenaKandidatnr": "CG133309",
                "aktoerId": "1000096233942",
                "sistEndret": 1652184804.44,
                "cvId": "76a38a8c-16df-41dd-965b-e8b4ef404251"
              }
            },
            "slettCv": null,
            "opprettJobbprofil": null,
            "endreJobbprofil": {
              "jobbprofil": {
                "opprettet": 1615290716.205,
                "stillinger": [
                  "Sjåfør",
                  "Sirkustekniker",
                  "Produktsjef kjøretøy"
                ],
                "kompetanser": [
                  "Sirkusestetikk",
                  "Sirkusvokabular",
                  "Definere riggebehov for sirkuskunster",
                  "Kontrollere sirkusrigging før fremføring",
                  "Servicearbeid"
                ],
                "stillingkladder": [],
                "geografi": [
                  {
                    "sted": "Oslo",
                    "kode": "NO03"
                  },
                  {
                    "sted": "Vestnes",
                    "kode": "NO15.1535"
                  }
                ],
                "ansettelsesformer": [
                  "FAST"
                ],
                "arbeidstider": [],
                "arbeidsdager": [],
                "arbeidstidsordninger": [
                  "SKIFT"
                ],
                "omfang": [
                  "HELTID"
                ],
                "oppstartKode": "ETTER_TRE_MND",
                "aktoerId": "1000096233942",
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
             "veilederId":"A123123",
             "tilordnet":"2021-11-19T13:18:03.307756228"
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
              "service": "toi-cv",
              "instance": "toi-cv-58849d5f86-7qffs",
              "time": "2021-11-19T10:53:59.163725026"
            },
            {
              "service": "toi-sammenstille-kandidat",
              "instance": "toi-sammenstille-kandidat-85b9d49b9c-fctpx",
              "time": "2021-11-19T13:18:03.307756227"
            }
          ]
        }
    """.trimIndent()