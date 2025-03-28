package no.nav.toi.stilling.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid

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
