package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun lagDatasource(env: Map<String, String>): HikariDataSource {
    val host = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_HOST")
    val port = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PORT")
    val database = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_DATABASE")
    val user = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_USERNAME")
    val pw = env.variable("NAIS_DATABASE_TOI_SAMMENSTILLE_KANDIDAT_DB_PASSWORD")

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

fun kjørFlyway(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}