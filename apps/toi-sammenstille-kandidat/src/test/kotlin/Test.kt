import no.nav.arbeidsgiver.toi.startApp
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class Test { //TODO: bedre navn

    @Test
    fun test1() { //TODO: bedre navn
        val testRapid = TestRapid()
        startApp(TestDatabase.dataSource, testRapid)
    }


}