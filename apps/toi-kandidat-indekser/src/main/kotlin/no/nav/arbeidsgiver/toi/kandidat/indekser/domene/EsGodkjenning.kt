package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import java.time.OffsetDateTime
import java.util.Objects

class EsGodkjenning(
    private val tittel: String,
    private val utsteder: String,
    private val gjennomfoert: OffsetDateTime,
    private val utloeper: OffsetDateTime,
    private val konseptId: String
): EnAvFlereSamledeKompetaser {

    override fun equals(other: Any?) = other is EsGodkjenning && tittel == other.tittel && utsteder == other.utsteder && gjennomfoert == other.gjennomfoert && utloeper == other.utloeper && konseptId == other.konseptId

    override fun hashCode() = Objects.hash(tittel, utsteder, gjennomfoert, utloeper, konseptId)
    override fun tilSamletKompetanse() = listOf(EsSamletKompetanse(tittel))
}
