import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.arbeidsgiver.toi.livshendelser.AccessTokenClient
import no.nav.arbeidsgiver.toi.livshendelser.PdlKlient
import no.nav.arbeidsgiver.toi.livshendelser.PersonhendelseService
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class HåndterPersonhendelserTest {

    private val wiremock = WireMockServer(8083).also(WireMockServer::start)
    private val mockOAuth2Server = MockOAuth2Server().also { it.start(InetAddress.getByName("localhost"), 18301) }

    @Test
    fun `sjekk at gradering er sendt for en hendelse med en ident`() {

        val personHendelse = personhendelse(
            hendelseId = "id1",
            personidenter = listOf("12312312312"),
            master = "testMaster",
            opprettet = LocalDateTime.of(2023,1,1,0,0).toInstant(ZoneOffset.UTC),
            opplysningstype = "type1",
            endringstype = Endringstype.OPPRETTET,
            tidligereHendelseId = "123",
            adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
        )
        val testRapid = TestRapid()
        val envs = mapOf("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to "http://localhost:18301/isso-idtoken/.well-known/openid-configuration")
        PersonhendelseService(testRapid, PdlKlient("http://localhost:8083/", AccessTokenClient(envs))).håndter(listOf(personHendelse))

        val inspektør = testRapid.inspektør
        assertThat(inspektør.size).isEqualTo(1)
    }
}

fun personhendelse(
    hendelseId: String,
    personidenter: List<String>,
    master: String,
    opprettet: Instant,
    opplysningstype: String,
    endringstype: Endringstype, // Husk å importere denne klassen
    tidligereHendelseId: String,
    adressebeskyttelse: Adressebeskyttelse // Husk å importere denne klassen
) = Personhendelse(
    hendelseId,
    personidenter,
    master,
    opprettet,
    opplysningstype,
    endringstype,
    tidligereHendelseId,
    adressebeskyttelse
)
