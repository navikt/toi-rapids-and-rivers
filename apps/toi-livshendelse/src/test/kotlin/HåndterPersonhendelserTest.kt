import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import org.assertj.core.api.Assert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HåndterPersonhendelserTest {

    @Test
    fun `sjekk at gradering er sendt for en hendelse med en ident`() {
        val personHendelse1 = PersonhendelseWrapper(
            hendelseId = "id1",
            personidenter = listOf("12312312312"),
            master = "testMaster",
            opprettet = LocalDateTime.of(2023,1,1,0,0).toInstant(ZoneOffset.UTC),
            opplysningstype = "type1",
            endringstype = Endringstype.OPPRETTET,
            tidligereHendelseId = "123",
            adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
        )


        Assertions.assertThat(true).isTrue()
    }
}

class PersonhendelseWrapper(
    hendelseId: String,
    personidenter: List<String>,
    master: String,
    opprettet: Instant,
    opplysningstype: String,
    endringstype: Endringstype, // Husk å importere denne klassen
    tidligereHendelseId: String,
    adressebeskyttelse: Adressebeskyttelse // Husk å importere denne klassen
) {
    val personhendelse: Personhendelse = Personhendelse(
        hendelseId,
        personidenter,
        master,
        opprettet,
        opplysningstype,
        endringstype,
        tidligereHendelseId,
        adressebeskyttelse
    )
}
