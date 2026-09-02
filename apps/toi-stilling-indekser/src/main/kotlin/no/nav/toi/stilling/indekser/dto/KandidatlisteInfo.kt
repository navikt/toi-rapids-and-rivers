package no.nav.toi.stilling.indekser.dto

import java.time.ZonedDateTime
import java.util.UUID

data class KandidatlisteInfo(
    val kandidatlisteId: UUID,
    val antallKandidater: Int,
    val kandidatlisteStatus: String,
    val opprettetDato: ZonedDateTime,
    val eier: String,
)