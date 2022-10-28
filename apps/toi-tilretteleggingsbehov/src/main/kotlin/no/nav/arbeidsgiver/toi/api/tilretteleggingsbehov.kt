package no.nav.arbeidsgiver.toi.api

import java.time.LocalDateTime

data class TilretteleggingsbehovInput(
    val fnr: Fødselsnummer,
    val arbeidstid: Set<Arbeidstid>,
    val fysisk: Set<Fysisk>,
    val arbeidshverdagen: Set<Arbeidshverdagen>,
    val utfordringerMedNorsk: Set<UtfordringerMedNorsk>
)

data class Tilretteleggingsbehov(
    val fnr: Fødselsnummer,
//    private val aktørId: String,
    val sistEndretAv: String,
    val sistEndretTidspunkt: LocalDateTime,
    val arbeidstid: Set<Arbeidstid>,
    val fysisk: Set<Fysisk>,
    val arbeidshverdagen: Set<Arbeidshverdagen>,
    val utfordringerMedNorsk: Set<UtfordringerMedNorsk>
)

enum class Arbeidstid {
    KAN_IKKE_JOBBE,
    HELTID,
    IKKE_HELE_DAGER,
    BORTE_FASTE_DAGER_ELLER_TIDER,
    FLEKSIBEL,
    GRADVIS_ØKNING
}

enum class Fysisk {
    ARBEIDSSTILLING,
    ERGONOMI,
    TUNGE_LØFT,
    HØRSEL,
    SYN,
    ANNET,
    UNIVERSELL_UTFORMING
}

enum class Arbeidshverdagen {
    TILRETTELAGT_OPPLÆRING,
    TILRETTELAGTE_ARBEIDSOPPGAVER,
    MENTOR,
    ANNET,
    STILLE_OG_ROLIG_MILJØ,
    PERSONLIG_BISTAND
}

enum class UtfordringerMedNorsk {
    SNAKKE_NORSK,
    SKRIVE_NORSK,
    LESE_NORSK,
    REGNING_OG_TALLFORSTÅELSE,
    ANDRE_UTFORDRINGER
}

typealias Fødselsnummer = String
