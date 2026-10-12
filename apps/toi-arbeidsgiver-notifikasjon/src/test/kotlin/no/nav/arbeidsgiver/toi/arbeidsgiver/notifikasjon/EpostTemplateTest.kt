package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EpostTemplateTest {


    @Test
    fun `GET-kall mot template returnerer en statisk HTML-mal for e-poster som brukes mot arbeidsgivere`() {
        opprettJavalin { true }

        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:8301/template")).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        )

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).isEqualTo(epostTemplate)
    }

    @Test
    fun `lagEpostBody erstatter plassholdere med tittel, tekst og avsender`() {
        val body = lagEpostBody("En slags jobb", "Hei, her kommer to nye kandidater!", "Vetle Veileder")

        assertThat(body).contains("<title id='tittel'>En slags jobb</title>")
        assertThat(body).contains("<b id='stillingstittel'>En slags jobb</b>")
        assertThat(body).contains("id='tekst'>Hei, her kommer to nye kandidater!</p>")
        assertThat(body).contains("<span id='avsender'>Vetle Veileder</span>")
    }
}
