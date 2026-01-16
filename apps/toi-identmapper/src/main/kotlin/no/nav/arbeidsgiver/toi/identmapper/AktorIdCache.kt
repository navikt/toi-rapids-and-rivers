package no.nav.arbeidsgiver.toi.identmapper


class AktorIdCache(
    private val repository: Repository,
    private val cacheNårAktørIdErNull: Boolean,
    private val hentAktørIdFraPdl: (String) -> (String?)
) {

    private val secureLog = SecureLog(log)

    fun hentAktørId(fødselsnummer: String): String? {
        val cachetAktørId = hentCachetAktørId(fødselsnummer)

        if (cachetAktørId.harHentetFraPdl) {
            log.info("Mappet fra fødselsnummer til aktørId, brukte cache, se securelog for aktørid")
            secureLog.info("Mappet fra fødselsnummer til aktørId ${cachetAktørId.verdi}, brukte cache")
            return cachetAktørId.verdi
        }

        return hentAktørIdFraPdl(fødselsnummer).also { nyAktørId ->
            log.info("Mappet fra fødselsnummer til aktørId, hentet fra PDL, se securelog for aktørid")
            secureLog.info("Mappet fra fødselsnummer til aktørId $nyAktørId, hentet fra PDL")

            if (nyAktørId != null || cacheNårAktørIdErNull) {
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

        return if (sisteMapping == null) {
            CachetAktørId(false, null)
        } else CachetAktørId(true, sisteMapping.aktørId)
    }

    private fun lagre(aktørid: String, fnr: String) {
        repository.lagreAktørId(aktørid, fnr)
    }

    private data class CachetAktørId(
        val harHentetFraPdl: Boolean,
        val verdi: String?
    )
}
