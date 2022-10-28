package no.nav.arbeidsgiver.toi

import com.github.kittinunf.fuel.Fuel
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TilretteleggingsbehovTest {

    private val tilretteleggingsbehovUrl = "http://localhost:9000/tilretteleggingsbehov"
    private val testRapid = TestRapid()

    @BeforeAll
    fun beforeAll() {
        startApp(testRapid, TestUtils.dataSource)
    }

    @Test
    fun `Skal kunne lagre tilretteleggingsbehov`() {
        val (request, response, result) = Fuel.put(tilretteleggingsbehovUrl)
            .body(tilretteleggingsbehovInputJson())
            .response()

        assertThat(response.statusCode).isEqualTo(200)
    }

    private fun tilretteleggingsbehovInputJson() =
        """
            {
                "fnr":"21909899211",
                "arbeidstid":["IKKE_HELE_DAGER"],
                "fysisk":[],
                "arbeidshverdagen":["STILLE_OG_ROLIG_MILJØ"],
                "utfordringerMedNorsk":["SNAKKE"]
            }
        """.trimIndent()

}