package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.JsonMessage
import org.flywaydb.core.Flyway
import java.sql.Date
import java.sql.ResultSet
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

    fun tilJsonMelding(erFritatt: Boolean) =
        JsonMessage.newMessage("fritattFraArena", mapOf("fnr" to fnr, "erFritatt" to erFritatt)).toJson()

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

    fun markerSomSendt(fritatt: Fritatt, status: Status) = dataSource.connection.use { connection ->
        connection.prepareStatement(
            """
        INSERT INTO sendingstatus (fnr, status, opprettet_rad)
        VALUES (?, ?, ?)
        """.trimIndent()
        ).apply {
            setString(1, fritatt.fnr)
            setString(2, status.name)
            setTimestamp(3, Timestamp(ZonedDateTime.now().toInstant().toEpochMilli()))
            executeUpdate()
        }
    }

    fun hentAlleSomIkkeErFritatt(): List<Fritatt> =
        dataSource.connection.use { connection ->
            val rs = connection.prepareStatement(
                """
                WITH cte AS (
                    SELECT 
                        fritatt.*,
                        sendingstatus.fnr AS sendingstatus_fnr,
                        CASE 
                            WHEN fritatt.slettet_i_arena = true THEN 'slettet'
                            WHEN fritatt.startdato > ? THEN 'FOER_FRITATT_PERIODE'
                            WHEN fritatt.sluttdato < ? THEN 'ETTER_FRITATT_PERIODE'
                            ELSE 'UKJENT_STATUS'
                        END AS ekstra_status
                    FROM 
                        fritatt 
                    LEFT JOIN 
                        sendingstatus 
                    ON 
                        fritatt.fnr = sendingstatus.fnr
                )
                SELECT *
                FROM cte
                WHERE 
                    sendingstatus_fnr IS NULL 
                    AND (
                        ekstra_status = 'slettet' 
                        OR ekstra_status = 'FOER_FRITATT_PERIODE' 
                        OR ekstra_status = 'ETTER_FRITATT_PERIODE'
                    )

            """.trimIndent()
            ).apply {
                val nå = Timestamp(ZonedDateTime.now().toInstant().toEpochMilli())
                setTimestamp(1, nå)
                setTimestamp(2, nå)
            }.executeQuery()

            return generateSequence {
                if (rs.next()) fraDatabase(rs) else null
            }.toList()
        }

    fun hentAlleSomIkkeHarSendtPeriodeStartetMenErIPeriode(): List<Fritatt> =
        dataSource.connection.use { connection ->
            val rs = connection.prepareStatement(
                """
                    select * from fritatt 
                    left join sendingstatus on fritatt.fnr = sendingstatus.fnr
                    where (sendingstatus.fnr is null or sendingstatus.status <> 'I_FRITATT_PERIODE')
                    and fritatt.slettet_i_arena = false AND fritatt.startdato <= ? and (fritatt.sluttdato >= ? or fritatt.sluttdato is null)
            """.trimIndent()
            ).apply {
                val nå = Timestamp(ZonedDateTime.now().toInstant().toEpochMilli())
                setTimestamp(1, nå)
                setTimestamp(2, nå)
            }.executeQuery()

            return generateSequence {
                if (rs.next()) fraDatabase(rs) else null
            }.toList()
        }

    private fun fraDatabase(rs: ResultSet) =
        Fritatt.fraDatabase(
            id = rs.getInt("db_id"),
            fnr = rs.getString("fnr"),
            startdato = rs.getDate("startdato").toLocalDate(),
            sluttdato = rs.getDate("sluttdato")?.toLocalDate(),
            sistEndretIArena = rs.getTimestamp("sistendret_i_arena").toInstant().atOslo(),
            meldingFraArena = rs.getString("melding_fra_arena"),
            slettetIArena = rs.getBoolean("slettet_i_arena"),
            opprettetRad = rs.getTimestamp("opprettet_rad").toInstant().atOslo(),
            sistEndretRad = rs.getTimestamp("sist_endret_rad").toInstant().atOslo()
        ) to
}


enum class Status {
    FOER_FRITATT_PERIODE, I_FRITATT_PERIODE, ETTER_FRITATT_PERIODE, SLETTET

}