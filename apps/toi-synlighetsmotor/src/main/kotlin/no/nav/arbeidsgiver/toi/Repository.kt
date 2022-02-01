package no.nav.arbeidsgiver.toi

import com.zaxxer.hikari.HikariDataSource

class Repository(val dataSource: HikariDataSource) {

    private val tabell = "evaluering"

    fun lagre(evaluering: Evaluering, aktørId: String, fødselsnummer: String) {
        val databaseMap = evaluering.databaseMap(aktørId, fødselsnummer)
        val kolonneString = kolonneString(databaseMap.keys.toList())
        val verdiString = verdiString(databaseMap.values.toList())

        dataSource.connection.use {
            it.prepareStatement("insert into evaluering $kolonneString values $verdiString").apply {
                databaseMap.values.forEachIndexed { index, any ->
                    this.setObject(index + 1, any)
                }
            }.executeQuery()
        }
    }

    private fun kolonneString(kolonner: List<String>) = kolonner.joinToString(prefix = "(", separator = ",", postfix = ")")
    private fun verdiString(verdier: List<Any>) = verdier.map { "?" }.joinToString(prefix = "(", separator = ",", postfix = ")")

    private fun Evaluering.databaseMap(aktørId: String, fødselsnummer: String) = mapOf(
        "aktor_id" to aktørId,
        "fodselsnummer" to fødselsnummer,
        "har_aktiv_cv" to harAktivCv,
        "har_jobbprofil" to harJobbprofil,
        "har_sett_hjemmel" to harSettHjemmel,
        "maa_ikke_behandle_tidligere_cv" to maaIkkeBehandleTidligereCv,
        "ikke_fritatt_kandidatsok" to erIkkefritattKandidatsøk,
        "er_under_oppfoelging" to erUnderOppfoelging,
        "har_riktig_formidlingsgruppe" to harRiktigFormidlingsgruppe,
        "er_ikke_kode6_eller_kode7" to erIkkeKode6eller7,
        "er_ikke_sperret_ansatt" to erIkkeSperretAnsatt,
        "er_ikke_doed" to erIkkeDoed,
        "er_ferdig_beregnet" to erFerdigBeregnet
    )
}