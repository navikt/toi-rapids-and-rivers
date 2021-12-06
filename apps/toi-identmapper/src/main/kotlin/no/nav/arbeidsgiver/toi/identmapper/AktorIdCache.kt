package no.nav.arbeidsgiver.toi.identmapper

class AktorIdCache(private val pdlKlient: PdlKlient) {
    fun hentAktørId(fødselsnummer: String): String? {
        return pdlKlient.hentAktørId(fødselsnummer)
    }
}
