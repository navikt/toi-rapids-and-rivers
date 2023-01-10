CREATE TABLE fritattkandidatsok (
    fodselsnummer VARCHAR(11),
    fritatt_kandidatsok BOOLEAN NOT NULL DEFAULT FALSE,
    sist_endret_av_veileder TEXT,
    sist_endret_av_system TEXT,
    sist_endret_tidspunkt timestamp with time zone not null
);
