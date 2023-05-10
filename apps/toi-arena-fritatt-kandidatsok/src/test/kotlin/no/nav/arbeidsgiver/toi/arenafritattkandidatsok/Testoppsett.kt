package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.ResultSet


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

fun slettStatusTabell() {
    try {
        val connection = dataSource.connection

        connection.use {
            it.prepareStatement("drop table sendingstatus").execute()
        }
    } catch (e: Exception) {
        println("Trenger ikke slette fordi db-skjema ikke opprettet ennå")
    }
}


fun slettAllDataIDatabase() {
    val connection = dataSource.connection

    connection.use {
        it.prepareStatement("delete from fritatt").execute()
    }
}

fun hentAlle(): List<Fritatt> = dataSource.connection.use { connection ->
    fun ResultSet.toFritatt() = Fritatt.fraDatabase(
        id = getInt("db_id"),
        fnr = getString("fnr"),
        startdato = getDate("startdato").toLocalDate(),
        sluttdato = getDate("sluttdato")?.toLocalDate(),
        sistEndretIArena = getTimestamp("sistendret_i_arena").toInstant().atOslo(),
        slettetIArena = getBoolean("slettet_i_arena"),
        opprettetRad = getTimestamp("opprettet_rad").toInstant().atOslo(),
        sistEndretRad = getTimestamp("sist_endret_rad").toInstant().atOslo(),
        meldingFraArena = getString("melding_fra_arena")
    )

    connection.prepareStatement("SELECT * FROM fritatt").executeQuery().let { resultSet ->
        generateSequence { if (resultSet.next()) resultSet.toFritatt() else null }.toList()
    }


}





