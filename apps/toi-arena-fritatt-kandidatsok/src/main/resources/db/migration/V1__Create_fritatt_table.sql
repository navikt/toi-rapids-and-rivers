CREATE TABLE fritatt
(
    id                            SERIAL PRIMARY KEY,
    fnr                           VARCHAR(11) NOT NULL,
    melding                       JSON        NOT NULL,
    startdato                     DATE        NOT NULL,
    sluttdato                     DATE        NOT NULL,
    sendingStatusAktivertFritatt  ENUM('ikke_aktuell', 'sendt', 'ikke_sendt', 'kansellert') NOT NULL,
    forsoktSendtAktivertFritatt   TIMESTAMP,
    sendingStatusDektivertFritatt ENUM('ikke_aktuell', 'sendt', 'ikke_sendt', 'kansellert') NOT NULL,
    forsoktSendtDektivertFritatt  TIMESTAMP,
    sistEndret                    TIMESTAMP   NOT NULL
);