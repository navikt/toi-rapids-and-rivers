package no.nav.arbeidsgiver.toi.identmapper

import org.flywaydb.core.Flyway
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

class Repository(private val dataSource: DataSource) {
    init {
        kjørFlywayMigreringer()
    }

    private val tabell = "identmapping"
    private val aktørIdKolonne = "aktor_id"
    private val fødselsnummerKolonne = "fnr"
    private val cachetTidspunktKolonne = "cachet_tidspunkt"

    fun lagreAktørId(aktørId: String, fødselsnummer: String) {
        val identMappingerBasertPåFødselsnummer = hentIdentMappinger(fødselsnummer)

        val harSammeMapping = identMappingerBasertPåFødselsnummer.any { it.aktørId == aktørId }

        dataSource.connection.use {
            if (harSammeMapping) {
                it.prepareStatement("UPDATE $tabell SET $cachetTidspunktKolonne = ? WHERE $aktørIdKolonne = ? $fødselsnummerKolonne = ?").apply {
                    setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                    setString(2, aktørId)
                    setString(3, fødselsnummer)
                }
            } else {
                it.prepareStatement("INSERT INTO $tabell($aktørIdKolonne, $fødselsnummerKolonne, $cachetTidspunktKolonne) VALUES (?, ?, ?)")
                    .apply {
                        setString(1, aktørId)
                        setString(2, fødselsnummer)
                        setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                    }
            }.executeUpdate()
        }
    }

    fun hentIdentMappinger(fødselsnummer: String): List<IdentMapping> {
        return dataSource.connection.use {
            val resultSet = it.prepareStatement("SELECT * FROM $tabell WHERE $fødselsnummerKolonne = ?").apply {
                setString(1, fødselsnummer)
            }.executeQuery()

            generateSequence {
                if (resultSet.next()) resultSet.tilIdentMapping()
                else null
            }.toList()
        }
    }

    private fun ResultSet.tilIdentMapping() = IdentMapping(
        aktørId = this.getString(aktørIdKolonne),
        fødselsnummer = this.getString(fødselsnummerKolonne),
        cachetTidspunkt = this.getTimestamp(cachetTidspunktKolonne).toLocalDateTime()
    )

    private fun kjørFlywayMigreringer() {
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

data class IdentMapping(
    val aktørId: String,
    val fødselsnummer: String,
    val cachetTidspunkt: LocalDateTime
)
