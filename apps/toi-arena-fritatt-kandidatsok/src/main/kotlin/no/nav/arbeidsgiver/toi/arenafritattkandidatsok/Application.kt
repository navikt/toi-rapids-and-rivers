package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun main() = RapidApplication.create(System.getenv()).also { rapidsConnection ->
    val dataSource = DatabaseKonfigurasjon(System.getenv()).lagDatasource()
    val repository = FritattRepository(dataSource)

    rapidsConnection.register(object : RapidsConnection.StatusListener {
        override fun onStartup(rapidsConnection: RapidsConnection) {
            repository.flywayMigrate(dataSource)
        }
    })

    ArenaFritattKandidatsokLytter(rapidsConnection, repository)
}.start()


val Any.log: Logger
    get() = LoggerFactory.getLogger(this::class.java)

fun Instant.atOslo(): ZonedDateTime = this.atZone(ZoneId.of("Europe/Oslo"))

fun LocalDateTime.atOsloSameInstant() = atZone(ZoneId.systemDefault()).withZoneSameInstant(
        ZoneId.of("Europe/Oslo")
    )

val arenaTidsformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")