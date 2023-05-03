package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName


val lokalPostgres: PostgreSQLContainer<*>
    get() {
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:14.4-alpine"))
            .withDatabaseName("dbname")
            .withUsername("username")
            .withPassword("pwd")

        postgres.start()
        return postgres
    }

val dataSource = HikariConfig().apply {
    jdbcUrl = lokalPostgres.jdbcUrl
    minimumIdle = 1
    maximumPoolSize = 10
    driverClassName = "org.postgresql.Driver"
    initializationFailTimeout = 5000
    username = lokalPostgres.username
    password = lokalPostgres.password
    validate()
}.let(::HikariDataSource)

fun kandidatlisteRepositoryMedLokalPostgres(): FritattRepository {
    try {
        slettAllDataIDatabase()
    } catch (e: Exception) {
        println("Trenger ikke slette fordi db-skjema ikke opprettet ennå")
    }
    return FritattRepository(dataSource).apply { flywayMigrate(dataSource) }
}

fun slettAllDataIDatabase() {
    val connection = dataSource.connection

    connection.use {
        it.prepareStatement("delete from kandidat").execute()
        it.prepareStatement("delete from kandidatliste").execute()
        it.prepareStatement("delete from samtykke").execute()
        it.prepareStatement("delete from visning_kontaktinfo").execute()
    }
}





