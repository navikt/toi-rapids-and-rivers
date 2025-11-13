package no.nav.arbeidsgiver.toi.arbeidsgiver.notifikasjon

import erGyldigEpostadresse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EpostValidatorTest {

    @Test
    fun `erGyldigEpostadresse godtar gyldige epostadresser`() {
        assertTrue(erGyldigEpostadresse("test@test.nav"))
        assertTrue(erGyldigEpostadresse("navn.test.navnesen@test.nav"))

        assertFalse(erGyldigEpostadresse(""))
        assertFalse(erGyldigEpostadresse("  "))
        assertFalse(erGyldigEpostadresse("."))
        assertFalse(erGyldigEpostadresse("@"))
        assertFalse(erGyldigEpostadresse("test"))
        assertFalse(erGyldigEpostadresse("test.test.nav"))
        assertFalse(erGyldigEpostadresse("test.test@nav"))
    }
}
