package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val host = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_DB_HOST"]
    private val port = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_DB_PORT"]
    private val database = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_DB_DATABASE"]
    private val user = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_DB_USERNAME"]
    private val pw = env["NAIS_DATABASE_ARENA_FRITATT_KANDIDATSOK_DB_PASSWORD"]

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
    val sendingStatusAktivertFritatt: String,
    val forsoktSendtAktivertFritatt: ZonedDateTime?,
    val sendingStatusDektivertFritatt: String,
    val forsoktSendtDektivertFritatt: ZonedDateTime?,
    val sistEndret: ZonedDateTime,
    val melding: String,

    )

class FritattRepository(private val dataSource: DataSource) {

    fun slettFritatt(fodselsnummer: String) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM fritatt WHERE fnr = ?"
            ).apply {
                setString(1, fodselsnummer)
                executeUpdate()
            }
        }
    }

    fun hentFritatt(fnr: String): Fritatt? {
        dataSource.connection.use { connection ->
            val resultSet = connection.prepareStatement(
                "SELECT * FROM fritatt WHERE fnr = ?"
            ).apply {
                setString(1, fnr)
            }.executeQuery()

            return if (resultSet.next()) {
                mapResultSetToFritatt(resultSet)
            } else {
                null
            }
        }
    }

    fun mapResultSetToFritatt(resultSet: ResultSet): Fritatt {
        return Fritatt(
            id = resultSet.getInt("id"),
            fnr = resultSet.getString("fnr"),
            startdato = resultSet.getDate("startdato").toLocalDate(),
            sluttdato = resultSet.getDate("sluttdato")?.toLocalDate(),
            sendingStatusAktivertFritatt = resultSet.getString("sendingstatus_aktivert_fritatt"),
            forsoktSendtAktivertFritatt = resultSet.getTimestamp("forsoktsendt_aktivert_fritatt")?.toInstant()
                ?.atZone(ZoneId.of("Europe/Oslo")),
            sendingStatusDektivertFritatt = resultSet.getString("sendingstatus_dektivert_fritatt"),
            forsoktSendtDektivertFritatt = resultSet.getTimestamp("forsoktsendt_dektivert_fritatt")?.toInstant()
                ?.atZone(ZoneId.of("Europe/Oslo")),
            sistEndret = resultSet.getTimestamp("sistendret").toInstant().atOslo(),
            melding = resultSet.getString("melding")
        )
    }

    fun opprettFritatt(fritatt: Fritatt) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO fritatt (fnr, startdato, sluttdato, sendingstatus_aktivert_fritatt, forsoktsendt_aktivert_fritatt, sendingstatus_dektivert_fritatt, forsoktsendt_dektivert_fritatt, sistendret, melding) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"

            ).apply {
                setString(1, fritatt.fnr)
                setDate(2, Date.valueOf(fritatt.startdato))
                if (fritatt.sluttdato != null) {
                    setDate(3, Date.valueOf(fritatt.sluttdato))
                } else {
                    setNull(3, Types.DATE)
                }
                setString(4, fritatt.sendingStatusAktivertFritatt)
                if (fritatt.forsoktSendtAktivertFritatt != null) {

                    setTimestamp(5, Timestamp(fritatt.forsoktSendtAktivertFritatt.toInstant().toEpochMilli()))
                } else {
                    setNull(5, Types.TIMESTAMP)
                }
                setString(6, fritatt.sendingStatusDektivertFritatt)
                if (fritatt.forsoktSendtDektivertFritatt != null) {
                    setTimestamp(7, Timestamp(fritatt.forsoktSendtDektivertFritatt.toInstant().toEpochMilli()))
                } else {
                    setNull(7, Types.TIMESTAMP)
                }
                setTimestamp(8, Timestamp(fritatt.sistEndret.toInstant().toEpochMilli()))
                setString(9, fritatt.melding)
                executeUpdate()
            }
        }

    }

    fun oppdaterFritatt(fritatt: Fritatt) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE fritatt SET startdato = ?, sluttdato = ?, sendingstatus_aktivert_fritatt = ?, forsoktsendt_aktivert_fritatt = ?, sendingstatus_dektivert_fritatt = ?, forsoktsendt_dektivert_fritatt = ?, sistendret = ?, melding = ? WHERE fnr = ?"
            ).apply {
                setDate(1, Date.valueOf(fritatt.startdato))
                if (fritatt.sluttdato != null) {
                    setDate(2, Date.valueOf(fritatt.sluttdato))
                } else {
                    setNull(2, Types.DATE)
                }
                setString(3, fritatt.sendingStatusAktivertFritatt)
                if (fritatt.forsoktSendtAktivertFritatt != null) {

                    setTimestamp(4, Timestamp(fritatt.forsoktSendtAktivertFritatt.toInstant().toEpochMilli()))
                } else {
                    setNull(4, Types.TIMESTAMP)
                }
                setString(5, fritatt.sendingStatusDektivertFritatt)
                if (fritatt.forsoktSendtDektivertFritatt != null) {
                    setTimestamp(6, Timestamp(fritatt.forsoktSendtDektivertFritatt.toInstant().toEpochMilli()))
                } else {
                    setNull(6, Types.TIMESTAMP)
                }
                setTimestamp(7, Timestamp(fritatt.sistEndret.toInstant().toEpochMilli()))
                setString(8, fritatt.melding)
                setString(9, fritatt.fnr)
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
                "SELECT id, fnr, melding, startdato, sluttdato, sendingstatus_aktivert_fritatt, forsoktsendt_aktivert_fritatt, sendingstatus_dektivert_fritatt, forsoktsendt_dektivert_fritatt, sistendret FROM fritatt"
            )
            val resultSet = statement.executeQuery()

            val fritatte = mutableListOf<Fritatt>()

            while (resultSet.next()) {
                fritatte.add(mapResultSetToFritatt(resultSet))
            }

            return fritatte
        }
    }

    fun flywayMigrate(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}