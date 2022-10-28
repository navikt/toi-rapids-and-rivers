package no.nav.arbeidsgiver.toi

import com.github.kittinunf.fuel.Fuel
import io.javalin.Javalin
import no.nav.arbeidsgiver.toi.api.Tilretteleggingsbehov
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TilretteleggingsbehovTest {

    private val javalinPort = 9000
    private val javalin = Javalin.create().start(javalinPort)
    private val tilretteleggingsbehovUrl = "http://localhost:$javalinPort/tilretteleggingsbehov"


    @Test
    fun `Skal kunne lagre tilretteleggingsbehov`() {
        var resultat: Tilretteleggingsbehov? = null
        //tilretteleggingsbehovController(javalinTestInstans, {})

        val inputDto = tilretteleggingsbehovInputJson()
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