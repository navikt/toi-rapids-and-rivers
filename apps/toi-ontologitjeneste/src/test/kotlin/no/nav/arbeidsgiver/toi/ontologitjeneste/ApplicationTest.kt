package no.nav.arbeidsgiver.toi.ontologitjeneste

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ApplicationTest {
    @Test
    fun `returnerer rapid når opprettelsen lykkes`() {
        assertThat(opprettRapidHvisJvmIkkeAvslutter { "rapid" }).isEqualTo("rapid")
    }

    @Test
    fun `returnerer null når JVM-en allerede avslutter`() {
        val rapid = opprettRapidHvisJvmIkkeAvslutter<String> { throw IllegalStateException("Shutdown in progress") }
        assertThat(rapid).isNull()
    }

    @Test
    fun `kaster videre IllegalStateException med annen melding`() {
        val feil = IllegalStateException("Noe annet gikk galt")
        assertThatThrownBy { opprettRapidHvisJvmIkkeAvslutter<String> { throw feil } }.isSameAs(feil)
    }

    @Test
    fun `kaster videre andre unntak`() {
        val feil = RuntimeException("Shutdown in progress")
        assertThatThrownBy { opprettRapidHvisJvmIkkeAvslutter<String> { throw feil } }.isSameAs(feil)
    }
}
