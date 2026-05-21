package no.nav.arbeidsgiver.toi.identmapper

class IdentCache(
    private val repository: IdentRepository,
    private val cacheNårAktørIdErNull: Boolean,
    private val hentAktørIdFraPdl: (String) -> (String?),
    private val hentFødselsnummerFraPdl: (String) -> (String?),
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
                cacheAktørId(aktørId = nyAktørId, fødselsnummer = fødselsnummer)
            }
        }
    }

    fun hentFødselsnummer(aktørId: String): String? {
        val cachetFødselsnummer = hentCachetFødselsnummer(aktørId)

        if (cachetFødselsnummer.harHentetFraPdl) {
            log.info("Mappet fra aktørId til fødselsnummer, brukte cache")
            secureLog.info("Mappet fra aktørId til fødselsnummer ${cachetFødselsnummer.verdi}, brukte cache")
            return cachetFødselsnummer.verdi
        }

        return hentFødselsnummerFraPdl(aktørId).also { nyttFødselsnummer ->
            log.info("Mappet fra aktørId til fødselsnummer, hentet fra PDL")
            secureLog.info("Mappet fra aktørId til fødselsnummer $nyttFødselsnummer, hentet fra PDL")

            if (nyttFødselsnummer != null) {
                repository.lagreIdentMapping(aktørId = aktørId, fødselsnummer = nyttFødselsnummer)
            }
        }
    }

    private fun cacheAktørId(aktørId: String?, fødselsnummer: String) {
        repository.lagreIdentMapping(aktørId, fødselsnummer)
    }

    private fun hentCachetAktørId(fødselsnummer: String): CachetAktørId {
        val identMappinger = repository.hentIdentMappingerForFnr(fødselsnummer)
        val sisteMapping = identMappinger.maxByOrNull { it.cachetTidspunkt }
        return if (sisteMapping == null) CachetAktørId(false, null)
        else CachetAktørId(true, sisteMapping.aktørId)
    }

    private fun hentCachetFødselsnummer(aktørId: String): CachetFødselsnummer {
        val identMappinger = repository.hentIdentMappingerForAktørId(aktørId)
        val sisteMapping = identMappinger.maxByOrNull { it.cachetTidspunkt }
        return if (sisteMapping == null) CachetFødselsnummer(false, null)
        else CachetFødselsnummer(true, sisteMapping.fødselsnummer)
    }

    private data class CachetAktørId(val harHentetFraPdl: Boolean, val verdi: String?)
    private data class CachetFødselsnummer(val harHentetFraPdl: Boolean, val verdi: String?)
}

