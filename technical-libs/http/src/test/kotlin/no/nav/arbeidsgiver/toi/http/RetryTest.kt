package no.nav.arbeidsgiver.toi.http

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.Optional
import javax.net.ssl.SSLSession

class RetryTest {

    @Test
    fun `prøver på nytt ved 5xx og returnerer første vellykkede svar`() {
        val svar = ArrayDeque(listOf(503, 502, 200))
        var antallKall = 0

        val response = medRetry(ventetid = Duration.ZERO) { antallKall++; respons(svar.removeFirst()) }

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(antallKall).isEqualTo(3)
    }

    @Test
    fun `prøver på nytt ved timeout`() {
        var antallKall = 0

        val response = medRetry(ventetid = Duration.ZERO) {
            antallKall++
            if (antallKall == 1) throw HttpTimeoutException("request timed out")
            respons(200)
        }

        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(antallKall).isEqualTo(2)
    }

    /*@Test
    fun `prøver ikke på nytt ved 4xx`() {
        var antallKall = 0

        val response = medRetry(ventetid = Duration.ZERO) { antallKall++; respons(404) }

        assertThat(response.statusCode()).isEqualTo(404)
        assertThat(antallKall).isEqualTo(1)
    }*/

    @Test
    fun `returnerer siste 5xx-svar når alle forsøk er brukt opp`() {
        var antallKall = 0

        val response = medRetry(maksForsøk = 3, ventetid = Duration.ZERO) { antallKall++; respons(503) }

        assertThat(response.statusCode()).isEqualTo(503)
        assertThat(antallKall).isEqualTo(3)
    }

    @Test
    fun `kaster siste unntak når alle forsøk er brukt opp`() {
        var antallKall = 0

        assertThatThrownBy {
            medRetry<String>(maksForsøk = 3, ventetid = Duration.ZERO) {
                antallKall++
                throw HttpTimeoutException("timeout $antallKall")
            }
        }.isInstanceOf(HttpTimeoutException::class.java).hasMessage("timeout 3")
        assertThat(antallKall).isEqualTo(3)
    }

    private fun respons(statuskode: Int): HttpResponse<String> = object : HttpResponse<String> {
        private val uri = URI.create("http://localhost/test")
        override fun statusCode() = statuskode
        override fun request(): HttpRequest = HttpRequest.newBuilder(uri).build()
        override fun previousResponse(): Optional<HttpResponse<String>> = Optional.empty()
        override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
        override fun body() = ""
        override fun sslSession(): Optional<SSLSession> = Optional.empty()
        override fun uri() = uri
        override fun version() = HttpClient.Version.HTTP_1_1
    }
}
