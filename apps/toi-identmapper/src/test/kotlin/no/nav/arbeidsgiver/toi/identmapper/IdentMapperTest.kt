package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IdentMapperTest {

    @Test
    fun `skal legge til aktørId på en melding med "fodselsnummer" og uten aktørId og publisere på rapid`() {
        val rapid = TestRapid()
        val fødselsnummerKey = "fodselsnummer"
        val fødselsnummer = "12345678912"
        val aktørId = "123456789"
        AktørIdPopulator(fødselsnummerKey, rapid) { aktørId }

        rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, fødselsnummer))
        Thread.sleep(300)

        val inspektør = rapid.inspektør
        val meldingPåRapid = inspektør.message(0)

        assertThat(inspektør.size).isEqualTo(1)
        assertThat(meldingPåRapid.fieldNames().asSequence().toList()).containsExactlyInAnyOrder(
            "fodselsnummer",
            "aktørId",
            "etAnnetFelt",
            "etObjekt"
        )
        assertThat(meldingPåRapid.get(fødselsnummerKey).asText()).isEqualTo(fødselsnummer)
        assertThat(meldingPåRapid.get("aktørId").asText()).isEqualTo(aktørId)
        assertThat(meldingPåRapid.get("etAnnetFelt").asBoolean()).isFalse
        assertThat(meldingPåRapid.get("etObjekt").fieldNames().asSequence().toList()).containsExactly("enListe")
        assertThat(
            meldingPåRapid.get("etObjekt").get("enListe").asIterable().toList()
                .map { it.intValue() }).containsExactly(1, 2, 3, 4)
    }

    @Test
    fun `skal legge til aktørId på en melding med "fnr" og uten aktørId og publisere på rapid`() {
        val rapid = TestRapid()
        val fødselsnummerKey = "fnr"
        val fødselsnummer = "12345678912"
        val aktørId = "123456789"
        AktørIdPopulator(fødselsnummerKey, rapid) { aktørId }

        rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, fødselsnummer))
        Thread.sleep(300)

        val inspektør = rapid.inspektør
        val meldingPåRapid = inspektør.message(0)

        assertThat(inspektør.size).isEqualTo(1)
        assertThat(meldingPåRapid.get("aktørId").asText()).isEqualTo(aktørId)
    }

    @Test
    fun `skal legge til aktørId på en melding med "fodselsnr" og uten aktørId og publisere på rapid`() {
        val rapid = TestRapid()
        val fødselsnummerKey = "fodselsnr"
        val fødselsnummer = "12345678912"
        val aktørId = "123456789"
        AktørIdPopulator(fødselsnummerKey, rapid) { aktørId }

        rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, fødselsnummer))
        Thread.sleep(300)

        val inspektør = rapid.inspektør
        val meldingPåRapid = inspektør.message(0)

        assertThat(inspektør.size).isEqualTo(1)
        assertThat(meldingPåRapid.get("aktørId").asText()).isEqualTo(aktørId)
    }

    fun meldingUtenAktørId(fødselsnummerKey: String, fødselsnummerValue: String) =
        """
            {
                "$fødselsnummerKey": "$fødselsnummerValue",
                "etAnnetFelt": false,
                "etObjekt": {
                    "enListe": [
                        1,
                        2,
                        3,
                        4
                    ]
                }
            }
        """.trimIndent()

}