import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SammenstillTest {

    @Test
    fun `Når veileder og CV har blitt mottatt for kandidat skal ny melding publiseres på rapid`() { //TODO: bedre navn
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        startApp(TestDatabase.dataSource, testRapid)
        testRapid.sendTestMessage(veilederMelding(aktørId, testRapid))
        testRapid.sendTestMessage(cvMelding(aktørId, testRapid))

        Thread.sleep(1000)
        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(1)
        assertThat(rapidInspektør.message(0).get("cv")).isNotNull
        assertThat(rapidInspektør.message(0).get("cv").asText()).isNotEqualTo("null")
    }

    private fun veilederMelding(aktørId: String, rapid: RapidsConnection) = """
        {
            "@event_name": "veileder",
            "aktørId": "$aktørId"
        }
    """.trimIndent()

    private fun cvMelding(aktørId: String, rapid: RapidsConnection) = """
        {
            "@event_name": "cv",
            "aktørId": "$aktørId"
        }
    """.trimIndent()
}