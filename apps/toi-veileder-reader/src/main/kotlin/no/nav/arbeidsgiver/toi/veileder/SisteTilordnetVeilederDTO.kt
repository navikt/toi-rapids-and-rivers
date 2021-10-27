package no.nav.arbeidsgiver.toi.veileder

import java.time.ZonedDateTime

data class SisteTilordnetVeilederKafkaDTO(
    val aktorId: String,
    val veilederId: String,
    val tilordnet: ZonedDateTime
)
