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
    hullICv: String? = null
): String = """
        {
          "@event_name": "cv.sammenstilt",
          "aktørId": "123",
          "oppfølgingsinformasjon": {
                "oppfolgingsenhet":"1234"
          },
          "cv": {
            "meldingstype": "SLETT",
            "oppfolgingsinformasjon": {
                "oppfolgingsenhet":"1234"
            },
            "opprettCv": null,
            "endreCv": {
              "cv": {
                "synligForVeileder": true
              }
            },
            "slettCv": null,
            "opprettJobbprofil": null,
            "endreJobbprofil": null,
            "slettJobbprofil": null,
            "aktoerId": "123",
            "sistEndret": 1637238150.172
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