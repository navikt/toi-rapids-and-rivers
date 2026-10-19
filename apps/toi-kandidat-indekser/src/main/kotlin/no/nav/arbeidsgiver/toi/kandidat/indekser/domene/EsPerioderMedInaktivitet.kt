package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

class EsPerioderMedInaktivitet(
    @field:JsonProperty private val startdatoForInnevarendeInaktivePeriode: LocalDate?,
    @field:JsonProperty private val sluttdatoerForInaktivePerioderPaToArEllerMer: List<LocalDate>
) {
    override fun equals(other: Any?) = other is EsPerioderMedInaktivitet && this.startdatoForInnevarendeInaktivePeriode == other.startdatoForInnevarendeInaktivePeriode && this.sluttdatoerForInaktivePerioderPaToArEllerMer == other.sluttdatoerForInaktivePerioderPaToArEllerMer

    override fun hashCode() = Objects.hash(
        this.startdatoForInnevarendeInaktivePeriode,
        this.sluttdatoerForInaktivePerioderPaToArEllerMer
    )

    companion object {
        fun fraMelding(packet: JsonMessage) = EsPerioderMedInaktivitet(
            startdatoForInnevarendeInaktivePeriode = packet["hullICv.førsteDagIInneværendeInaktivePeriode"].let { if(it.isMissingOrNull()) null else LocalDate.parse(it.asString()) },
            sluttdatoerForInaktivePerioderPaToArEllerMer = packet["hullICv.sluttdatoerForInaktivePerioder"].values().map(JsonNode::asString).map(LocalDate::parse)
        )
    }
}
