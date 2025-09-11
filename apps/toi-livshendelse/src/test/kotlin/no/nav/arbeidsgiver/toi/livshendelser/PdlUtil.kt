package no.nav.arbeidsgiver.toi.livshendelser

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock

fun WireMockServer.stub404(
    ident: String = "12312312312",
    token: String? = "mockedAccessToken"
) {
    stubMedSvar(token, ident,
        WireMock.aResponse()
            .withStatus(200)
            .withBody(
                """
                            {
                                "errors": [
                                    {
                                      "message": "Fant ikke person",
                                      "locations": [],
                                      "path": [],
                                      "extensions": {
                                        "code": "not_found",
                                        "details": null,
                                        "classification": "ExecutionAborted"
                                      }
                                    }
                                  ],
                                  "data": {}
                            }
                        """.trimIndent()
            ))
}

fun WireMockServer.stubPdl(
    ident: String = "12312312312",
    identSvar: String = """
        {
            "ident" : "$ident"
        }
    """.trimIndent(),
    gradering: String = "STRENGT_FORTROLIG",
    token: String? = "mockedAccessToken"
) {
    stubMedSvar(token, ident,
        WireMock.aResponse()
            .withStatus(200)
            .withBody(
                """
                            {
                                "data": {
                                    "hentPerson": {
                                            "adressebeskyttelse": [
                                                {
                                                    "gradering" : "$gradering"
                                                }
                                            ]
                                    },
                                    "hentIdenter": {
                                        "identer": [
                                            $identSvar
                                        ]
                                    }
                                }
                            }
                        """.trimIndent()
            ))
}

private fun WireMockServer.stubMedSvar(
    token: String?,
    ident: String,
    respons: ResponseDefinitionBuilder
) {
    val pesostegn = "$"
    stubFor(
        WireMock.post(WireMock.urlEqualTo("/graphql"))
            .apply { if (token != null) withHeader("Authorization", WireMock.equalTo("Bearer $token")) }
            .withRequestBody(
                WireMock.equalToJson(
                    """
                        {
                            "query": "query( ${pesostegn}ident: ID!) { hentPerson(ident: ${pesostegn}ident) { adressebeskyttelse(historikk: false) { gradering }} hentIdenter(ident: ${pesostegn}ident, grupper: [AKTORID], historikk: false) { identer { ident }} }",
                            "variables":{"ident":"$ident"}
                        }
                    """.trimIndent()
                )
            )
            .willReturn(respons)
    )
}