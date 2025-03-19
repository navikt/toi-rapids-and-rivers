package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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
        val repository = Repository(dataSource)
        repository.kjørFlywayMigreringer()
        slettAlt()
    }

    fun slettAlt() {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM evaluering"
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
