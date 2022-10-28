CREATE TABLE tilretteleggingsbehov (
    fødselsnummer VARCHAR(11) PRIMARY KEY,
    sistEndretAvNavIdent VARCHAR(7) NOT NULL,
    sistEndretTidspunkt TIMESTAMP NOT NULL,
    arbeidstid TEXT,
    fysisk TEXT,
    arbeidshverdagen TEXT,
    utfordringerMedNorsk TEXT
);
