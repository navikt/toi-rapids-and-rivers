package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class InkomplettSynlighetsevalueringsgrunnlagLytterTest {

    companion object {
        private val aktørId = "1234"
        private val feltNavn = listOf(
            "arbeidsmarkedCv",
            "veileder",
            "oppfølgingsinformasjon",
            "siste14avedtak",
            "oppfølgingsperiode",
            "arenaFritattKandidatsøk",
            "hjemmel",
            "måBehandleTidligereCv",
            "kvp",
            "adressebeskyttelse")
        @JvmStatic
        private fun felter() = feltNavn.map { Arguments.of(it) }.stream()
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare et av datafeltene den trenger for synlighetsevaluering, skal den be om resten`(felt: String) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "$felt": {}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.hasNonNull(felt)).isTrue()
            assertThat(melding.get("@behov").asIterable().map(JsonNode::asText))
                .containsExactlyInAnyOrder(*feltNavn.toTypedArray())
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med alle behov utfylt trenger man ikke be om behov på nytt`(felt: String) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${feltNavn.joinToString(",","[","]"){""""$it""""}},
                "$felt": {}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(0)
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare noen behov utfylt trenger man be om behov på resten`(felt: String) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${(feltNavn.subList(0, feltNavn.size-3)).joinToString(",","[","]"){""""$it""""}},
                "$felt": {}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.get("@behov").asIterable().map(JsonNode::asText))
                .containsExactlyInAnyOrder(*feltNavn.toTypedArray())
            assertThat(melding.get("@behov").asIterable()).hasSize(feltNavn.size)
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det lå andre behov på meldingen så skal de ikke forsvinne når man ber om nye`(felt: String) {
        val annetBehov = "Uinterresant hendelse"
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${(feltNavn.subList(0, feltNavn.size-3) + annetBehov).joinToString(",","[","]"){""""$it""""}},
                "$felt": {}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.get("@behov").asIterable().map(JsonNode::asText))
                .containsExactlyInAnyOrder(*(feltNavn + annetBehov).toTypedArray())
            assertThat(melding.get("@behov").asIterable()).hasSize(feltNavn.size+1)
        })
    }

    @Test
    fun `Om det er en melding uten noen av de interessante feltene er ikke dette en relevant melding å reagere på`() {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId"
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(0)
        })
    }
}