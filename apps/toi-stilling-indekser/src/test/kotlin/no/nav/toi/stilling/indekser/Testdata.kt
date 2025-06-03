package no.nav.toi.stilling.indekser

import no.nav.toi.TestRapid

fun testProgramMedHendelse(
    env: MutableMap<String, String>,
    hendelse: String,
    assertion: TestRapid.RapidInspector.() -> Unit,
) {
    val rapid = TestRapid()

    startApp(rapid, env)

    rapid.sendTestMessage(hendelse)
    rapid.inspektør.assertion()
}
