package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.JsonMessage
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val host = env.variable("NAIS_DATABASE_MYAPP_MYDB_HOST")
    private val port = env.variable("NAIS_DATABASE_MYAPP_MYDB_PORT")
    private val database = env.variable("NAIS_DATABASE_MYAPP_MYDB_DATABASE")
    private val username = env.variable("NAIS_DATABASE_MYAPP_MYDB_USERNAME")
    private val password = env.variable("NAIS_DATABASE_MYAPP_MYDB_PASSWORD")

    fun lagDatasource() = HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$database"
        minimumIdle = 1
        maximumPoolSize = 2
        driverClassName = "org.postgresql.Driver"
        username = this@DatabaseKonfigurasjon.username
        password = this@DatabaseKonfigurasjon.password
    }.let(::HikariDataSource)
}

class Repository(private val dataSource: DataSource) {

    private val aktørIdKolonne = "aktor_id"
    private val kandidatKolonne = "kandidat"

    init {
        kjørFlywayMigreringer()
    }

    fun lagreKandidat(kandidat: Kandidat) {
        dataSource.connection.use {
            it.prepareStatement("""
                insert into sammenstiltkandidat($aktørIdKolonne, $kandidatKolonne) 
                VALUES (?,?) 
                ON CONFLICT (aktorid) DO UPDATE SET $kandidatKolonne = ?""".trimIndent())
        }.apply {
            setString(1, kandidat.aktørId)
            setString(2, kandidat.toJson())
            setString(3, kandidat.toJson())
        }.executeUpdate()
    }

    fun hentKandidat(aktørId: String) = dataSource.connection.use {
        it.prepareStatement("select $kandidatKolonne from sammenstiltkandidat where aktorid = ?")
    }.apply {
        setString(1, aktørId)
        execute()
    }.executeQuery().let { resultSet ->
        if (resultSet.next()) Kandidat.fraJson(resultSet.getString(1)) else null
    }

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

typealias AktøridHendelse = Pair<String, JsonMessage>

private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")


