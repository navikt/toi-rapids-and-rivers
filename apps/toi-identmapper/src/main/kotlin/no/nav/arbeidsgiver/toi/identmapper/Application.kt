package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.RapidApplication

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    val accessTokenClient = AccessTokenClient(System.getenv())
    val pdlKlient = PdlKlient(accessTokenClient)

    listOf("fnr", "fodselsnr", "fodselsnummer").forEach { fnrKey ->
        // AktørIdPopulator(fnrKey, rapidsConnection, pdlKlient::aktørIdFor)
    }
}.start()