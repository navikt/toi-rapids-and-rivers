package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {

    private val host = env["NAIS_DATABASE_TOI_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_HOST"]
    private val port = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_PORT"]
    private val database = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_DATABASE"]
    private val user = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_USERNAME"]
    private val pw = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_PASSWORD"]

    fun lagDatasource() = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$database"
        minimumIdle = 1
        maximumPoolSize = 2
        driverClassName = "org.postgresql.Driver"
        initializationFailTimeout = 5000
        username = user
        password = pw
        validate()
    }.let(::HikariDataSource)
}

data class Fritatt(
    val id: Int,
    val fnr: String,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val sendingStatusAktivert: String,
    val forsoktSendtAktivert: ZonedDateTime?,
    val sendingStatusDeaktivert: String,
    val forsoktSendtDeaktivert: ZonedDateTime?,
    val sistEndretIArena: ZonedDateTime,
    val slettetIArena: Boolean,
    val opprettetRad: ZonedDateTime,
    val sistEndretRad: ZonedDateTime,
    val meldingFraArena: String,
)

class FritattRepository(private val dataSource: DataSource) {

    fun upsertFritatt(fritatt: Fritatt) = dataSource.connection.use { connection ->
        connection.prepareStatement(
            """
        INSERT INTO fritatt (fnr, startdato, sluttdato, sendingstatus_aktivert, forsoktsendt_aktivert, sendingstatus_deaktivert, forsoktsendt_deaktivert, sistendret_i_arena, slettet_i_arena, opprettet_rad, sist_endret_rad, melding_fra_arena)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (fnr) 
        DO UPDATE SET startdato = excluded.startdato, sluttdato = excluded.sluttdato, sendingstatus_aktivert = excluded.sendingstatus_aktivert, forsoktsendt_aktivert = excluded.forsoktsendt_aktivert, sendingstatus_deaktivert = excluded.sendingstatus_deaktivert, forsoktsendt_deaktivert = excluded.forsoktsendt_deaktivert, sistendret_i_arena = excluded.sistendret_i_arena, slettet_i_arena = excluded.slettet_i_arena, opprettet_rad = excluded.opprettet_rad, sist_endret_rad = excluded.sist_endret_rad, melding_fra_arena = excluded.melding_fra_arena
        """.trimIndent()
        ).apply {
            setString(1, fritatt.fnr)
            setDate(2, Date.valueOf(fritatt.startdato))
            setDate(3, fritatt.sluttdato?.let { Date.valueOf(it) })
            setString(4, fritatt.sendingStatusAktivert)
            setTimestamp(5, fritatt.forsoktSendtAktivert?.let { Timestamp(it.toInstant().toEpochMilli()) })
            setString(6, fritatt.sendingStatusDeaktivert)
            setTimestamp(7, fritatt.forsoktSendtDeaktivert?.let { Timestamp(it.toInstant().toEpochMilli()) })
            setTimestamp(8, Timestamp(fritatt.sistEndretIArena.toInstant().toEpochMilli()))
            setBoolean(9, fritatt.slettetIArena)
            setTimestamp(10, Timestamp(fritatt.opprettetRad.toInstant().toEpochMilli()))
            setTimestamp(11, Timestamp(fritatt.sistEndretRad.toInstant().toEpochMilli()))
            setString(12, fritatt.meldingFraArena)
            executeUpdate()
        }
    }


    fun hentAlle(): List<Fritatt> = dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT * FROM fritatt").executeQuery().let { resultSet ->
            generateSequence { if (resultSet.next()) resultSet.toFritatt() else null }.toList()
        }
    }

    private fun ResultSet.toFritatt() = Fritatt(
        id = getInt("db_id"),
        fnr = getString("fnr"),
        startdato = getDate("startdato").toLocalDate(),
        sluttdato = getDate("sluttdato")?.toLocalDate(),
        sendingStatusAktivert = getString("sendingstatus_aktivert"),
        forsoktSendtAktivert = getTimestamp("forsoktsendt_aktivert")?.toInstant()?.atOslo(),
        sendingStatusDeaktivert = getString("sendingstatus_deaktivert"),
        forsoktSendtDeaktivert = getTimestamp("forsoktsendt_deaktivert")?.toInstant()?.atOslo(),
        sistEndretIArena = getTimestamp("sistendret_i_arena").toInstant().atOslo(),
        slettetIArena = getBoolean("slettet_i_arena"),
        opprettetRad = getTimestamp("opprettet_rad").toInstant().atOslo(),
        sistEndretRad = getTimestamp("sist_endret_rad").toInstant().atOslo(),
        meldingFraArena = getString("melding_fra_arena")
    )

    fun flywayMigrate(dataSource: DataSource) {
        Flyway.configure().dataSource(dataSource).load().migrate()
    }
}