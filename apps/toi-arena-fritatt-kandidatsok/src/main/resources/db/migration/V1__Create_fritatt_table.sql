create table fritatt
(
    id                            serial primary key,
    fnr                           varchar(11) not null,
    startdato                     date        not null,
    sluttdato                     date,
    sendingstatus_aktivert_fritatt text        not null,
    forsoktsendt_aktivert_fritatt   timestamp with time zone,
    sendingstatus_dektivert_fritatt text        not null,
    forsoktsendt_dektivert_fritatt  timestamp with time zone,
    sistendret                    timestamp   with time zone not null,
    melding                       text        not null
);