import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.arbeidsgiver.toi.FritattKandidatsokTilDatabase
import org.flywaydb.core.Flyway
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
    private val fritattKandidatsokKolonne = "fritatt_kandidatsok"
    private val sistEndretAvVeileder = "sist_endret_av_veileder"
    private val sistEndretAvSystem = "sist_endret_av_system"
    private val sistEndretAvTidspunkt = "sist_endret_tidspunkt"


    init {
        kjørFlywayMigreringer()
    }

    fun lagreKandidat(fritattKandidatsokTilDatabase: FritattKandidatsokTilDatabase, skalSlettes: Boolean) {


        /*val fritattInnslagFinnesForPersonen = hentKandidat(kandidat.aktørId) != null


        dataSource.connection.use {
            if (kandidatFinnes) {
                it.prepareStatement("UPDATE $sammenstiltkandidatTabell SET $kandidatKolonne = ? WHERE $aktørIdKolonne = ?")
                    .apply {
                        setString(1, kandidat.toJson())
                        setString(2, kandidat.aktørId)
                    }
            } else {
                it.prepareStatement("insert into $sammenstiltkandidatTabell($aktørIdKolonne, $kandidatKolonne) VALUES (?,?)")
                    .apply {
                        setString(1, kandidat.aktørId)
                        setString(2, kandidat.toJson())
                    }
            }.executeUpdate()
        }*/
    }

    fun kandidatFinnes(fødselsnummer: String): Boolean = dataSource.connection.use {
        val statement =
            it.prepareStatement("select 1 from $fritattKandidatsøkTabell where $fødselsnummerKolonne = ?")
        statement.setString(1, fødselsnummer)
        val resultSet = statement.executeQuery()
        return resultSet.next()
    }

    //fun slettKandidat




    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}


private fun Map<String, String>.variable(felt: String) = this[felt] ?: throw Exception("$felt er ikke angitt")
