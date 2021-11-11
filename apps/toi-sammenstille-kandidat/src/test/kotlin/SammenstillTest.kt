import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SammenstillTest {

    @Test
    fun `Når veileder og CV har blitt mottatt for kandidat skal ny melding publiseres på rapid`() { //TODO: bedre navn
        val testRapid = TestRapid()
        startApp(TestDatabase.dataSource, testRapid)

        val aktørId = "AktørId"
        testRapid.publish(veilederMelding(aktørId, testRapid))
        testRapid.publish(cvMelding(aktørId, testRapid))

        Thread.sleep(200)
        val rapidInspektør = testRapid.inspektør
        assertThat(rapidInspektør.size).isEqualTo(3)
        // assert at siste melding er sammenstilt

        // TODO: Hent ut kandidat fra database
    }

    private fun veilederMelding(aktørId: String, rapid: RapidsConnection) = """
        {
            "@event_name": "Kandidat.ny_veileder",
            "aktørId": "$aktørId"
        }
    """.trimIndent()

    private fun cvMelding(aktørId: String, rapid: RapidsConnection) = """
        {
            "@event_name": "Kandidat.NyFraArbeidsplassen",
            "aktørId": "$aktørId"
        }
    """.trimIndent()
}