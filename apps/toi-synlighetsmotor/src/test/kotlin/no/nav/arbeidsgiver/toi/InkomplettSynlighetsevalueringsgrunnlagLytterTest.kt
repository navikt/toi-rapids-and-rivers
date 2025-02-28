package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import no.nav.arbeidsgiver.toi.CvMeldingstype.*
import no.nav.arbeidsgiver.toi.Testdata.Companion.adressebeskyttelse
import no.nav.arbeidsgiver.toi.Testdata.Companion.aktivOppfølgingsperiode
import no.nav.arbeidsgiver.toi.Testdata.Companion.arbeidsmarkedCv
import no.nav.arbeidsgiver.toi.Testdata.Companion.arenaFritattKandidatsøk
import no.nav.arbeidsgiver.toi.Testdata.Companion.avsluttetOppfølgingsperiode
import no.nav.arbeidsgiver.toi.Testdata.Companion.hjemmel
import no.nav.arbeidsgiver.toi.Testdata.Companion.kvp
import no.nav.arbeidsgiver.toi.Testdata.Companion.måBehandleTidligereCv
import no.nav.arbeidsgiver.toi.Testdata.Companion.oppfølgingsinformasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class InkomplettSynlighetsevalueringsgrunnlagLytterTest {

    companion object {
        private val aktørId = "1234"
        @JvmStatic
        private fun felter() = Felt.entries.map { Arguments.of(it) }.stream()
    }
    enum class Felt(val navn: String, val synligTrue: String, val synligFalse: String) {
        ARBEIDSMARKED_CV("arbeidsmarkedCv", arbeidsmarkedCv(OPPRETT), arbeidsmarkedCv(SLETT)),
        OPPFØLGINGSINFORMASJON("oppfølgingsinformasjon", oppfølgingsinformasjon(), oppfølgingsinformasjon(erDoed = true)),
        OPPFØLGINGSPERIODE("oppfølgingsperiode", aktivOppfølgingsperiode(), avsluttetOppfølgingsperiode()),
        ARENAFRITATTKANDIDATSØK("arenaFritattKandidatsøk", arenaFritattKandidatsøk(fnr = null), arenaFritattKandidatsøk(fritattKandidatsøk = true, fnr = null)),
        HJEMMEL("hjemmel", hjemmel(), hjemmel(opprettetDato = null, slettetDato = null)),
        MÅBEHANDLETIDLIGERECV("måBehandleTidligereCv", måBehandleTidligereCv(false), måBehandleTidligereCv(true)),
        KVP("kvp", kvp(event = "STARTET"), kvp(event = "AVSLUTTET")),
        ADRESSEBESKYTTELSE("adressebeskyttelse", """"adressebeskyttelse":null""", adressebeskyttelse())
    }

    private val alleFelter = Felt.entries.map(Felt::navn) + "veileder" + "siste14avedtak"

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare et av datafeltene den trenger for synlighetsevaluering, skal den be om resten om synlighet er true på evaluering på det den har mottatt`(felt: Felt) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                ${felt.synligTrue}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.hasNonNull(felt.navn)).isTrue()
            assertThat(melding.path("@behov").asIterable().map(JsonNode::asText))
                .containsExactlyInAnyOrder(*alleFelter.toTypedArray())
            assertThat(melding.path("synlighet").isMissingNode).isTrue()
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare et av datafeltene den trenger for synlighetsevaluering, skal den ikke be om resten om synlighet er false på evaluering på det den har mottatt, men heller sende videre med synlighet false`(felt: Felt) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                ${felt.synligFalse}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.hasNonNull(felt.navn)).isTrue()
            assertThat(melding.path("@behov").isMissingNode).isTrue()
            melding.path("synlighet").apply {
                assertThat(path("erSynlig").asBoolean()).isFalse()
                assertThat(path("ferdigBeregnet").asBoolean()).isTrue()
            }
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare et av datafeltene den trenger for synlighetsevaluering, skal den ikke be om resten om synlighet er false på evaluering på det den har mottatt, men heller sende videre med synlighet false, også når andre behov finnes fra før`(felt: Felt) {
        val preBehov = "Uinterresant behov"
        testProgramMedHendelse("""
            {
                "@behov": ["$preBehov"],
                "aktørId": "$aktørId",
                ${felt.synligFalse}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.hasNonNull(felt.navn)).isTrue()
            assertThat(melding.path("@behov").asIterable().map(JsonNode::asText)).containsExactly(preBehov)
            melding.path("synlighet").apply {
                assertThat(path("erSynlig").asBoolean()).isFalse()
                assertThat(path("ferdigBeregnet").asBoolean()).isTrue()
            }
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med alle behov utfylt trenger man ikke be om behov på nytt`(felt: Felt) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${alleFelter.joinToString(",","[","]"){""""$it""""}},
                ${felt.synligTrue}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            assertThat(field(0, "@behov").isMissingNode).isTrue()
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare noen behov utfylt trenger man be om behov på resten`(felt: Felt) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${(alleFelter.subList(0, Felt.entries.size-3)).joinToString(",","[","]"){""""$it""""}},
                ${felt.synligTrue}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.path("@behov").asIterable().map(JsonNode::asText))
                .containsExactlyInAnyOrder(*alleFelter.toTypedArray())
            assertThat(melding.path("@behov").asIterable()).hasSize(alleFelter.size)
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det lå andre behov på meldingen så skal de ikke forsvinne når man ber om nye`(felt: Felt) {
        val annetBehov = "Uinterresant hendelse"
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${(alleFelter.subList(0, Felt.entries.size-3) + annetBehov).joinToString(",","[","]"){""""$it""""}},
                ${felt.synligTrue}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.path("@behov").asIterable().map(JsonNode::asText))
                .containsExactlyInAnyOrder(*(alleFelter + annetBehov).toTypedArray())
            assertThat(melding.path("@behov").asIterable()).hasSize(alleFelter.size+1)
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