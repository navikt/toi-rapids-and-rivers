package no.nav.arbeidsgiver.toi.veileder

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VeilederTest {

    val wiremock = WireMockServer(8089).also { it.start() }
    private val url = "http://localhost:8089/graphql"
    private val accessToken = "TestAccessToken"

    @Test
    fun `Lesing av veilederMelding fra eksternt topic skal produsere ny melding på rapid`() {
        val aktørId = "10000100000"
        val veilederId = "A313111"
        val tilordnet = "2020-12-21T10:58:19.023+01:00"

        val nomKlient = NomKlient(url = url) { accessToken }

        val testRapid = TestRapid()
        wiremock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .withRequestBody(
                    WireMock.equalTo(
                        """
                        {
                          "query": "query(${'$'}identer: [String!]!) {
                            ressurser(where: { navidenter: ${'$'}identer }) {
                              id
                              ressurs {
                                navIdent
                                visningsNavn
                                fornavn
                                etternavn
                                epost
                              }
                            }
                          }",
                          "variables": {
                            "identer": ["$veilederId"]
                          }
                        }
                        """.trimIndent()
                    )
                )
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "data": {
                                "ressurser": [
                                  {
                                    "id": "A112123",
                                    "ressurs": {
                                      "navIdent": "$veilederId",
                                      "visningsNavn": "Jon Blund",
                                      "fornavn": "Jon",
                                      "etternavn": "Blund",
                                      "epost": "Jonblund@jonb.no"
                                    }
                                  }
                                ]
                              }
                            }
                            """.trimIndent()
                        )
                )
        )

        VeilederLytter(testRapid, nomKlient)

        testRapid.sendTestMessage(veilederMeldingFraEksterntTopic(aktørId, veilederId, tilordnet))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "@event_name",
            "veileder",
            "veilederinformasjon",
            "aktørId",
            "system_read_count",
            "@id",
            "@opprettet",
            "system_participating_services"
        )
        assertThat(meldingJson.get("@event_name").asText()).isEqualTo("veileder")
        assertThat(meldingJson.get("aktørId").asText()).isEqualTo(aktørId)

        val veilederJson = meldingJson.get("veileder")
        assertThat(veilederJson.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "aktorId",
            "veilederId",
            "tilordnet",
        )
        meldingJson.get("veileder").apply {
            assertThat(get("aktorId").asText()).isEqualTo(aktørId)
            assertThat(get("veilederId").asText()).isEqualTo(veilederId)
            assertThat(get("tilordnet").asText()).isEqualTo(tilordnet)
        }

        val info = meldingJson["veileder"]["veilederinformasjon"]
        assertThat(info["navIdent"].asText()).isEqualTo(veilederId)
        assertThat(info["visningsNavn"].asText()).isEqualTo("Jon Blund")
        assertThat(info["fornavn"].asText()).isEqualTo("Jon")
        assertThat(info["etternavn"].asText()).isEqualTo("Blund")
        assertThat(info["epost"].asText()).isEqualTo("Jonblund@jonb.no")
    }

    private fun veilederMeldingFraEksterntTopic(aktørId: String, veilederId: String, tilordnet: String) = """
        {
            "aktorId": "$aktørId",
            "veilederId": "$veilederId",
            "tilordnet": "$tilordnet"
        }
    """.trimIndent()
}
