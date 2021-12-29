package no.nav.arbeidsgiver.toi

import no.nav.helse.rapids_rivers.*

abstract class Lytter(rapidsConnection: RapidsConnection, private val behandler: Behandler, private val hendelseType: HendelseType, påkrevdeFelter: List<String>): River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", hendelseType.eventNavn)
                påkrevdeFelter.forEach { felt -> it.demandKey(felt) }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        behandler.behandleHendelse(Hendelse(hendelseType, packet["aktørId"].asText() , packet))
    }
}

class CvLytter(rapidsConnection: RapidsConnection, behandler: Behandler): Lytter(rapidsConnection, behandler, HendelseType.CV, listOf("aktørId", "cv"))

class VeilederLytter(rapidsConnection: RapidsConnection, behandler: Behandler): Lytter(rapidsConnection, behandler, HendelseType.VEILEDER, listOf("aktørId", "veileder"))

class OppfølgingsinformasjonLytter(rapidsConnection: RapidsConnection, behandler: Behandler): Lytter(rapidsConnection, behandler, HendelseType.OPPFØLGINGSINFORMASJON, listOf("aktørId", "oppfølgingsinformasjon"))

class OppfølgingsperiodeLytter(rapidsConnection: RapidsConnection, behandler: Behandler): Lytter(rapidsConnection, behandler, HendelseType.OPPFØLGINGSPERIODE, listOf("aktørId", "oppfølgingsperiode"))

class FritattKandidatsøkLytter(rapidsConnection: RapidsConnection, behandler: Behandler): Lytter(rapidsConnection, behandler, HendelseType.FRITATT_KANDIDATSØK, listOf("aktørId", "fritattKandidatsøk"))
