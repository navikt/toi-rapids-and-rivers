package no.nav.arbeidsgiver.toi.kandidat.indekser

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.arbeidsgiver.toi.kandidat.indekser.domene.EsCv
import no.nav.toi.TestRapid
import org.apache.hc.core5.http.HttpHost
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers


@Testcontainers
class KandidatfeedTest {
    companion object {
        private val esIndex = "kandidatfeed"
        @Container
        private var elasticsearch: ElasticsearchContainer =
            ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.18.3")
                .withExposedPorts(9200)
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
        private lateinit var testEsClient: ESClient
        private lateinit var client: OpenSearchClient
    }

    @BeforeEach
    fun setUp() {
        testEsClient = ESClient(elasticsearch.httpHostAddress, esIndex, "kandidat", "kandidat")
        client = OpenSearchClient(
            ApacheHttpClient5TransportBuilder.builder(
                HttpHost.create(elasticsearch.httpHostAddress)
            ).build())
    }

    @Test
    fun `Melding med kun CV og aktørId vil ikke opprette kandidat i ES`() {
        val meldingMedKunCvOgAktørId = rapidMelding(synlighetJson = "")

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)
        testrapid.sendTestMessage(meldingMedKunCvOgAktørId)

        assertIngenIIndekser()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til true men dekte behov ikke eksisterer på meldingen skal ikke kandidat legges i ES`() {
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true))

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingSynlig)

        assertIngenIIndekser()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet til false men dekte behov ikke eksisterer skal kandidat enda slettes i ES`() {
        val kandidatnr = "CG133309"

        testEsClient.lagreEsCv(EsCvObjectMother.giveMeEsCv(kandidatnr = kandidatnr))

        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true), kandidatnr = kandidatnr)

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingUsynlig)

        assertIngenIIndekser()
    }

    @Test
    fun `Meldinger der synlighet er ferdig beregnet og har dekte behov skal kandidat legges til i ES`() {
        assertIngenIIndekser()
        val tomJson = """{}"""
        val expectedKandidatnr = "CG133310"
        val meldingSynlig = rapidMelding(
            synlighet(erSynlig = true, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            hullICv = tomJson,
            ontologi = tomJson,
            kandidatnr = expectedKandidatnr
        )
        val meldingUsynlig = rapidMelding(
            synlighet(erSynlig = false, ferdigBeregnet = true),
            organisasjonsenhetsnavn = "NAV et kontor",
            hullICv = tomJson,
            ontologi = tomJson,
            kandidatnr = expectedKandidatnr
        )

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingSynlig)
        testrapid.sendTestMessage(meldingUsynlig)

        assertEnKandidatMedKandidatnr(expectedKandidatnr)
    }

    @Test
    fun `Meldinger der synlighet ikke er ferdig beregnet skal ikke kandidat legges i ES`() {
        val meldingSynlig = rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = false))

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)
        testrapid.sendTestMessage(meldingSynlig)

        assertIngenIIndekser()
    }

    @Test
    fun `UsynligKandidatfeedLytter leser ikke melding om slutt_av_hendelseskjede er true`() {
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true), sluttAvHendelseskjede = true)

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingUsynlig)

        assertIngenIIndekser()
        assertThat(testrapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `SynligKandidatfeedLytter leser ikke melding om slutt_av_hendelseskjede er true`() {
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", hullICv = "{}", ontologi = "{}", sluttAvHendelseskjede = true)
        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(rapidMelding)

        assertIngenIIndekser()
        assertThat(testrapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `UsynligKandidatfeedLytter legger tilbake melding med slutt_av_hendelseskjede satt til true`() {
        val meldingUsynlig = rapidMelding(synlighet(erSynlig = false, ferdigBeregnet = true))

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(meldingUsynlig)

        assertThat(testrapid.inspektør.size).isEqualTo(1)
        assertThat(testrapid.inspektør.message(0).get("@slutt_av_hendelseskjede").booleanValue()).isEqualTo(true)
    }

    @Test
    fun `SynligKandidatfeedLytter legger tilbake melding med slutt_av_hendelseskjede satt til true`() {
        val rapidMelding =
            rapidMelding(synlighet(erSynlig = true, ferdigBeregnet = true), organisasjonsenhetsnavn = "NAV et kontor", hullICv = "{}", ontologi = "{}")

        val testrapid = TestRapid()

        SynligKandidatfeedLytter(testrapid, testEsClient)
        UsynligKandidatfeedLytter(testrapid, testEsClient)

        testrapid.sendTestMessage(rapidMelding)

        assertThat(testrapid.inspektør.size).isEqualTo(1)
        assertThat(testrapid.inspektør.message(0).get("@slutt_av_hendelseskjede").booleanValue()).isEqualTo(true)
    }

    private fun assertIngenIIndekser() {
        assertThat(client.count().count()).isEqualTo(0)
    }

    private fun assertEnKandidatMedKandidatnr(expectedKandidatnr: String) {
        assertThat(client.count().count()).isEqualTo(1)
        val cv = client.get({ req ->
            req.index(esIndex).id("123")
        }, EsCv::class.java)
        assertThat(cv.found()).isTrue
        assertThat(cv.index()).isEqualTo(expectedKandidatnr)
        assertThat(jacksonObjectMapper().readTree(cv.toJsonString())["kandidatnr"]).isEqualTo(expectedKandidatnr)
    }
}