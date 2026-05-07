package no.nav.arbeidsgiver.toi.identmapper

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

class IdentRepository(private val dataSource: DataSource) {
    private val tabell = "identmapping"
    private val aktørIdKolonne = "aktor_id"
    private val fødselsnummerKolonne = "fnr"
    private val cachetTidspunktKolonne = "cachet_tidspunkt"

    fun lagreAktørId(aktørId: String?, fødselsnummer: String) {
        lagreIdentMapping(aktørId = aktørId, fødselsnummer = fødselsnummer)
    }

    fun lagreFødselsnummer(aktørId: String, fødselsnummer: String?) {
        if (fødselsnummer == null) return
        lagreIdentMapping(aktørId = aktørId, fødselsnummer = fødselsnummer)
    }

    fun hentIdentMappingerForFnr(fødselsnummer: String): List<IdentMapping> {
        return hentIdentMappinger(fødselsnummerKolonne, fødselsnummer)
    }

    fun hentIdentMappingerForAktørId(aktørId: String): List<IdentMapping> {
        return hentIdentMappinger(aktørIdKolonne, aktørId)
    }

    private fun hentIdentMappinger(kolonne: String, verdi: String): List<IdentMapping> {
        dataSource.connection.use {
            val resultSet = it.prepareStatement("SELECT * FROM $tabell WHERE $kolonne = ?").apply {
                setString(1, verdi)
            }.executeQuery()

            return generateSequence {
                if (resultSet.next()) tilIdentMapping(resultSet) else null
            }.toList()
        }
    }

    private fun lagreIdentMapping(aktørId: String?, fødselsnummer: String) {
        val identMappingerBasertPåFødselsnummer = hentIdentMappingerForFnr(fødselsnummer)
        val harSammeMapping = identMappingerBasertPåFødselsnummer.any { it.aktørId == aktørId }

        dataSource.connection.use {
            if (harSammeMapping) {
                it.prepareStatement(
                    "UPDATE $tabell SET $cachetTidspunktKolonne = ? " +
                            "WHERE $fødselsnummerKolonne = ? " +
                            "AND $aktørIdKolonne ${if (aktørId != null) "= ?" else "is NULL"}"
                ).apply {
                    setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                    setString(2, fødselsnummer)
                    if (aktørId != null) setString(3, aktørId)
                }
            } else {
                it.prepareStatement("INSERT INTO $tabell($aktørIdKolonne, $fødselsnummerKolonne, $cachetTidspunktKolonne) VALUES (?, ?, ?)")
                    .apply {
                        setString(1, aktørId)
                        setString(2, fødselsnummer)
                        setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                    }
            }.executeUpdate()
        }
    }


    private fun tilIdentMapping(resultSet: ResultSet) = IdentMapping(
        aktørId = resultSet.getString(aktørIdKolonne),
        fødselsnummer = resultSet.getString(fødselsnummerKolonne),
        cachetTidspunkt = resultSet.getTimestamp(cachetTidspunktKolonne).toLocalDateTime()
    )

    fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

data class IdentMapping(
    val aktørId: String?,
    val fødselsnummer: String,
    val cachetTidspunkt: LocalDateTime
)

fun hentDatabasekonfigurasjon(env: Map<String, String>): HikariDataSource {
    val host = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_IDENTMAPPING_DB_HOST")
    val port = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_IDENTMAPPING_DB_PORT")
    val database = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_IDENTMAPPING_DB_DATABASE")
    val user = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_IDENTMAPPING_DB_USERNAME")
    val pw = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_IDENTMAPPING_DB_PASSWORD")

    return HikariConfig().apply {
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

private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")
