package no.nav.arbeidsgiver.toi.identmapper

import org.flywaydb.core.Flyway
import java.time.LocalDateTime
import javax.sql.DataSource

class Repository(private val dataSource: DataSource) {
    init {
        kjørFlywayMigreringer()
    }

    fun lagreAktørId(aktørId: String, fødselsnummer: String) {
        TODO("Not yet implemented")
    }

    fun hentAktørId(fødselsnummer: String): IdentMapping {
        TODO("Not yet implemented")
    }

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
