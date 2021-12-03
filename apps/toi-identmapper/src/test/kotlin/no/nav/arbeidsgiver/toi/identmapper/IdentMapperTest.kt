package no.nav.arbeidsgiver.toi.identmapper

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdentMapperTest {

    @Test
    fun `skal legge til aktørId på en melding med "fodselsnummer" og uten aktørId og publisere på rapid`() {
        val rapid = TestRapid()
        val fødselsnummerKey = "fodselsnummer"
        val fødselsnummer = "12345678912"
        val aktørId = "123456789"

        AktorIdPopulator(fødselsnummerKey, rapid, "test") { aktørId }
        rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, fødselsnummer))

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

        AktorIdPopulator(fødselsnummerKey, rapid, "test") { aktørId }
        rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, fødselsnummer))

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

        AktorIdPopulator(fødselsnummerKey, rapid, "test") { aktørId }
        rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, fødselsnummer))

        val inspektør = rapid.inspektør
        val meldingPåRapid = inspektør.message(0)

        assertThat(inspektør.size).isEqualTo(1)
        assertThat(meldingPåRapid.get("aktørId").asText()).isEqualTo(aktørId)
    }

    @Test
    fun `en melding med fødselsnummer som ikke finnes i PDL skal gi feil i prod`() {
        val rapid = TestRapid()
        val fødselsnummerKey = "fodselsnummer"

        assertThrows<RuntimeException> {
            AktorIdPopulator(fødselsnummerKey, rapid, "prod-gcp") { null }
            rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, "finnesIkke"))
        }
    }

    @Test
    fun `en melding med fødselsnummer som ikke finnes i PDL skal ignoreres i dev`() {
        val rapid = TestRapid()
        val fødselsnummerKey = "fodselsnummer"

        AktorIdPopulator(fødselsnummerKey, rapid, "dev-gcp") { null }
        rapid.sendTestMessage(meldingUtenAktørId(fødselsnummerKey, "finnesIkke"))

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `en melding som inneholder "aktørId" skal ikke publiseres på nytt`() {
        val rapid = TestRapid()
        val aktørIdKey = "aktørId"
        val fødselsnummerKey = "fodselsnummer"

        AktorIdPopulator(fødselsnummerKey, rapid, "test") { "dummyAktørId" }
        rapid.sendTestMessage(meldingMedAktørId(aktørIdKey))

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `en melding som inneholder nøkkelen "aktorId" skal ikke publiseres på nytt`() {
        val rapid = TestRapid()
        val aktørIdKey = "aktorId"
        val fødselsnummerKey = "fodselsnummer"

        AktorIdPopulator(fødselsnummerKey, rapid, "test") { "dummyAktørId" }
        rapid.sendTestMessage(meldingMedAktørId(aktørIdKey))

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `en melding som inneholder "aktoerId" skal ikke publiseres på nytt`() {
        val rapid = TestRapid()
        val aktørIdKey = "aktoerId"
        val fødselsnummerKey = "fodselsnummer"

        AktorIdPopulator(fødselsnummerKey, rapid, "test") { "dummyAktørId" }
        rapid.sendTestMessage(meldingMedAktørId(aktørIdKey))

        assertThat(rapid.inspektør.size).isEqualTo(0)
    }

    @Test
    fun `en melding som inneholder "AKTORID" skal ikke publiseres på nytt`() {
        val rapid = TestRapid()
        val aktørIdKey = "AKTORID"
        val fødselsnummerKey = "fodselsnummer"

        AktorIdPopulator(fødselsnummerKey, rapid, "test") { "dummyAktørId" }
        rapid.sendTestMessage(meldingMedAktørId(aktørIdKey))

        assertThat(rapid.inspektør.size).isEqualTo(0)
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

    fun meldingMedAktørId(aktørIdKey: String) =
        """
            {
                "$aktørIdKey": "1234566534",
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