import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon.NotifikasjonKlient

const val TEST_ACCESS_TOKEN = "TestAccessToken"

fun WireMockServer.stubNySak() {
    val respons = """
        {
            "data": {
                "nySak": {
                    "__typename": "${NotifikasjonKlient.NySakSvar.NySakVellykket.name}",
                    "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
                }
            }
        }
    """.trimIndent()

    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(WireMock.ok(respons).withHeader("Content-Type", "application/json"))
    )
}

fun WireMockServer.stubDuplisertSak() {
    val respons = """
        {
            "data": {
                "nySak": {
                   "__typename": "${NotifikasjonKlient.NySakSvar.DuplikatGrupperingsid.name}",
                    "feilmelding": "notifikasjon med angitt eksternId og merkelapp finnes fra før"
                }
            }
        }
    """.trimIndent()

    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(WireMock.ok(respons).withHeader("Content-Type", "application/json"))
    )
}

fun WireMockServer.stubNyStatusSak() {
    val respons = """
        {
            "data": {
                "nyStatusSakByGrupperingsid": {
                    "__typename": "${NotifikasjonKlient.NyStatusSakSvar.NyStatusSakVellykket.name}",
                    "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
                }
            }
        }
    """.trimIndent()

    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(WireMock.ok(respons).withHeader("Content-Type", "application/json"))
    )
}

fun WireMockServer.stubSlettSak() {
    val respons = """
        {
            "data": {
                "hardDeleteSakByGrupperingsid": {
                    "__typename": "${NotifikasjonKlient.SlettSakSvar.HardDeleteSakVellykket.name}",
                    "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
                }
            }
        }
    """.trimIndent()

    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(WireMock.ok(respons).withHeader("Content-Type", "application/json"))
    )
}

fun WireMockServer.stubNyBeskjed() {
    val respons = """
        {
            "data": {
                "nyBeskjed": {
                    "__typename": "NyBeskjedVellykket",
                    "id": "79c444d8-c658-43f8-8bfe-fabe668c6dcb"
                }
            }
        }
    """.trimIndent()

    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(WireMock.ok(respons).withHeader("Content-Type", "application/json"))
    )
}

fun WireMockServer.stubDuplisertBeskjed() {
    val respons = """
        {
            "data": {
                "nyBeskjed": {
                    "__typename": "DuplikatEksternIdOgMerkelapp",
                    "feilmelding": "notifikasjon med angitt eksternId og merkelapp finnes fra før"
                }
            }
        }
    """.trimIndent()

    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(WireMock.ok(respons).withHeader("Content-Type", "application/json"))
    )
}

fun WireMockServer.stubUventetStatusIRespons(queryName: String) {
    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(
                WireMock.ok(
                    """
                        {
                          "data": {
                            "$queryName": {
                              "__typename": "UforventetStatus",
                              "feilmelding": "Dette er en artig feil"
                            }
                          }
                        }
                    """.trimIndent()
                ).withHeader("Content-Type", "application/json")
            )
    )
}

fun WireMockServer.stubErrorsIRespons() {
    val error = """
            {
                "error": {
                    "errors": [
                        {
                            "message": "Feilmelding"
                        }
                    ]
                }
            }
        """.trimIndent()

    this.stubFor(
        WireMock.post("/api/graphql")
            .withHeader("Authorization", WireMock.containing("Bearer $TEST_ACCESS_TOKEN"))
            .willReturn(WireMock.ok(error).withHeader("Content-Type", "application/json"))
    )
}