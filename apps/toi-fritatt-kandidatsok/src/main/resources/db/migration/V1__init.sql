CREATE TABLE fritattkandidatsok (
    id bigserial primary key,
    fodselsnummer VARCHAR(11) UNIQUE,
    fritatt_kandidatsok BOOLEAN NOT NULL DEFAULT FALSE,
    sist_endret_av_veileder TEXT,
    sist_endret_av_system TEXT,
    sist_endret_tidspunkt timestamp with time zone not null
);
