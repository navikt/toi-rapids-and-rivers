package no.nav.toi.stilling.indekser

class OpenSearchService(private val client: IndexClient, private val env: MutableMap<String, String>) {

    fun opprettIndeks() : Boolean {
        val indeks = hentNyesteIndeks()
        val indeksBleOpprettet = opprettIndeksHvisDenIkkeFinnes(indeks)
        if (indeksBleOpprettet){
            client.oppdaterAlias(indeks)
            log.info("Har opprettet indeks $indeks og alias peker nå på denne indeksen")
        } else {
            log.info("Indeks er allerede opprettet og dermed gjøres det ingenting")
        }

        return indeksBleOpprettet
    }

    fun opprettReindekserIndeks() {
        val nyIndeks = hentReindekserIndeks()
        val gjeldendeIndeks = hentGjeldendeIndeks() ?: kanIkkeStarteReindeksering()
        if (!client.finnesIndeks(nyIndeks)) {
            client.opprettIndeks(nyIndeks)
            log.info("Starter reindeksering på ny indeks $nyIndeks")
        } else {
            log.info("Gjenopptar reindeksering på ny indeks $nyIndeks")
        }

        log.info("Fortsetter samtidig konsumering på gjeldende indeks $gjeldendeIndeks")
    }

    fun indekser(stillinger: List<RekrutteringsbistandStilling>, indeks: String) {
        client.indekser(stillinger, indeks)
    }

    fun oppdaterStillingsinfo(stillingsId: String, stillingsinfo: Stillingsinfo, indeks: String) {
        client.oppdaterStillingsinfo(stillingsId = stillingsId, stillingsinfo = stillingsinfo, indeks = indeks)
    }

    fun indekserStilling(stilling: RekrutteringsbistandStilling, indeks: String) {
        client.indekserStilling(stilling, indeks)
    }

    private fun opprettIndeksHvisDenIkkeFinnes(indeksNavn: String): Boolean {
        if (!client.finnesIndeks(stillingAlias)) {
            client.opprettIndeks(indeksNavn)
            return true
        }
        log.info("Bruker eksisterende indeks '$indeksNavn'")
        return false
    }

    fun finnesIndeks(indeksnavn: String): Boolean {
        return client.finnesIndeks(indeksnavn)
    }

    fun byttTilNyIndeks() {
        val indeksnavn = hentNyesteIndeks()
        client.oppdaterAlias(indeksnavn, true)
    }

    fun hentGjeldendeIndeks(): String? {
        return client.hentIndeksAliasPekerPå()
    }

    fun hentNyesteIndeks(): String {
        return hentVersjonFraNaisConfig()
    }

    fun hentGjeldendeIndeksversjon(): String? {
        val indeks = client.hentIndeksAliasPekerPå() ?: return null
        return indeks
    }

    private fun hentReindekserIndeks(): String {
        return env.variable("REINDEKSER_INDEKS")
    }

    fun hentVersjonFraNaisConfig(): String {
        return env.variable("INDEKS_VERSJON")
    }
}
