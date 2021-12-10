package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.*

class CvLytter(rapidsConnection: RapidsConnection, private val behandler: Behandler) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "cv")
                it.demandKey("aktørId")
                it.interestedIn("cv")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(HendelseType.CV, packet["aktørId"].asText() , packet))
    }
}

class VeilederLytter(
    rapidsConnection: RapidsConnection, private val behandler: Behandler
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "veileder")
                it.demandKey("veileder")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(HendelseType.VEILEDER, packet["aktørId"].asText(), packet))
    }
}

class OppfølgingsinformasjonLytter(
    rapidsConnection: RapidsConnection, private val behandler: Behandler
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppfølgingsinformasjon")
                it.demandKey("oppfølgingsinformasjon")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(HendelseType.OPPFØLGINGSINFORMASJON, packet["aktørId"].asText(), packet))
    }
}

class OppfølgingsperiodeLytter(
    rapidsConnection: RapidsConnection, private val behandler: Behandler
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppfølgingsperiode")
                it.demandKey("oppfølgingsperiode")
                it.demandKey("aktørId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(HendelseType.OPPFØLGINGSPERIODE, packet["aktørId"].asText(), packet))
    }
}
