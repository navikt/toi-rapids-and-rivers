import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.arbeidsgiver.toi.FritattKandidatsokTilDatabase
import org.flywaydb.core.Flyway
import java.sql.Timestamp
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
    private val fritattKandidatsøkTabell = "fritattkandidatsok"
    private val fødselsnummerKolonne = "fodselsnummer"
    private val fritattKandidatsøkKolonne = "fritatt_kandidatsok"
    private val sistEndretAvVeileder = "sist_endret_av_veileder"
    private val sistEndretAvSystem = "sist_endret_av_system"
    private val sistEndretTidspunkt = "sist_endret_tidspunkt"


    init {
        kjørFlywayMigreringer()
    }

    fun lagreKandidat(fritattKandidatsokTilDatabase: FritattKandidatsokTilDatabase, skalSlettes: Boolean) {
        if(kandidatFinnes(fritattKandidatsokTilDatabase.fodselsnummer)) {
            if(skalSlettes) {
                slettKandidat(fritattKandidatsokTilDatabase.fodselsnummer)
            } else {
                oppdaterKandidat(fritattKandidatsokTilDatabase)
            }
        } else {
            insertKandiat(fritattKandidatsokTilDatabase)
        }
    }

    fun kandidatFinnes(fødselsnummer: String): Boolean = dataSource.connection.use {
        val statement =
            it.prepareStatement("select 1 from $fritattKandidatsøkTabell where $fødselsnummerKolonne = ?")
        statement.setString(1, fødselsnummer)
        val resultSet = statement.executeQuery()
        return resultSet.next()
    }

    fun slettKandidat(fødselsnummer: String) = dataSource.connection.use {
        val statement =
            it.prepareStatement("DELETE from $fritattKandidatsøkTabell where $fødselsnummerKolonne = ?")
        statement.setString(1, fødselsnummer)
        statement.executeQuery()
    }

    fun insertKandiat(fritattKandidatsokTilDatabase: FritattKandidatsokTilDatabase) = dataSource.connection.use {
        it.prepareStatement("insert into $fritattKandidatsøkTabell(" +
                "$fødselsnummerKolonne, $fritattKandidatsøkKolonne, $sistEndretAvVeileder, $sistEndretAvSystem, $sistEndretTidspunkt) " +
                "VALUES (?,?,?,?,?)")
            .apply {
                setString(1, fritattKandidatsokTilDatabase.fodselsnummer)
                setBoolean(2, fritattKandidatsokTilDatabase.fritattKandidatsøk)
                setString(3, fritattKandidatsokTilDatabase.sistEndretAvVeileder)
                setString(4, fritattKandidatsokTilDatabase.sistEndretAvSystem)
                setTimestamp(5, Timestamp(fritattKandidatsokTilDatabase.sistEndretTidspunkt.toInstant().toEpochMilli()))
            }.executeUpdate()
    }

    fun oppdaterKandidat(fritattKandidatsokTilDatabase: FritattKandidatsokTilDatabase) = dataSource.connection.use {
        it.prepareStatement(
            "UPDATE $fritattKandidatsøkTabell " +
                    "SET $fritattKandidatsøkKolonne = ?, $sistEndretAvVeileder = ?, $sistEndretAvSystem = ?, $sistEndretTidspunkt = ? " +
                    "WHERE $fødselsnummerKolonne = ?")
            .apply {
                setBoolean(1, fritattKandidatsokTilDatabase.fritattKandidatsøk)
                setString(2, fritattKandidatsokTilDatabase.sistEndretAvVeileder)
                setString(3, fritattKandidatsokTilDatabase.sistEndretAvSystem)
                setTimestamp(4, Timestamp(fritattKandidatsokTilDatabase.sistEndretTidspunkt.toInstant().toEpochMilli()))
                setString(5, fritattKandidatsokTilDatabase.fodselsnummer)
            }.executeUpdate()
    }




    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}


private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")
