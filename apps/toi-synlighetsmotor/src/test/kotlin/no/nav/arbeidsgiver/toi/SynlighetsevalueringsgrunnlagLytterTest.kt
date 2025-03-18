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

class SynlighetsevalueringsgrunnlagLytterTest {

    companion object {
        private val aktørId = "1234"
        @JvmStatic
        private fun felter() = Felt.entries.map { Arguments.of(it) }.stream()
    }
    enum class Felt(val navn: String, val skalGiSynligTrue: String, val skalGiSynligFalse: String) {
        ARBEIDSMARKED_CV("arbeidsmarkedCv", arbeidsmarkedCv(OPPRETT), arbeidsmarkedCv(SLETT)),
        OPPFØLGINGSINFORMASJON("oppfølgingsinformasjon", oppfølgingsinformasjon(), oppfølgingsinformasjon(erDoed = true)),
        OPPFØLGINGSPERIODE("oppfølgingsperiode", aktivOppfølgingsperiode(), avsluttetOppfølgingsperiode()),
        ARENAFRITATTKANDIDATSØK("arenaFritattKandidatsøk", arenaFritattKandidatsøk(fnr = null), arenaFritattKandidatsøk(fritattKandidatsøk = true, fnr = null)),
        HJEMMEL("hjemmel", hjemmel(), hjemmel(opprettetDato = null, slettetDato = null)),
        MÅBEHANDLETIDLIGERECV("måBehandleTidligereCv", måBehandleTidligereCv(false), måBehandleTidligereCv(true)),
        KVP("kvp", kvp(event = "AVSLUTTET"), kvp(event = "STARTET")),
        ADRESSEBESKYTTELSE("adressebeskyttelse", """"adressebeskyttelse":"UGRADERT"""", adressebeskyttelse("STRENGT_FORTROLIG")),
    }

    private val alleFelter = Felt.entries.map(Felt::navn) + "veileder" + "siste14avedtak"

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare et av datafeltene den trenger for synlighetsevaluering, skal den be om resten om synlighet er true på evaluering på det den har mottatt`(felt: Felt) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                ${felt.skalGiSynligTrue}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(melding.path(felt.navn).isMissingNode).isFalse()
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
                ${felt.skalGiSynligFalse}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(felt.navn in melding.fieldNames().asSequence().toList()).isTrue()
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
                ${felt.skalGiSynligFalse}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            assertThat(felt.navn in melding.fieldNames().asSequence().toList()).isTrue()
            assertThat(melding.path("@behov").asIterable().map(JsonNode::asText)).containsExactly(preBehov)
            melding.path("synlighet").apply {
                assertThat(path("erSynlig").asBoolean()).isFalse()
                assertThat(path("ferdigBeregnet").asBoolean()).isTrue()
            }
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med alle behov lagt på trenger man ikke be om behov på nytt`(felt: Felt) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${alleFelter.joinToString(",","[","]"){""""$it""""}},
                ${felt.skalGiSynligTrue}
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(0)
        })
    }

    @ParameterizedTest
    @MethodSource("felter")
    fun `Om det kommer en melding med bare noen behov lagt på trenger man be om behov på resten`(felt: Felt) {
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${(alleFelter.subList(0, Felt.entries.size-3)).joinToString(",","[","]"){""""$it""""}},
                ${felt.skalGiSynligTrue}
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
                ${felt.skalGiSynligTrue}
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

    @Test
    fun `Om både livshendelse og oppfølginsinformasjon sier at synlig er false skal synlig være false`() {
        val alleFelterSattTilÅGiSynligTrue = ((Felt.entries-Felt.ADRESSEBESKYTTELSE-Felt.OPPFØLGINGSINFORMASJON)
            .map(Felt::skalGiSynligTrue)).joinToString()
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${alleFelter.joinToString(",","[","]"){""""$it""""}},
                ${oppfølgingsinformasjon(diskresjonskode = "6")},
                ${Felt.ADRESSEBESKYTTELSE.skalGiSynligFalse},
                $alleFelterSattTilÅGiSynligTrue
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            (Felt.entries.map(Felt::navn)).forEach { feltNavn ->
                assertThat(feltNavn in melding.fieldNames().asSequence().toList()).isTrue()
            }
            assertThat(melding.path("@behov").map(JsonNode::asText)).containsExactlyInAnyOrder(*alleFelter.toTypedArray())
            melding.path("synlighet").apply {
                assertThat(path("erSynlig").asBoolean()).isFalse()
                assertThat(path("ferdigBeregnet").asBoolean()).isTrue()
            }
        })
    }

    @Test
    fun `Om bare livshendelse men ikke oppfølginsinformasjon sier at synlig er false skal synlig være false`() {

        val alleFelterSattTilÅGiSynligTrue = ((Felt.entries-Felt.ADRESSEBESKYTTELSE-Felt.OPPFØLGINGSINFORMASJON)
            .map(Felt::skalGiSynligTrue)).joinToString()
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${alleFelter.joinToString(",","[","]"){""""$it""""}},
                ${Felt.OPPFØLGINGSINFORMASJON.skalGiSynligTrue},
                ${Felt.ADRESSEBESKYTTELSE.skalGiSynligFalse},
                $alleFelterSattTilÅGiSynligTrue
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            (Felt.entries.map(Felt::navn)).forEach { feltNavn ->
                assertThat(feltNavn in melding.fieldNames().asSequence().toList()).isTrue()
            }
            assertThat(melding.path("@behov").map(JsonNode::asText)).containsExactlyInAnyOrder(*alleFelter.toTypedArray())
            melding.path("synlighet").apply {
                assertThat(path("erSynlig").asBoolean()).isFalse()
                assertThat(path("ferdigBeregnet").asBoolean()).isTrue()
            }
        })
    }

    @Test
    fun `Om bare oppfølginsinformasjon men ikke livshendelse sier at synlig er false skal synlig være false`() {
        val alleFelterSattTilÅGiSynligTrue = ((Felt.entries-Felt.ADRESSEBESKYTTELSE-Felt.OPPFØLGINGSINFORMASJON)
            .map(Felt::skalGiSynligTrue)).joinToString()
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${alleFelter.joinToString(",","[","]"){""""$it""""}},
                ${oppfølgingsinformasjon(diskresjonskode = "6")},
                ${Felt.ADRESSEBESKYTTELSE.skalGiSynligTrue},
                $alleFelterSattTilÅGiSynligTrue
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            (Felt.entries.map(Felt::navn)).forEach { feltNavn ->
                assertThat(feltNavn in melding.fieldNames().asSequence().toList()).isTrue()
            }
            assertThat(melding.path("@behov").map(JsonNode::asText)).containsExactlyInAnyOrder(*alleFelter.toTypedArray())
            melding.path("synlighet").apply {
                assertThat(path("erSynlig").asBoolean()).isFalse()
                assertThat(path("ferdigBeregnet").asBoolean()).isTrue()
            }
        })
    }

    @Test
    fun `Om både oppfølginsinformasjon og livshendelse sier at synlig er true skal synlig være true`(){
        val alleFelterSattTilÅGiSynligTrue = ((Felt.entries-Felt.ADRESSEBESKYTTELSE-Felt.OPPFØLGINGSINFORMASJON)
            .map(Felt::skalGiSynligTrue)).joinToString()
        testProgramMedHendelse("""
            {
                "aktørId": "$aktørId",
                "@behov": ${alleFelter.joinToString(",","[","]"){""""$it""""}},
                ${Felt.OPPFØLGINGSINFORMASJON.skalGiSynligTrue},
                ${Felt.ADRESSEBESKYTTELSE.skalGiSynligTrue},
                $alleFelterSattTilÅGiSynligTrue
            }
        """.trimIndent(), {
            assertThat(size).isEqualTo(1)
            val melding = message(0)
            (Felt.entries.map(Felt::navn)).forEach { feltNavn ->
                assertThat(feltNavn in melding.fieldNames().asSequence().toList()).isTrue()
            }
            assertThat(melding.path("@behov").map(JsonNode::asText)).containsExactlyInAnyOrder(*alleFelter.toTypedArray())
            melding.path("synlighet").apply {
                assertThat(path("erSynlig").asBoolean()).isTrue()
                assertThat(path("ferdigBeregnet").asBoolean()).isTrue()
            }
        })
    }
}