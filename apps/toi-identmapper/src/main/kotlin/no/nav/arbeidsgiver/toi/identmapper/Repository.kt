package no.nav.arbeidsgiver.toi.identmapper

import java.time.LocalDateTime
import javax.sql.DataSource

class Repository(dataSource: DataSource) {
    fun lagreAktørId(aktørId: String, fødselsnummer: String) {
        TODO("Not yet implemented")
    }

    fun hentAktørId(fødselsnummer: String): IdentMapping {
        TODO("Not yet implemented")
    }
}

data class IdentMapping(
    val aktørId: String,
    val fødselsnummer: String,
    val cachetTidspunkt: LocalDateTime
)
