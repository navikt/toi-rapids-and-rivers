package no.nav.arbeidsgiver.toi

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry

class InkomplettSynlighetsgrunnlagLytter(
    private val rapidsConnection: RapidsConnection
) : River.PacketListener {

    private val requiredFields = listOf(
        "arbeidsmarkedCv",
        "veileder",
        "oppfølgingsinformasjon",
        "siste14avedtak",
        "oppfølgingsperiode",
        "arenaFritattKandidatsøk",
        "hjemmel",
        "måBehandleTidligereCv",
        "kvp"
    )

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireKey("aktørId")
                it.interestedIn("@behov")
                it.interestedIn(*requiredFields.toTypedArray())
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val existingBehov: Set<String> =
            if (packet.get("@behov") != null && packet["@behov"].isArray) {
                packet.get("@behov").mapNotNull { if (it.isTextual) it.asText() else null }.toSet()
            } else {
                emptySet()
            }

        if (existingBehov.containsAll(requiredFields)) return

        val newBehov =  (existingBehov union requiredFields).toList()
        packet["@behov"] = newBehov

        val aktorId = packet["aktørId"].asText()
        rapidsConnection.publish(aktorId, packet.toJson())
    }

    override fun onError(problems: MessageProblems, context: MessageContext, metadata: MessageMetadata) {
        println("jjj")
    }

    override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
        super.onSevere(error, context)
    }
}
