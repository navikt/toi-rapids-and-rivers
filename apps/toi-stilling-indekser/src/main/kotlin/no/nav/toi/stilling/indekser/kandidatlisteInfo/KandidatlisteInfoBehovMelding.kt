package no.nav.toi.stilling.indekser.kandidatlisteInfo

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage

fun lagKandidatlisteInfoMelding(stillingsId: String): JsonMessage {
    return JsonMessage.newMessage(
        mapOf(
            "stillingsId" to stillingsId,
            "@event_name" to "indekserKandidatlisteInfo",
            "@behov" to listOf("kandidatlisteInfo"),
        )
    )
}