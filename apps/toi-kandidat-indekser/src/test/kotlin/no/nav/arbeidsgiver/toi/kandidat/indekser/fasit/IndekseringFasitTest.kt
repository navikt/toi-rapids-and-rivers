package no.nav.arbeidsgiver.toi.kandidat.indekser.fasit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.head
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import no.nav.arbeidsgiver.toi.kandidat.indekser.ESClient
import no.nav.arbeidsgiver.toi.kandidat.indekser.SynligKandidatfeedLytter
import no.nav.toi.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import tools.jackson.databind.json.JsonMapper
import java.io.File

/**
 * Sikrer at dokumentet som sendes til OpenSearch ikke endrer seg.
 *
 * En melding fra src/test/resources/fasit går gjennom den ekte SynligKandidatfeedLytter og ESClient. WireMock tar imot
 * forespørselen, og dokumentet sammenlignes med fasitfilen ved siden av meldingen.
 *
 * Etter en tilsiktet endring skriver du fasitfilene på nytt med
 *   OPPDATER_FASIT=true ./gradlew :apps:toi-kandidat-indekser:test --tests "*IndekseringFasitTest*"
 * og går gjennom endringene i git diff før du committer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndekseringFasitTest {

    private val openSearch = WireMockServer(options().dynamicPort()).also(WireMockServer::start).apply {
        stubFor(head(urlEqualTo("/kandidater-1")).willReturn(ok()))
        stubFor(get(urlEqualTo("/_alias/kandidater")).willReturn(okJson("""{ "kandidater-1": { "aliases": { "kandidater": {} } } }""")))
        stubFor(
            put(urlPathMatching("/kandidater-1/_doc/.+")).willReturn(
                okJson(
                    """
                    {
                      "_index": "kandidater-1", "_id": "CG133309", "_version": 1, "result": "created",
                      "_shards": { "total": 1, "successful": 1, "failed": 0 }, "_seq_no": 0, "_primary_term": 1
                    }
                    """
                )
            )
        )
    }
    private val esClient = ESClient(openSearch.baseUrl(), "brukernavn", "passord")

    @AfterAll
    fun afterAll() = openSearch.stop()

    @Test
    fun `kandidat med alle felt utfylt`() {
        val dokument = indekser(File("src/test/resources/fasit/kandidat-melding.json").readText())

        sammenlignMedFasit(File("src/test/resources/fasit/kandidat-dokument.json"), dokument)
    }

    @Test
    fun `kandidat med nullfelt og datoer som tekst`() {
        val dokument = indekser(File("src/test/resources/fasit/kandidat-med-nullfelt-melding.json").readText())

        sammenlignMedFasit(File("src/test/resources/fasit/kandidat-med-nullfelt-dokument.json"), dokument)
    }

    /** Sender meldingen til lytteren og returnerer dokumentet slik ESClient sendte det til OpenSearch. */
    private fun indekser(melding: String): String {
        openSearch.resetRequests()

        val rapid = TestRapid()
        SynligKandidatfeedLytter(rapid, esClient)
        rapid.sendTestMessage(melding)

        val forespørsel = openSearch.findAll(putRequestedFor(urlPathMatching("/kandidater-1/_doc/.+"))).single()
        assertThat(forespørsel.url).isEqualTo("/kandidater-1/_doc/CG133309?refresh=true")
        return forespørsel.bodyAsString
    }

    private fun sammenlignMedFasit(fasit: File, dokument: String) {
        if (System.getenv("OPPDATER_FASIT") == "true") {
            val mapper = JsonMapper.shared()
            fasit.writeText(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(dokument)) + "\n")
        }

        assertThatJson(dokument).isEqualTo(fasit.readText())
    }
}
