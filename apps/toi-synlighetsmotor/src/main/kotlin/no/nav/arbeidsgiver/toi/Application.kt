package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun repository() = Repository(datasource())

fun main() = RapidApplication.create(System.getenv()).also { SynlighetsLytter(it, repository()) }.start()

fun datasource() = DatabaseKonfigurasjon(System.getenv()).lagDatasource()

val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

