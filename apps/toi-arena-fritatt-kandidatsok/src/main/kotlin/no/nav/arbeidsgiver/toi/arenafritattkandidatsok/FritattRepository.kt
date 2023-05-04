package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
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

    fun hentFritatt(fnr: String): Fritatt? {
        dataSource.connection.use { connection ->
            val resultSet = connection.prepareStatement(
                "SELECT * FROM fritatt WHERE fnr = ?"
            ).apply {
                setString(1, fnr)
            }.executeQuery()

            return if (resultSet.next()) {
                resultSet.toFritatt()
            } else {
                null
            }
        }
    }

    fun opprettFritatt(fritatt: Fritatt) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO fritatt (fnr, startdato, sluttdato, sendingstatus_aktivert, forsoktsendt_aktivert, sendingstatus_deaktivert, forsoktsendt_deaktivert, sistendret_i_arena, slettet_i_arena, opprettet_rad, sist_endret_rad, melding_fra_arena)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).apply {
                setString(1, fritatt.fnr)
                setDate(2, Date.valueOf(fritatt.startdato))
                if (fritatt.sluttdato != null) {
                    setDate(3, Date.valueOf(fritatt.sluttdato))
                } else {
                    setNull(3, Types.DATE)
                }
                setString(4, fritatt.sendingStatusAktivert)
                fritatt.forsoktSendtAktivert?.let {
                    setTimestamp(5, Timestamp(it.toInstant().toEpochMilli()))
                } ?: setNull(5, Types.TIMESTAMP)

                setString(6, fritatt.sendingStatusDeaktivert)
                fritatt.forsoktSendtDeaktivert?.let {
                    setTimestamp(7, Timestamp(it.toInstant().toEpochMilli()))
                } ?: setNull(7, Types.TIMESTAMP)

                setTimestamp(8, Timestamp(fritatt.sistEndretIArena.toInstant().toEpochMilli()))
                setBoolean(9, fritatt.slettetIArena)
                setTimestamp(10, Timestamp(fritatt.opprettetRad.toInstant().toEpochMilli()))
                setTimestamp(11, Timestamp(fritatt.sistEndretRad.toInstant().toEpochMilli()))
                setString(12, fritatt.meldingFraArena)
                executeUpdate()
            }
        }
    }


    fun oppdaterFritatt(fritatt: Fritatt) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE fritatt
                SET startdato = ?, sluttdato = ?, sendingstatus_aktivert = ?, forsoktsendt_aktivert = ?, sendingstatus_deaktivert = ?, forsoktsendt_deaktivert = ?, sistendret_i_arena = ?, slettet_i_arena = ?, opprettet_rad = ?, sist_endret_rad = ?, melding_fra_arena = ?
                WHERE fnr = ?
                """.trimIndent()
            ).apply {
                setDate(1, Date.valueOf(fritatt.startdato))
                if (fritatt.sluttdato != null) {
                    setDate(2, Date.valueOf(fritatt.sluttdato))
                } else {
                    setNull(2, Types.DATE)
                }
                setString(3, fritatt.sendingStatusAktivert)
                fritatt.forsoktSendtAktivert?.let {
                    setTimestamp(4, Timestamp(it.toInstant().toEpochMilli()))
                } ?: setNull(4, Types.TIMESTAMP)

                setString(5, fritatt.sendingStatusDeaktivert)
                fritatt.forsoktSendtDeaktivert?.let {
                    setTimestamp(6, Timestamp(it.toInstant().toEpochMilli()))
                } ?: setNull(6, Types.TIMESTAMP)
                setTimestamp(7, Timestamp(fritatt.sistEndretIArena.toInstant().toEpochMilli()))
                setBoolean(8, fritatt.slettetIArena)
                setTimestamp(9, Timestamp(fritatt.opprettetRad.toInstant().toEpochMilli()))
                setTimestamp(10, Timestamp(fritatt.sistEndretRad.toInstant().toEpochMilli()))
                setString(11, fritatt.meldingFraArena)
                setString(12, fritatt.fnr)
                executeUpdate()
            }

        }
    }

    fun upsertFritatt(fritatt: Fritatt) {
        val eksisterendeFritatt = hentFritatt(fritatt.fnr)

        if (eksisterendeFritatt == null) {
            opprettFritatt(fritatt)
        } else {
            oppdaterFritatt(fritatt)
        }
    }

    fun hentAlle(): List<Fritatt> {
        dataSource.connection.use { connection ->
            val statement = connection.prepareStatement(
                "SELECT * FROM fritatt"
            )
            val resultSet = statement.executeQuery()

            return generateSequence {
                if (resultSet.next()) {
                    resultSet.toFritatt()
                } else {
                    null
                }
            }.toList()
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
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}