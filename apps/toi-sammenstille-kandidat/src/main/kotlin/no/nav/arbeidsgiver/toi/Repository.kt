package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.JsonMessage
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val host = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_HOST")
    private val port = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PORT")
    private val database = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_DATABASE")
    private val user = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_USERNAME")
    private val pw = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PASSWORD")


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

class Repository(private val dataSource: DataSource) {

    private val aktørIdKolonne = "aktor_id"
    private val kandidatKolonne = "kandidat"
    private val sammenstiltkandidatTabell = "sammenstiltkandidat"

    init {
        kjørFlywayMigreringer()
    }

    fun lagreKandidat(kandidat: Kandidat) {
        val kandidatFinnes = hentKandidat(kandidat.aktørId) != null

        dataSource.connection.use {
            if (kandidatFinnes) {
                it.prepareStatement("UPDATE $sammenstiltkandidatTabell SET $kandidatKolonne = ? WHERE $aktørIdKolonne = ?")
                    .apply {
                        setString(1, kandidat.toJson())
                        setString(2, kandidat.aktørId)
                    }
            } else {
                it.prepareStatement("insert into $sammenstiltkandidatTabell($aktørIdKolonne, $kandidatKolonne) VALUES (?,?)")
                    .apply {
                        setString(1, kandidat.aktørId)
                        setString(2, kandidat.toJson())
                    }
            }.executeUpdate()
        }
    }

    fun hentKandidat(aktørId: String) = dataSource.connection.use {
        val statement =
            it.prepareStatement("select $kandidatKolonne from $sammenstiltkandidatTabell where $aktørIdKolonne = ?")
        statement.setString(1, aktørId)
        val resultSet = statement.executeQuery()
        if (resultSet.next())
            Kandidat.fraJson(resultSet.getString(1))
        else null

    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

data class Kandidat(
    val aktørId: String,
    val cv: String? = null,
    val veileder: String? = null
) {
    companion object {
        private val objectMapper = jacksonObjectMapper()
        fun fraJson(json:String) = objectMapper.readTree(json).let {
            Kandidat(it["aktørId"].asText(), it["cv"].toString(),it["veileder"].toString())
        }
    }
    fun toJson(): String = """ {
        "aktørId":$aktørId,
        "cv":$cv,
        "veileder":$veileder
        }
    """.trimIndent()
}

typealias AktøridHendelse = Pair<String, JsonMessage>

private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")


