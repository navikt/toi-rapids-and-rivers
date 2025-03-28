package no.nav.toi.stilling.indekser

class OpenSearchService(private val client: IndexClient, private val env: MutableMap<String, String>) {

    fun initialiserIndeksering() {
        val indeks = hentNyesteIndeks()

        log.info("Initialiser indeksering på indeks $indeks")

        val indeksBleOpprettet = opprettIndeksHvisDenIkkeFinnes(indeks)
        if (indeksBleOpprettet) client.oppdaterAlias(indeks)
    }

    fun initialiserReindeksering() {
        val nyIndeks = hentNyesteIndeks()
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

    fun skalReindeksere(): Boolean {
        if (!client.finnesIndeks(stillingAlias)) return false
        val gjeldendeVersjon = hentGjeldendeIndeksversjon() ?: return false // indeks finnes ikke enda
        val nyIndeksVersjon = env["INDEKS_VERSJON"]?: throw NullPointerException("Miljøvariabel INDEKS_VERSJON")

        return nyIndeksVersjon != gjeldendeVersjon
    }

    fun hentGjeldendeIndeksversjon(): String? {
        val indeks = client.hentIndeksAliasPekerPå() ?: return null
        return hentVersjon(indeks)
    }

    fun byttTilNyIndeks() {
        val indeksnavn = hentNyesteIndeks()
        client.fjernAlias()
        client.oppdaterAlias(indeksnavn)
    }

    fun hentGjeldendeIndeks(): String? {
        return client.hentIndeksAliasPekerPå()
    }

    private fun hentVersjon(indeksNavn: String): String {
        return indeksNavn.split("_").last()
    }

    fun hentNyesteIndeks(): String {
        return hentIndeksNavn(hentVersjonFraNaisConfig())
    }

    private fun hentVersjonFraNaisConfig(): String {
        return env["INDEKS_VERSJON"] ?: throw NullPointerException("Miljøvariabel INDEKS_VERSJON")
    }

    private fun hentIndeksNavn(versjon: String): String {
        return "${stillingAlias}_$versjon"
    }

    private fun kanIkkeStarteReindeksering(): Nothing {
        throw Exception("Kan ikke starte reindeksering uten noen alias som peker på indeks")
    }
}
