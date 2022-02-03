CREATE TABLE evaluering (
    aktor_id VARCHAR(20) PRIMARY KEY,
    fodselsnummer VARCHAR(11),
    har_aktiv_cv BOOLEAN NOT NULL DEFAULT FALSE,
    har_jobbprofil BOOLEAN NOT NULL DEFAULT FALSE,
    har_sett_hjemmel BOOLEAN NOT NULL DEFAULT FALSE,
    maa_ikke_behandle_tidligere_cv BOOLEAN NOT NULL DEFAULT FALSE,
    ikke_fritatt_kandidatsok BOOLEAN NOT NULL DEFAULT FALSE,
    er_under_oppfolging BOOLEAN NOT NULL DEFAULT FALSE,
    har_riktig_formidlingsgruppe BOOLEAN NOT NULL DEFAULT FALSE,
    er_ikke_kode6_eller_kode7 BOOLEAN NOT NULL DEFAULT FALSE,
    er_ikke_sperret_ansatt BOOLEAN NOT NULL DEFAULT FALSE,
    er_ikke_doed BOOLEAN NOT NULL DEFAULT FALSE,
    er_ferdig_beregnet BOOLEAN NOT NULL DEFAULT FALSE
);
