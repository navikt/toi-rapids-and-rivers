package no.nav.toi.stilling.publiser.arbeidsplassen

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ArbeidsplassenResultat
import no.nav.toi.stilling.publiser.arbeidsplassen.dto.ArbeidsplassenStilling
import java.net.URI

interface ArbeidsplassenRestKlient {
    fun publiserStilling(stilling: ArbeidsplassenStilling)
}

class ArbeidsplassenRestKlientImpl(
    val baseUrl: URI,
    val autorizationToken: String,
): ArbeidsplassenRestKlient {
    companion object {
        val PROVIDER_ID = "15054" // Fast og endres ikke - samme i prod og dev
    }

    override fun publiserStilling(stilling: ArbeidsplassenStilling) {
        val body = objectMapper.writeValueAsString(stilling)
        val (_, response: Response, result: Result<String, FuelError>) = Fuel
            .post(path = "$baseUrl/stillingsimport/api/v1/transfers/$PROVIDER_ID")
            .header("Accept", "application/x-json-stream")
            .header("Cache-Control", "no-cache")
            .header("Content-type", "application/x-json-stream")
            .header("Authorization", "Bearer $autorizationToken")
            .body(body)
            .responseString()

        val feilmelding = "Klarte ikke å publisere stilling til Arbeidsplassen ${response.statusCode} ${response.responseMessage}"
        when (result) {
            is Result.Failure -> error(feilmelding)
            is Result.Success -> {
                if (response.statusCode != 200) {
                    log.error(feilmelding)
                    error(feilmelding)
                }
                val resultSomStreng = response.body().asString("text/html;charset=UTF-8")
                log.info("resultSomStreng: $resultSomStreng")
                val arbeidsplassenResultat = objectMapper.readValue(resultSomStreng, ArbeidsplassenResultat::class.java)
                log.info("Resultat av publisering til Arbeidsplassen: $arbeidsplassenResultat")

                if (arbeidsplassenResultat.status == "ERROR") {
                    val feilmeldingVedPublisering = "Feil ved publisering av stilling til Arbeidsplassen: ${arbeidsplassenResultat.message}"
                    log.error(feilmeldingVedPublisering)
                    error(feilmeldingVedPublisering)
                }
                log.info("Publiserte stilling til Arbeidsplassen OK: $arbeidsplassenResultat")
            }
        }
    }
}
