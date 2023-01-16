package no.nav.arbeidsgiver.toi.presentertekandidater.notifikasjoner

import no.nav.arbeidsgiver.toi.presentertekandidater.sikkerhet.TokendingsKlient
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

class Notifikasjon(private val tokendingsKlient: TokendingsKlient) {
    private val skalSletteNotifikasjonOm = Duration.of(3, ChronoUnit.MONTHS)

    fun sendNotifikasjon(accessToken: String, epostArbeidsgiver: String, stillingsId: UUID, virksomhetsnummer: String) {

    }

    private fun lenkeTilKandidatliste() = ""
}
