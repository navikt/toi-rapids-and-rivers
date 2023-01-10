import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource


class TestDatabase {
    val dataSource: DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
            username = "sa"
            password = ""
            validate()
        })



}
