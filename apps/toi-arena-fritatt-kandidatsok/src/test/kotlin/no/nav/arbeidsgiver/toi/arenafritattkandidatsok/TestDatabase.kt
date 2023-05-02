package no.nav.arbeidsgiver.toi.arenafritattkandidatsok

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.ResultSet
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
        val repository = FritattRepository(dataSource)
        repository.flywayMigrate(dataSource)
        slettAlt()
    }

    private fun slettAlt() {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM fritatt"
            ).execute()
        }
    }

    fun <T> ResultSet.map(mapper: (ResultSet) -> T): List<T> {
        return generateSequence {
            if (this.next()) {
                mapper(this)
            } else {
                null
            }
        }.toList()
    }
}
