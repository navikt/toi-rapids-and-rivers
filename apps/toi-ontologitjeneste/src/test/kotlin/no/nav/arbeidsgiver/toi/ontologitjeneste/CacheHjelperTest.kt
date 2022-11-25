package no.nav.arbeidsgiver.toi.ontologitjeneste

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CacheHjelperTest {
    @Test
    fun `ikke kall funksjon bare ved opprettelse av cache`() {
        var antallFunksjonsKall = 0
        val funksjonsKall: (String)-> OntologiRelasjoner = {
            antallFunksjonsKall++
            OntologiRelasjoner(listOf(it), listOf(it))
        }
        CacheHjelper().lagCache(funksjonsKall)
        assertThat(antallFunksjonsKall).isEqualTo(0)
    }
    @Test
    fun `kall funksjon ved opprettelse av cache-entry`()  {
        var antallFunksjonsKall = 0
        val funksjonsKall: (String)-> OntologiRelasjoner = {
            antallFunksjonsKall++
            OntologiRelasjoner(listOf(it), listOf(it))
        }
        CacheHjelper().lagCache(funksjonsKall)("noe")
        assertThat(antallFunksjonsKall).isEqualTo(1)
    }
    @Test
    fun `returner cachet entry`()  {
        var antallFunksjonsKall = 0
        val funksjonsKall: (String)-> OntologiRelasjoner = {
            antallFunksjonsKall++
            OntologiRelasjoner(listOf(it), listOf(it))
        }
        val cache = CacheHjelper().lagCache(funksjonsKall)
        assertThat(cache("noe")).isEqualTo(cache("noe"))
        assertThat(antallFunksjonsKall).isEqualTo(1)
    }
    @Test
    fun `returner cachet entry per kall`()  {
        var antallFunksjonsKall = 0
        val funksjonsKall: (String)-> OntologiRelasjoner = {
            antallFunksjonsKall++
            OntologiRelasjoner(listOf(it), listOf(it))
        }
        val cache = CacheHjelper().lagCache(funksjonsKall)
        cache("noe")
        cache("noe annet")
        assertThat(cache("noe annet")).isNotEqualTo(cache("noe"))
        assertThat(antallFunksjonsKall).isEqualTo(2)
    }
}