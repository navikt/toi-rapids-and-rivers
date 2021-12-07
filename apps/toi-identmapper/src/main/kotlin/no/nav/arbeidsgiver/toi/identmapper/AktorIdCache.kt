package no.nav.arbeidsgiver.toi.identmapper

import java.time.LocalDateTime

class AktorIdCache(
    private val repository: Repository,
    private val hentAktørIdFraPdl: (String) -> (String?)
) {
    private val varighetIDager = 30

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
        val sisteMapping = identMappinger.maxByOrNull { it.cachetTidspunkt }

        if (sisteMapping == null || mappingErUtgått(sisteMapping)) {
            return null
        }

        return sisteMapping.aktørId
    }

    private fun mappingErUtgått(identMapping: IdentMapping): Boolean {
        val sisteGyldigeTidspunktForMapping = LocalDateTime.now().minusDays(varighetIDager.toLong())

        return identMapping.cachetTidspunkt.isBefore(sisteGyldigeTidspunktForMapping)
    }
}
