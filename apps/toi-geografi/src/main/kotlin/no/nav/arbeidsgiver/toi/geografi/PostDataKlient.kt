package no.nav.arbeidsgiver.toi.geografi

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID

class PostDataKlient(private val url: String) {
    private val client = HttpClients.createDefault()
    private val objectMapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private var postKodeData = emptyMap<String, PostData>()
    private var sistHentet = LocalDateTime.MIN

    private fun hentAllePostkoder(): Map<String, PostData> {
        if(sistHentet.isBefore(now().minusHours(1))) {
            val httpGet = HttpGet("$url/rest/postdata").apply {
                addHeader("Nav-CallId", UUID.randomUUID())
            }
            postKodeData = client.execute(httpGet) { response ->
                when (response.code) {
                    200 -> EntityUtils.toString(response.entity).let { objectMapper.readValue<List<PostData>>(it) }
                    else -> throw Exception("Feil i kall til postdata med kode ${response.code}")
                }.associateBy(PostData::postkode)
            }
            sistHentet = now()
        }
        return postKodeData
    }

    fun findPostData(postnummer: String): PostData? = hentAllePostkoder()[postnummer]
}