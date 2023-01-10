import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val applikasjonsnavn = "TOI_FRITATT_KANDIDATSOK"
    private val host = env.variable("NAIS_DATABASE_${applikasjonsnavn}_DB_HOST")
    private val port = env.variable("NAIS_DATABASE_${applikasjonsnavn}_DB_PORT")
    private val database = env.variable("NAIS_DATABASE_${applikasjonsnavn}_DB_DATABASE")
    private val user = env.variable("NAIS_DATABASE_${applikasjonsnavn}_DB_USERNAME")
    private val pw = env.variable("NAIS_DATABASE_${applikasjonsnavn}_DB_PASSWORD")

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

}


private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")
