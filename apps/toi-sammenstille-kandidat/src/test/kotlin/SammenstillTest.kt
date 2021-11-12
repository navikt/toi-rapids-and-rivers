import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class SammenstillTest {

    @Test
    fun `Når veileder og CV har blitt mottatt for kandidat skal ny melding publiseres på rapid`() { //TODO: bedre navn
        val aktørId = "10000100000"
        val testRapid = TestRapid()
        startApp(TestDatabase.dataSource, testRapid)
        testRapid.sendTestMessage(veilederMelding(aktørId, testRapid))
        testRapid.sendTestMessage(cvMelding(aktørId, testRapid))

        println("rapidinspektør:" + testRapid.inspektør.message(0) + " " + testRapid.inspektør.message(1))



        Thread.sleep(1000)
        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(3)

        // assert at siste melding er sammenstilt

        // TODO: Hent ut kandidat fra database
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