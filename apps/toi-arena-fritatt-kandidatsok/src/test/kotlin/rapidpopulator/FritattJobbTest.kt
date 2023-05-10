package rapidpopulator

import org.junit.jupiter.api.Test

class FritattJobbTest {
    @Test
    fun `skal dytte false-melding på rapid om det finnes en ny melding med periode som ikke har startet`(): Unit = TODO()

    @Test
    fun `skal dytte true-melding på rapid om det finnes en ny melding med periode som har startet`(): Unit = TODO()

    @Test
    fun `skal dytte false-melding på rapid om det finnes en ny melding med periode som har sluttet`(): Unit = TODO()

    @Test
    fun `skal dytte false-melding på rapid om det finnes en ny melding med slettet satt til true`(): Unit = TODO()

    @Test
    fun `skal dytte true-melding på rapid om det finnes en melding som har tidligere rapportert at den ikke har startet men har startet nå`(): Unit = TODO()

    @Test
    fun `skal dytte true-melding på rapid om det finnes en ny melding med periode som har startet der det allerede eksisterte en melding som tidligere hadde rapportert å ha sluttet`(): Unit = TODO()
}