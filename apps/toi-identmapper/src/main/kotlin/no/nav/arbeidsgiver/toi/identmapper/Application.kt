package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    val env = System.getenv()

    RapidApplication.create(env).also { rapidsConnection ->
        val accessTokenClient = AccessTokenClient(env)

        val pdlUrl = env["PDL_URL"]!!
        val pdlKlient = PdlKlient(pdlUrl, accessTokenClient)

        listOf("fnr", "fodselsnr", "fodselsnummer").forEach { fnrKey ->
            // AktørIdPopulator(fnrKey, rapidsConnection, pdlKlient::aktørIdFor)
        }
    }.start()
}