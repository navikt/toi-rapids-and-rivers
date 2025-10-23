package no.nav.arbeidsgiver.toi.kandidat.indekser.geografi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

class GeografiKlient(private val url: String) {
    private val client = HttpClients.createDefault()
    private val objectMapper = jacksonObjectMapper()

    private var geografier = emptyMap<String, Geografi>()
    private var sistHentet = LocalDateTime.MIN

    private fun hentAlleGeografier(): Map<String, Geografi> {
        if(sistHentet.isBefore(LocalDateTime.now().minusHours(1))) {
            val httpGet = HttpGet("$url/rest/geografier").apply {
                addHeader("Nav-CallId", UUID.randomUUID())
            }
            geografier = client.execute(httpGet) { response ->
                when (response.code) {
                    200 -> EntityUtils.toString(response.entity).let { objectMapper.readValue<List<Geografi>>(it) }
                    else -> throw Exception("Feil i kall til postdata med kode ${response.code}")
                }.associateBy(Geografi::geografikode)
            }
            sistHentet = now()
        }
        return geografier
    }

    fun findArenaGeography(geografiKode: String): Geografi? = hentAlleGeografier()[geografiKode]
}