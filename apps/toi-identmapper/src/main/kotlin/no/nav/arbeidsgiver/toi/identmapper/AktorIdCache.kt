package no.nav.arbeidsgiver.toi.identmapper

class AktorIdCache(
    private val pdlKlient: PdlKlient,
    private val repository: Repository
) {
    fun hentAktørId(fødselsnummer: String): String? {
        var aktørId = hentCachetAktørId(fødselsnummer)

        if (aktørId == null) {
            aktørId = pdlKlient.hentAktørId(fødselsnummer)?.also { nyAktørId ->
                cacheAktørId(
                    aktørId = nyAktørId,
                    fødselsnummer = fødselsnummer
                )
            }
        }

        return aktørId
    }

    private fun cacheAktørId(aktørId: String, fødselsnummer: String) {
        repository.lagreAktørId(aktørId, fødselsnummer)
    }

    private fun hentCachetAktørId(fødselsnummer: String): String? {
        val identMappinger = repository.hentIdentMappinger(fødselsnummer)
        val nyesteMapping = identMappinger.maxByOrNull { it.cachetTidspunkt }
        // TODO: Refresh aktørId hvis eldre enn n dager ...

        return nyesteMapping?.aktørId
    }
}
