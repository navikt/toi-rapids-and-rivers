import no.nav.arbeidsgiver.toi.erKristiHimmelfartsdag
import no.nav.arbeidsgiver.toi.erPinse
import no.nav.arbeidsgiver.toi.erPåske
import no.nav.arbeidsgiver.toi.påskedag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

class PåskeTest {
    @Test
    fun påskeTest() {
        assertEquals(LocalDate.of(2022, 4, 17), påskedag(2022))
        assertEquals(LocalDate.of(2023, 4, 9), påskedag(2023))
        assertEquals(LocalDate.of(2024, 3, 31), påskedag(2024))
        assertEquals(LocalDate.of(2025, 4, 20), påskedag(2025))
        assertEquals(LocalDate.of(2026, 4, 5), påskedag(2026))
        assertEquals(LocalDate.of(2027, 3, 28), påskedag(2027))
        assertEquals(LocalDate.of(2028, 4, 16), påskedag(2028))
        assertEquals(LocalDate.of(2029, 4, 1), påskedag(2029))
    }
    @Test
    fun erPåske() {
        assertEquals(false, LocalDate.of(2023, Month.APRIL, 4).erPåske())
        assertEquals(true, LocalDate.of(2023, Month.APRIL, 5).erPåske())
        assertEquals(true, LocalDate.of(2023, Month.APRIL, 6).erPåske())
        assertEquals(true, LocalDate.of(2023, Month.APRIL, 7).erPåske())
        assertEquals(true, LocalDate.of(2023, Month.APRIL, 8).erPåske())
        assertEquals(true, LocalDate.of(2023, Month.APRIL, 9).erPåske())
        assertEquals(true, LocalDate.of(2023, Month.APRIL, 10).erPåske())
        assertEquals(false, LocalDate.of(2023, Month.APRIL, 11).erPåske())
    }
    @Test
    fun erPinse() {
        assertEquals(false, LocalDate.of(2022, Month.JUNE, 4).erPinse())
        assertEquals(true, LocalDate.of(2022, Month.JUNE, 6).erPinse())
        assertEquals(false, LocalDate.of(2022, Month.JUNE, 7).erPinse())
        assertEquals(false, LocalDate.of(2023, Month.MAY, 27).erPinse())
        assertEquals(true, LocalDate.of(2023, Month.MAY, 29).erPinse())
        assertEquals(false, LocalDate.of(2023, Month.MAY, 30).erPinse())
        assertEquals(false, LocalDate.of(2024, Month.MAY, 18).erPinse())
        assertEquals(true, LocalDate.of(2024, Month.MAY, 20).erPinse())
        assertEquals(false, LocalDate.of(2024, Month.MAY, 21).erPinse())
        assertEquals(false, LocalDate.of(2025, Month.JUNE, 7).erPinse())
        assertEquals(true, LocalDate.of(2025, Month.JUNE, 9).erPinse())
        assertEquals(false, LocalDate.of(2025, Month.JUNE, 10).erPinse())
        assertEquals(false, LocalDate.of(2026, Month.MAY, 23).erPinse())
        assertEquals(true, LocalDate.of(2026, Month.MAY, 25).erPinse())
        assertEquals(false, LocalDate.of(2026, Month.MAY, 26).erPinse())
        assertEquals(false, LocalDate.of(2027, Month.MAY, 15).erPinse())
        assertEquals(true, LocalDate.of(2027, Month.MAY, 17).erPinse())
        assertEquals(false, LocalDate.of(2027, Month.MAY, 18).erPinse())
        assertEquals(false, LocalDate.of(2028, Month.JUNE, 3).erPinse())
        assertEquals(true, LocalDate.of(2028, Month.JUNE, 5).erPinse())
        assertEquals(false, LocalDate.of(2028, Month.JUNE, 6).erPinse())
        assertEquals(false, LocalDate.of(2029, Month.MAY, 19).erPinse())
        assertEquals(true, LocalDate.of(2029, Month.MAY, 21).erPinse())
        assertEquals(false, LocalDate.of(2029, Month.MAY, 22).erPinse())
    }
    @Test
    fun erKristiHimmelfartsdag() {
        assertEquals(false, LocalDate.of(2022, Month.MAY, 25).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2022, Month.MAY, 26).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2022, Month.MAY, 27).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2023, Month.MAY, 17).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2023, Month.MAY, 18).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2023, Month.MAY, 19).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2024, Month.MAY, 8).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2024, Month.MAY, 9).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2024, Month.MAY, 10).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2025, Month.MAY, 28).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2025, Month.MAY, 29).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2025, Month.MAY, 30).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2026, Month.MAY, 13).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2026, Month.MAY, 14).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2026, Month.MAY, 15).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2027, Month.MAY, 5).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2027, Month.MAY, 6).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2027, Month.MAY, 7).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2028, Month.MAY, 24).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2028, Month.MAY, 25).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2028, Month.MAY, 26).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2029, Month.MAY, 9).erKristiHimmelfartsdag())
        assertEquals(true, LocalDate.of(2029, Month.MAY, 10).erKristiHimmelfartsdag())
        assertEquals(false, LocalDate.of(2029, Month.MAY, 11).erKristiHimmelfartsdag())
    }
}