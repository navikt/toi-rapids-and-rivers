package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.RapidApplication

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    listOf("fnr", "fodselsnr", "fodselsnummer").forEach { fnrKey ->
        AktørIdPopulator(fnrKey, rapidsConnection, PdlKlient())
    }
}.start()