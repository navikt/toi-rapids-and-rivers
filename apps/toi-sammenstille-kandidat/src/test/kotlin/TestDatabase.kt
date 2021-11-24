import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.arbeidsgiver.toi.Kandidat
import no.nav.arbeidsgiver.toi.Repository
import javax.sql.DataSource

class TestDatabase {

    val dataSource: DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
            validate()
        })

    init {
        Repository(dataSource)
        slettAlt()
    }

    fun slettAlt() {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM sammenstiltkandidat"
            ).execute()
        }
    }

    fun hentAntallKandidater() =
        dataSource.connection.use {
            val statement = it.prepareStatement("SELECT count(*) FROM sammenstiltkandidat")
            val resultSet = statement.executeQuery()
            if (resultSet.next()) resultSet.getInt(1)
            else 0
        }
}
