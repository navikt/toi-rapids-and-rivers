package no.nav.arbeidsgiver.toi.veileder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VeilederTest {

    val wiremock = WireMockServer(8089)
    private val url = "http://localhost:8089/graphql"
    private val accessToken = "TestAccessToken"

    @BeforeAll
    fun setup() {
        wiremock.start()
    }

    @AfterAll
    fun tearDown() {
        wiremock.stop()
    }

    fun stubWireMock(query: String, responseBody: String) {
        wiremock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/graphql"))
                .withRequestBody(WireMock.equalToJson(query))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
    }

    fun createTestRapidAndLytter(): TestRapid {
        val testRapid = TestRapid()
        val nomKlient = NomKlient(url = url) { accessToken }
        VeilederLytter(testRapid, nomKlient)
        return testRapid
    }

    private fun createQuery(veilederId: String): String {
        return """{
          "query": "query(${'$'}identer: [String!]!) {\n    ressurser(where: { navidenter: ${'$'}identer }) {\n        id\n        ressurs {\n            navIdent\n            visningsNavn\n            fornavn\n            etternavn\n            epost\n        }\n    }\n}",
          "variables": {
            "identer": ["$veilederId"]
          }
        }"""
    }

    private fun createResponseBodyWithData(veilederId: String): String {
        return """{
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
        }"""
    }

    private fun veilederMeldingFraEksterntTopic(aktørId: String, veilederId: String, tilordnet: String) = """
        {
            "aktorId": "$aktørId",
            "veilederId": "$veilederId",
            "tilordnet": "$tilordnet"
        }
    """.trimIndent()

    private fun performCommonAssertions(inspektør: TestRapid.RapidInspector, aktørId: String, veilederId: String, tilordnet: String) {
        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)
        assertThat(meldingJson["@event_name"].asText()).isEqualTo("veileder")
        assertThat(meldingJson["aktørId"].asText()).isEqualTo(aktørId)

        val veilederJson = meldingJson["veileder"]
        assertThat(veilederJson["aktorId"].asText()).isEqualTo(aktørId)
        assertThat(veilederJson["veilederId"].asText()).isEqualTo(veilederId)
        assertThat(veilederJson["tilordnet"].asText()).isEqualTo(tilordnet)
    }

    @Test
    fun `Lesing av veilederMelding fra eksternt topic skal produsere ny melding på rapid`() {
        // arrange
        val aktørId = "10000100000"
        val veilederId = "A313111"
        val tilordnet = "2020-12-21T10:58:19.023+01:00"

        val query = createQuery(veilederId)
        val responseBody = createResponseBodyWithData(veilederId)

        stubWireMock(query, responseBody)

        val testRapid = createTestRapidAndLytter()

        // act
        testRapid.sendTestMessage(veilederMeldingFraEksterntTopic(aktørId, veilederId, tilordnet))

        // assert
        val inspektør = testRapid.inspektør
        performCommonAssertions(inspektør, aktørId, veilederId, tilordnet)
    }

    @Test
    fun `Lesing av veilederMelding der nom ikke har ressurs`() {
        val aktørId = "10000100000"
        val veilederId = "A313111"
        val tilordnet = "2020-12-21T10:58:19.023+01:00"

        val query = """
    {
      "query": "query(${'$'}identer: [String!]!) {\n    ressurser(where: { navidenter: ${'$'}identer }) {\n        id\n        ressurs {\n            navIdent\n            visningsNavn\n            fornavn\n            etternavn\n            epost\n        }\n    }\n}",
      "variables": {
        "identer": ["$veilederId"]
      }
    }
    """

        val responseBody = """
    {
      "data": {
        "ressurser": [
          {
            "id": "A112123",
            "ressurs": null
          }
        ]
      }
    }
    """

        stubWireMock(query, responseBody)

        val testRapid = createTestRapidAndLytter()
        testRapid.sendTestMessage(veilederMeldingFraEksterntTopic(aktørId, veilederId, tilordnet))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson["@event_name"].asText()).isEqualTo("veileder")
        assertThat(meldingJson["aktørId"].asText()).isEqualTo(aktørId)

        val veilederJson = meldingJson["veileder"]
        assertThat(veilederJson["aktorId"].asText()).isEqualTo(aktørId)
        assertThat(veilederJson["veilederId"].asText()).isEqualTo(veilederId)
        assertThat(veilederJson["tilordnet"].asText()).isEqualTo(tilordnet)
        assertThat(veilederJson["veilederinformasjon"].isNull).isTrue()
    }

    @Test
    fun `Lesing av veilederMelding der nom har ressurs som har nullverdier`() {
        // Initialize test variables
        val aktørId = "10000100000"
        val veilederId = "A313111"
        val tilordnet = "2020-12-21T10:58:19.023+01:00"

        val spørring = createQuery(veilederId)

        val responseBody = """{
      "data": {
        "ressurser": [
          {
            "id": "A112123",
            "ressurs": {
              "navIdent": null,
              "visningsNavn": null,
              "fornavn": null,
              "etternavn": null,
              "epost": null
            }
          }
        ]
      }
    }"""

        stubWireMock(spørring, responseBody)

        val testRapid = createTestRapidAndLytter()
        testRapid.sendTestMessage(veilederMeldingFraEksterntTopic(aktørId, veilederId, tilordnet))

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)
        assertThat(meldingJson["@event_name"].asText()).isEqualTo("veileder")
        assertThat(meldingJson["aktørId"].asText()).isEqualTo(aktørId)

        val veilederJson = meldingJson["veileder"]
        assertThat(veilederJson["aktorId"].asText()).isEqualTo(aktørId)
        assertThat(veilederJson["veilederId"].asText()).isEqualTo(veilederId)
        assertThat(veilederJson["tilordnet"].asText()).isEqualTo(tilordnet)
        assertThat(
            jacksonObjectMapper().treeToValue(
                veilederJson["veilederinformasjon"],
                NomKlient.Veilederinformasjon::class.java
            )
        ).isEqualTo(
            NomKlient.Veilederinformasjon(
                navIdent = null,
                visningsNavn = null,
                fornavn = null,
                etternavn = null,
                epost = null
            )
        )
    }

    @Test
    fun `Lesing av veilederMelding fra eksternt topic skal produsere ny melding på rapid om det er error melding, og i tillegg ressurs`() {
        val aktørId = "10000100000"
        val veilederId = "A313111"
        val tilordnet = "2020-12-21T10:58:19.023+01:00"

        val spørring = """
            {
              "query": "query(${'$'}identer: [String!]!) {\n    ressurser(where: { navidenter: ${'$'}identer }) {\n        id\n        ressurs {\n            navIdent\n            visningsNavn\n            fornavn\n            etternavn\n            epost\n        }\n    }\n}",
              "variables": {
                "identer": ["$veilederId"]
              }
            }
            """

        val responsBody = """
            {
              "errors": [
                {
                  "message": "AD har ingen data på nav-ident: $veilederId",
                  "locations": [],
                  "extensions": {
                    "code": "not_found",
                    "classification": "ExecutionAborted"
                  }
                }
              ],
              "data": {
                "ressurser": [
                  {
                    "id": "$veilederId",
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
            """

        stubWireMock(spørring, responsBody)

        val testRapid = createTestRapidAndLytter()
        testRapid.sendTestMessage(veilederMeldingFraEksterntTopic(aktørId, veilederId, tilordnet))

        val inspektør = testRapid.inspektør

        assertThat(inspektør.size).isEqualTo(1)
        val meldingJson = inspektør.message(0)

        assertThat(meldingJson["@event_name"].asText()).isEqualTo("veileder")
        assertThat(meldingJson["aktørId"].asText()).isEqualTo(aktørId)

        val veilederJson = meldingJson["veileder"]
        assertThat(veilederJson["aktorId"].asText()).isEqualTo(aktørId)
        assertThat(veilederJson["veilederId"].asText()).isEqualTo(veilederId)
        assertThat(veilederJson["tilordnet"].asText()).isEqualTo(tilordnet)
        assertThat(
            jacksonObjectMapper().treeToValue(
                veilederJson["veilederinformasjon"],
                NomKlient.Veilederinformasjon::class.java
            )
        ).isEqualTo(
            NomKlient.Veilederinformasjon(
                navIdent = veilederId,
                visningsNavn = "Jon Blund",
                fornavn = "Jon",
                etternavn = "Blund",
                epost = "Jonblund@jonb.no"
            )
        )
    }

}
