package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon
import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EpostTemplateTest {

    @Test
    fun `lagEpostBody erstatter plassholdere med tittel, tekst og avsender`() {
        val body = lagEpostBody("En slags jobb", "Hei, her kommer to nye kandidater!", "Vetle Veileder")

        assertThat(body.contains("<title>En slags jobb</title>")).isTrue()
        assertThat(body.contains("<pre style='font-family: unset;'>Hei, her kommer to nye kandidater!</pre>")).isTrue()
        assertThat(body.contains("<p style='padding-block:40px 32px'>Vennlig hilsen Vetle Veileder</p>")).isTrue()
    }
}