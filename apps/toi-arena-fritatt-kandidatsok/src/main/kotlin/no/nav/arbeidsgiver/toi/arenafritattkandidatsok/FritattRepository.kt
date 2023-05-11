package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {

    private val host = env["NAIS_DATABASE_TOI_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_HOST"]
    private val port = env["NAIS_DATABASE_TOI_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_PORT"]
    private val database = env["NAIS_DATABASE_TOI_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_DATABASE"]
    private val user = env["NAIS_DATABASE_TOI_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_USERNAME"]
    private val pw = env["NAIS_DATABASE_TOI_ARENA_FRITATT_KANDIDATSOK_FRKAS_DB_PASSWORD"]

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

class Fritatt private constructor(
    val id: Int?,
    val fnr: String,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val sistEndretIArena: ZonedDateTime,
    val slettetIArena: Boolean,
    val meldingFraArena: String,
    val opprettetRad: ZonedDateTime,
    val sistEndretRad: ZonedDateTime,
) {
    companion object {
        fun ny(
            fnr: String,
            startdato: LocalDate,
            sluttdato: LocalDate?,
            sistEndretIArena: ZonedDateTime,
            slettetIArena: Boolean,
            meldingFraArena: String,
        ) = Fritatt(
            null,
            fnr,
            startdato,
            sluttdato,
            sistEndretIArena,
            slettetIArena,
            meldingFraArena,
            ZonedDateTime.now(),
            ZonedDateTime.now()
        )

        fun fraDatabase(
            id: Int,
            fnr: String,
            startdato: LocalDate,
            sluttdato: LocalDate?,
            sistEndretIArena: ZonedDateTime,
            slettetIArena: Boolean,
            meldingFraArena: String,
            opprettetRad: ZonedDateTime,
            sistEndretRad: ZonedDateTime,
        ) = Fritatt(
            id,
            fnr,
            startdato,
            sluttdato,
            sistEndretIArena,
            slettetIArena,
            meldingFraArena,
            opprettetRad,
            sistEndretRad
        )
    }
}

class FritattRepository(private val dataSource: DataSource) {

    fun upsertFritatt(fritatt: Fritatt) = dataSource.connection.use { connection ->

        connection.autoCommit = false
        try {
            connection.prepareStatement(
                """
        INSERT INTO fritatt (fnr, startdato, sluttdato, sistendret_i_arena, slettet_i_arena, opprettet_rad, sist_endret_rad, melding_fra_arena)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (fnr) 
        DO UPDATE SET startdato = excluded.startdato, sluttdato = excluded.sluttdato, sistendret_i_arena = excluded.sistendret_i_arena, slettet_i_arena = excluded.slettet_i_arena, sist_endret_rad = excluded.sist_endret_rad, melding_fra_arena = excluded.melding_fra_arena
        """.trimIndent()
            ).apply {
                setString(1, fritatt.fnr)
                setDate(2, Date.valueOf(fritatt.startdato))
                setDate(3, fritatt.sluttdato?.let { Date.valueOf(it) })
                setTimestamp(4, Timestamp(fritatt.sistEndretIArena.toInstant().toEpochMilli()))
                setBoolean(5, fritatt.slettetIArena)
                setTimestamp(6, Timestamp(fritatt.opprettetRad.toInstant().toEpochMilli()))
                setTimestamp(7, Timestamp(fritatt.sistEndretRad.toInstant().toEpochMilli()))
                setString(8, fritatt.meldingFraArena)
                executeUpdate()
            }
            connection.prepareStatement(
                """
        DELETE FROM sendingstatus
        WHERE fnr = ?
        """.trimIndent()
            ).apply {
                setString(1, fritatt.fnr)
                executeUpdate()
            }
        } catch (e: Exception) {
            connection.rollback()
            throw e
        }
        connection.commit()
    }

    fun flywayMigrate(dataSource: DataSource) {
        Flyway.configure().dataSource(dataSource).load().migrate()
    }

    fun markerSomSendt(fritatt: Fritatt, foerFritattPeriode: Status) {
        TODO("Not yet implemented")
    }
}

enum class Status {
    FOER_FRITATT_PERIODE, I_FRITATT_PERIODE, ETTER_FRITATT_PERIODE, SLETTET
}