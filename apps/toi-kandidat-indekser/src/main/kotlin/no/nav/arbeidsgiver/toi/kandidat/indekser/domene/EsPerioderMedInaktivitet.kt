package no.nav.arbeidsgiver.toi.kandidat.indekser.domene

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects

class EsPerioderMedInaktivitet(
    private val startdatoForInnevarendeInaktivePeriode: LocalDate?,
    private val sluttdatoerForInaktivePerioderPaToArEllerMer: List<LocalDate>
) {
    override fun equals(other: Any?) = other is EsPerioderMedInaktivitet && this.startdatoForInnevarendeInaktivePeriode == other.startdatoForInnevarendeInaktivePeriode && this.sluttdatoerForInaktivePerioderPaToArEllerMer == other.sluttdatoerForInaktivePerioderPaToArEllerMer

    override fun hashCode() = Objects.hash(
        this.startdatoForInnevarendeInaktivePeriode,
        this.sluttdatoerForInaktivePerioderPaToArEllerMer
    )

    companion object {
        fun fraMelding(packet: JsonMessage) = EsPerioderMedInaktivitet(
            startdatoForInnevarendeInaktivePeriode = packet["hullICv.førsteDagIInneværendeInaktivePeriode"].let { if(it.isMissingOrNull()) null else LocalDate.parse(it.asText()) },
            sluttdatoerForInaktivePerioderPaToArEllerMer = packet["hullICv.sluttdatoerForInaktivePerioder"].map(JsonNode::asText).map(LocalDate::parse)
        )
    }
}
