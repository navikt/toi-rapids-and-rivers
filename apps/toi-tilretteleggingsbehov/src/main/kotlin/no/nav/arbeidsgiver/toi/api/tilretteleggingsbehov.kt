package no.nav.arbeidsgiver.toi.api

import java.time.LocalDateTime

data class TilretteleggingsbehovInput(
    private val fødselsnummer: String,
    private val arbeidstid: Set<Arbeidstid>,
    private val fysisk: Set<Fysisk>,
    private val arbeidshverdagen: Set<Fysisk>,
    private val utfordringerMedNorsk: Set<UtfordringerMedNorsk>
)

data class Tilretteleggingsbehov(
    private val fødselsnummer: String,
//    private val aktørId: String,
    private val sistEndretAvNavIdent: String,
    private val sistEndretTidspunkt: LocalDateTime,
    private val arbeidstid: Set<Arbeidstid>,
    private val fysisk: Set<Fysisk>,
    private val arbeidshverdagen: Set<Arbeidshverdagen>,
    private val utfordringerMedNorsk: Set<UtfordringerMedNorsk>
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
