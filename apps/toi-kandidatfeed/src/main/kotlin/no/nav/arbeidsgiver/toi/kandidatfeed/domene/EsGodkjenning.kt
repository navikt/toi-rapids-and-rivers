package no.nav.arbeidsgiver.toi.kandidatfeed.domene

import java.util.*

class EsGodkjenning(
    private val tittel: String,
    private val utsteder: String,
    private val gjennomfoert: Date,
    private val utloeper: Date,
    private val konseptId: String
): EnAvFlereSamledeKompetaser {

    override fun equals(other: Any?) = other is EsGodkjenning && tittel == other.tittel && utsteder == other.utsteder && gjennomfoert == other.gjennomfoert && utloeper == other.utloeper && konseptId == other.konseptId

    override fun hashCode() = Objects.hash(tittel, utsteder, gjennomfoert, utloeper, konseptId)
    override fun tilSamletKompetanse() = listOf(EsSamletKompetanse(tittel))
}
