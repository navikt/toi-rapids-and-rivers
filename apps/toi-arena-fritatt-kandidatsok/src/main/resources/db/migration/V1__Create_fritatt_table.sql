CREATE TABLE fritatt
(
    db_id                     SERIAL PRIMARY KEY,
    fnr                       VARCHAR(11) UNIQUE       NOT NULL,
    startdato                 DATE                     NOT NULL,
    sluttdato                 DATE,
    sending_aktivert_status   TEXT                     NOT NULL,
    forsoktsendt_aktivert     TIMESTAMP WITH TIME ZONE,
    sending_deaktivert_status TEXT                     NOT NULL,
    forsoktsendt_deaktivert   TIMESTAMP WITH TIME ZONE,
    sistendret_i_arena        TIMESTAMP WITH TIME ZONE NOT NULL,
    slettet_i_arena           BOOLEAN                  NOT NULL,
    opprettet_rad             TIMESTAMP WITH TIME ZONE NOT NULL,
    sist_endret_rad           TIMESTAMP WITH TIME ZONE NOT NULL,
    melding_fra_arena         TEXT                     NOT NULL
);