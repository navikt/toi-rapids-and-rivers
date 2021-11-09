package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helse.rapids_rivers.JsonMessage
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val host = env["NAIS_DATABASE_MYAPP_MYDB_HOST"] ?: throw Exception("Hostnavn for database er ikke angitt")
    private val port = env["NAIS_DATABASE_MYAPP_MYDB_PORT"] ?: throw Exception("Portnummer for database er ikke angitt")
    private val database = env["NAIS_DATABASE_MYAPP_MYDB_DATABASE"] ?: throw Exception("Database-navn er ikke angitt")
    private val username =
        env["NAIS_DATABASE_MYAPP_MYDB_USERNAME"] ?: throw Exception("Database-brukernavn er ikke angitt")
    private val password =
        env["NAIS_DATABASE_MYAPP_MYDB_PASSWORD"] ?: throw Exception("Database-passord er ikke angitt")

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

    init {
        kjørFlywayMigreringer()
    }

    fun lagreKandidat(kandidat: Kandidat) {
        dataSource.connection.use {
            it.prepareStatement("insert into sammenstiltkandidat(aktorid, nyeste) VALUES (?,?) ON CONFLICT (aktorid) DO UPDATE SET nyeste = ?")
        }.apply {
            setString(1, kandidat.aktørId)
            setString(2, kandidat.toJson())
            setString(3, kandidat.toJson())
        }.executeUpdate()
    }

    fun hentKandidat(aktørId: String) = dataSource.connection.use {
        it.prepareStatement("select nyeste from sammenstiltkandidat where aktorid = ?")
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


