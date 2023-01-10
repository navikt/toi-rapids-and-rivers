CREATE TABLE fritattkandidatsok (
    id bigserial PRIMARY KEY,
    fodselsnummer VARCHAR(11) UNIQUE,
    fritatt_kandidatsok BOOLEAN NOT NULL DEFAULT FALSE,
    sist_endret_av_veileder TEXT,
    sist_endret_av_system TEXT,
    sist_endret_tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL
);
