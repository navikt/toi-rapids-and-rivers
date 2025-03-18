package no.nav.arbeidsgiver.toi

fun requiredFieldsSynlilghetsbehov() = listOf(
    "arbeidsmarkedCv",
    "veileder",     // TODO: synlighetsmotor har ikke behov for denne. flytt need til kandidatfeed
    "oppfølgingsinformasjon",
    "siste14avedtak",     // TODO: synlighetsmotor har ikke behov for denne. flytt need til kandidatfeed
    "oppfølgingsperiode",
    "arenaFritattKandidatsøk",
    "hjemmel",
    "måBehandleTidligereCv",
    "kvp",
    "adressebeskyttelse"
)
