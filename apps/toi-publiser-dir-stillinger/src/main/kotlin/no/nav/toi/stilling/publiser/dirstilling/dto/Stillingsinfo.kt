package no.nav.toi.stilling.publiser.dirstilling.dto

data class Stillingsinfo(
    val stillingsid: String,
    val stillingsinfoid: String,
    val eierNavident: String?,
    val eierNavn: String?,
    val stillingskategori: Stillingskategori?,
    val eierNavKontorEnhetId: String?,
)
