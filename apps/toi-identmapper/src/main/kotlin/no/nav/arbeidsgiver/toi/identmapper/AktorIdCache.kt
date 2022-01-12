package no.nav.arbeidsgiver.toi.identmapper

import java.time.LocalDateTime

class AktorIdCache(
    private val repository: Repository,
    private val cluster: String,
    private val hentAktørIdFraPdl: (String) -> (String?)
) {
    private val varighetIDager = 30

    fun hentAktørId(fødselsnummer: String): String? {
        val cachetAktørId = hentCachetAktørId(fødselsnummer)

        if (cachetAktørId.harHentetFraPdl) {
            log.info("Mappet fra fødselsnummer til aktørId ${cachetAktørId.verdi}, brukte cache")
            return cachetAktørId.verdi
        }

        return hentAktørIdFraPdl(fødselsnummer).also { nyAktørId ->
            log.info("Mappet fra fødselsnummer til aktørId $nyAktørId, hentet fra PDL")
            
            if (nyAktørId != null || cluster == "dev-gcp") {
                cacheAktørId(
                    aktørId = nyAktørId,
                    fødselsnummer = fødselsnummer
                )
            }
        }
    }

    private fun cacheAktørId(aktørId: String?, fødselsnummer: String) {
        repository.lagreAktørId(aktørId, fødselsnummer)
    }

    private fun hentCachetAktørId(fødselsnummer: String): CachetAktørId {
        val identMappinger = repository.hentIdentMappinger(fødselsnummer)
        val sisteMapping = identMappinger.maxByOrNull { it.cachetTidspunkt }

        if (sisteMapping == null || mappingErUtgått(sisteMapping)) {
            return CachetAktørId(false, null)
        }

        return CachetAktørId(true, sisteMapping.aktørId)
    }

    private fun mappingErUtgått(identMapping: IdentMapping): Boolean {
        val sisteGyldigeTidspunktForMapping = LocalDateTime.now().minusDays(varighetIDager.toLong())

        return identMapping.cachetTidspunkt.isBefore(sisteGyldigeTidspunktForMapping)
    }

    private data class CachetAktørId(
        val harHentetFraPdl: Boolean,
        val verdi: String?
    )
}
