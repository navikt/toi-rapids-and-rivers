package no.nav.arbeidsgiver.toi.identmapper

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun hentDatabasekonfigurasjon(env: Map<String, String>): HikariDataSource {
    val host = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_DB_HOST")
    val port = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_DB_PORT")
    val database = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_DB_DATABASE")
    val user = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_DB_USERNAME")
    val pw = env.variable("NAIS_DATABASE_TOI_IDENTMAPPER_DB_PASSWORD")

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