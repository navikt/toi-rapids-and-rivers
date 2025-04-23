package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.KeyMessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

class TestRapid(private val meterRegistry: MeterRegistry = SimpleMeterRegistry(), private val maxTriggedeMeldinger: Int = 10) :
    RapidsConnection() {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    private val messages = mutableListOf<Pair<String?, String>>()
    val inspektør get() = RapidInspector(messages.toList())

    fun reset() {
        messages.clear()
    }

    fun sendTestMessage(message: String) {
        notifyMessage(message, this, MessageMetadata("test.message", -1, -1, null, emptyMap()), meterRegistry)
        sjekkForLoop()
    }

    fun sendTestMessage(message: String, key: String) {
        notifyMessage(message, KeyMessageContext(this, key), MessageMetadata("test.message", -1, -1, key, emptyMap()), meterRegistry)
        sjekkForLoop()
    }

    private fun sjekkForLoop() {
        val meldingerFørLoopTest = keyOgMeldinger()
        sjekkForLoopRecursive(0)
        messages.clear()
        meldingerFørLoopTest.forEach { (key, message) ->
            if (key == null) publish(message.toString()) else publish(key, message.toString())
        }
    }

    private fun sjekkForLoopRecursive(kjørFra: Int) {
        val messages = keyOgMeldinger()
        messages.filterIndexed { index, _ -> index>=kjørFra }.forEach { (key, message) ->
            if(key == null) {
                notifyMessage(message.toString(), this, MessageMetadata("test.message", -1, -1, null, emptyMap()), meterRegistry)
            } else {
                notifyMessage(message.toString(), KeyMessageContext(this, key), MessageMetadata("test.message", -1, -1, key, emptyMap()), meterRegistry)
            }
        }
        val newMessages = keyOgMeldinger()
        if (newMessages.size > maxTriggedeMeldinger) {
            throw IllegalStateException("Loop antatt med ${newMessages.size} meldinger: $newMessages")
        } else if(newMessages.size > messages.size) {
            sjekkForLoopRecursive(messages.size)
        } else if(newMessages.size < messages.size) {
            throw IllegalStateException("Skal ikke være mulig å nå denne linjen. Feil i implementasjonen av ${this::class.java.simpleName}")
        }
    }

    private fun keyOgMeldinger() = inspektør.let { inspektør ->
        (0 until inspektør.size)
            .map { inspektør.key(it) to inspektør.message(it) }
    }

    override fun publish(message: String) {
        messages.add(null to message)
    }

    override fun publish(key: String, message: String) {
        messages.add(key to message)
    }

    override fun rapidName(): String {
        return "testRapid"
    }

    override fun start() {}
    override fun stop() {}

    class RapidInspector(private val messages: List<Pair<String?, String>>) {
        private val jsonMessages = mutableMapOf<Int, JsonNode>()
        val size get() = messages.size

        fun key(index: Int) = messages[index].first
        fun message(index: Int) = jsonMessages.getOrPut(index) { objectMapper.readTree(messages[index].second) }
        fun field(index: Int, field: String) =
            requireNotNull(message(index).path(field).takeUnless { it.isMissingNode || it.isNull }) {
                "Message does not contain field '$field'"
            }
    }
}