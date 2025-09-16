package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

class EsGodkjenning(
    private val tittel: String,
    private val utsteder: String,
    private val gjennomfoert: LocalDate,
    private val utloeper: LocalDate?,
    private val konseptId: String
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
