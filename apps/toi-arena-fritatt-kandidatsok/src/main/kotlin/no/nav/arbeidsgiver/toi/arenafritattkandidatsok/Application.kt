package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun main() {
    val dataSource = DatabaseKonfigurasjon(System.getenv()).lagDatasource()
    val repository = FritattRepository(dataSource)

    RapidApplication.create(System.getenv()).apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                repository.flywayMigrate(dataSource)
                startFritattScedulerKlokken(21, 0, 0, 0, repository, this@apply)
            }
        })

        ArenaFritattKandidatsokLytter(this, repository)
    }.start()
}

val Any.log
    get() = LoggerFactory.getLogger(this::class.java)

fun Instant.atOslo() = this.atZone(ZoneId.of("Europe/Oslo"))

fun LocalDateTime.atOsloSameInstant() = atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Europe/Oslo"))

val arenaTidsformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")