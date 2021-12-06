package no.nav.arbeidsgiver.toi.identmapper

class AktorIdCache(private val pdlKlient: PdlKlient) {
    fun hentAktørId(fødselsnummer: String): String? {
        var aktørId = hentCachetAktørId(fødselsnummer)

        if (aktørId == null) {
            aktørId = pdlKlient.hentAktørId(fødselsnummer)
            cacheAktørId(aktørId)
        }

        return aktørId
    }

    private fun cacheAktørId(aktørId: String?) {
        // TODO: Lagre i cache
    }

    private fun hentCachetAktørId(fødselsnummer: String): String? {
        // TODO: Hent cachet aktørId
        
        return null
    }
}
