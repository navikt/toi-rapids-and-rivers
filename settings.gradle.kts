rootProject.name = "toi-rapids-and-rivers"

// https://docs.gradle.org/current/userguide/configuration_cache_enabling.html#config_cache:stable
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

include(
    "apps:toi-arbeidsmarked-cv",
    "apps:toi-veileder",
    "apps:toi-sammenstille-kandidat",
    "apps:toi-oppfolgingsinformasjon",
    "apps:toi-siste-14a-vedtak",
    "apps:toi-identmapper",
    "apps:toi-synlighetsmotor",
    "apps:toi-siste-oppfolgingsperiode",
    "apps:toi-siste-oppfolgingsperiode-pond",
    "apps:toi-organisasjonsenhet",
    "apps:toi-helseapp",
    "apps:toi-hull-i-cv",
    "apps:toi-ontologitjeneste",
    "apps:toi-arbeidsgiver-notifikasjon",
    "apps:toi-kvp",
    "apps:toi-livshendelse",
    "apps:toi-evaluertdatalogger",
    "apps:asr-domain",
    "apps:toi-arbeidssoekeropplysninger",
    "apps:toi-arbeidssoekerperiode",
    "apps:toi-publisering-til-arbeidsplassen",
    "apps:toi-stilling-indekser",
    "apps:toi-publiser-dir-stillinger",
    "technical-libs:testrapid",
    "technical-libs:testrapid-jackson2-deprekert",
    "apps:toi-kandidat-indekser",
    "apps:toi-geografi"
)
