package no.nav.arbeidsgiver.toi.identmapper

class AktorIdCache(
    private val repository: Repository,
    private val hentAktørIdFraPdl: (String) -> (String?)
) {
    fun hentAktørId(fødselsnummer: String): String? {
        var aktørId = hentCachetAktørId(fødselsnummer)

        if (aktørId == null) {
            aktørId = hentAktørIdFraPdl(fødselsnummer)?.also { nyAktørId ->
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
