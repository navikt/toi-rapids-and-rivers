package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

class EsGodkjenning(
    @field:JsonProperty private val tittel: String,
    @field:JsonProperty private val utsteder: String,
    @field:JsonProperty private val gjennomfoert: LocalDate,
    @field:JsonProperty private val utloeper: LocalDate?,
    @field:JsonProperty private val konseptId: String
): EnAvFlereSamledeKompetaser {

    override fun equals(other: Any?) = other is EsGodkjenning && tittel == other.tittel && utsteder == other.utsteder && gjennomfoert == other.gjennomfoert && utloeper == other.utloeper && konseptId == other.konseptId

    override fun hashCode() = Objects.hash(tittel, utsteder, gjennomfoert, utloeper, konseptId)
    override fun tilSamletKompetanse() = listOf(EsSamletKompetanse(tittel))

    companion object {
        fun fraMelding(cvNode: JsonNode) = cvNode["godkjenninger"].map { godkjenning ->
            EsGodkjenning(
                tittel = godkjenning["tittel"].asText(),
                utsteder = godkjenning["utsteder"].asText(),
                gjennomfoert = godkjenning["gjennomfoert"].yyyyMMddTilLocalDate(),
                utloeper = godkjenning["utloeper"]?.let { if(it.isMissingOrNull()) null else it.yyyyMMddTilLocalDate() },
                konseptId = godkjenning["konseptId"].asText(""),
            )
        }
    }
}
