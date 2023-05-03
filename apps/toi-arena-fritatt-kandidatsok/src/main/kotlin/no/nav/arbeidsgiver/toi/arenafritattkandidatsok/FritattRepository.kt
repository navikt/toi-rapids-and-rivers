package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
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
    val forsoktSendtAktivertFritatt: LocalDateTime?,
    val sendingStatusDektivertFritatt: String,
    val forsoktSendtDektivertFritatt: LocalDateTime?,
    val sistEndret: LocalDateTime,
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
            sendingStatusAktivertFritatt = resultSet.getString("sendingStatusAktivertFritatt"),
            forsoktSendtAktivertFritatt = resultSet.getTimestamp("forsoktSendtAktivertFritatt")?.toLocalDateTime(),
            sendingStatusDektivertFritatt = resultSet.getString("sendingStatusDektivertFritatt"),
            forsoktSendtDektivertFritatt = resultSet.getTimestamp("forsoktSendtDektivertFritatt")?.toLocalDateTime(),
            sistEndret = resultSet.getTimestamp("sistEndret").toLocalDateTime(),
            melding = resultSet.getString("melding")

        )
    }

    fun opprettFritatt(fritatt: Fritatt) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "INSERT INTO fritatt (id, fnr, startdato, sluttdato, sendingStatusAktivertFritatt, forsoktSendtAktivertFritatt, sendingStatusDektivertFritatt, forsoktSendtDektivertFritatt, sistEndret, melding) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            ).apply {
                setInt(1, fritatt.id)
                setString(2, fritatt.fnr)
                setDate(3, Date.valueOf(fritatt.startdato))
                if (fritatt.sluttdato != null) {
                    setDate(4, Date.valueOf(fritatt.sluttdato))
                } else {
                    setNull(4, Types.DATE)
                }
                setString(5, fritatt.sendingStatusAktivertFritatt)
                if (fritatt.forsoktSendtAktivertFritatt != null) {
                    setTimestamp(6, Timestamp.valueOf(fritatt.forsoktSendtAktivertFritatt))
                } else {
                    setNull(6, Types.TIMESTAMP)
                }
                setString(7, fritatt.sendingStatusDektivertFritatt)
                if (fritatt.forsoktSendtDektivertFritatt != null) {
                    setTimestamp(8, Timestamp.valueOf(fritatt.forsoktSendtDektivertFritatt))
                } else {
                    setNull(8, Types.TIMESTAMP)
                }
                setTimestamp(9, Timestamp.valueOf(fritatt.sistEndret))
                setString(10, fritatt.melding)
                executeUpdate()
            }
        }

    }

    fun oppdaterFritatt(fritatt: Fritatt) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE fritatt SET melding = ?, startdato = ?, sluttdato = ?, sendingStatusAktivertFritatt = ?, forsoktSendtAktivertFritatt = ?, sendingStatusDektivertFritatt = ?, forsoktSendtDektivertFritatt = ?, sistEndret = ? WHERE fnr = ?"
            ).apply {
                setString(1, fritatt.melding)
                // ...
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
                "SELECT id, fnr, melding, startdato, sluttdato, sendingStatusAktivertFritatt, forsoktSendtAktivertFritatt, sendingStatusDektivertFritatt, forsoktSendtDektivertFritatt, sistEndret FROM fritatt"
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