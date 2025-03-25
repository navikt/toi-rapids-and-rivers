package no.nav.arbeidsgiver.toi.livshendelser

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock


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
    val pesostegn = "$"
    stubFor(
        WireMock.post(WireMock.urlEqualTo("/graphql"))
            .apply { if(token != null) withHeader("Authorization", WireMock.equalTo("Bearer $token")) }
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
            .willReturn(
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
                    )
            )
    )
}