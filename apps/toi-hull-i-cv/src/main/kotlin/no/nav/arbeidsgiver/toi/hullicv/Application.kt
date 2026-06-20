package no.nav.arbeidsgiver.toi.hullicv

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication

fun main() = startApp(RapidApplication.create(System.getenv()))

fun startApp(rapidsConnection: RapidsConnection) = rapidsConnection.also {
    HullICvLytter(rapidsConnection)
}.start()
