CREATE TABLE fritatt
(
    id                            SERIAL PRIMARY KEY,
    fnr                           VARCHAR(11) NOT NULL,
    startdato                     DATE        NOT NULL,
    sluttdato                     DATE        NOT NULL,
    sendingStatusAktivertFritatt  TEXT        NOT NULL,
    forsoktSendtAktivertFritatt   TIMESTAMP,
    sendingStatusDektivertFritatt TEXT        NOT NULL,
    forsoktSendtDektivertFritatt  TIMESTAMP,
    sistEndret                    TIMESTAMP   NOT NULL,
    melding                       TEXT        NOT NULL

);