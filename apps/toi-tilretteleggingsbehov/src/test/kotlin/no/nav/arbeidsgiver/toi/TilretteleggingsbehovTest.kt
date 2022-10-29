package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.responseObject
import no.nav.arbeidsgiver.toi.api.*
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilretteleggingsbehovTest {

    private val tilretteleggingsbehovUrl = "http://localhost:9000/tilretteleggingsbehov"
    private val testRapid = TestRapid()
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    /*
    Test cases:
    - hvis ikke kan publisere på Kafka, reverser lagring i database og returner 500
     */

    @BeforeAll
    fun beforeAll() {
        startApp(testRapid, TestUtils.dataSource)
    }

    @Test
    fun `Skal kunne lagre tilretteleggingsbehov`() {
        val tilretteleggingsbehovInput = tilretteleggingsbehovInput()

        val (_, response, result) = Fuel.put(tilretteleggingsbehovUrl)
            .body(objectMapper.writeValueAsString(tilretteleggingsbehovInput))
            .responseString()

        assertThat(response.statusCode).isEqualTo(200)
        val lagreteTilretteleggingsbehov = hentTilretteleggingsbehov(tilretteleggingsbehovInput.fnr, TestUtils.dataSource)!!
        assertThat(lagreteTilretteleggingsbehov.fnr).isEqualTo(tilretteleggingsbehovInput.fnr)
        assertThat(lagreteTilretteleggingsbehov.arbeidshverdagen).isEqualTo(tilretteleggingsbehovInput.arbeidshverdagen)
        assertThat(lagreteTilretteleggingsbehov.fysisk).isEqualTo(tilretteleggingsbehovInput.fysisk)
        assertThat(lagreteTilretteleggingsbehov.utfordringerMedNorsk).isEqualTo(tilretteleggingsbehovInput.utfordringerMedNorsk)
        assertThat(lagreteTilretteleggingsbehov.arbeidstid).isEqualTo(tilretteleggingsbehovInput.arbeidstid)
        assertThat(lagreteTilretteleggingsbehov.sistEndretTidspunkt).isEqualToIgnoringSeconds(LocalDateTime.now())
        assertThat(lagreteTilretteleggingsbehov.sistEndretAv).isEqualTo("G-DUMMY")

        val tilretteleggingsbehovIResponsen = objectMapper.readValue(result.get(), Tilretteleggingsbehov::class.java)
        assertThat(lagreteTilretteleggingsbehov).isEqualTo(tilretteleggingsbehovIResponsen)
        TODO("Assert sending på rapid")
    }

    private fun tilretteleggingsbehovInput() =
        TilretteleggingsbehovInput(
            fnr = "12121287654",
            arbeidstid = setOf(Arbeidstid.IKKE_HELE_DAGER),
            fysisk = setOf(),
            arbeidshverdagen = setOf(Arbeidshverdagen.STILLE_OG_ROLIG_MILJØ),
            utfordringerMedNorsk = setOf(UtfordringerMedNorsk.SNAKKE_NORSK)
        )
}