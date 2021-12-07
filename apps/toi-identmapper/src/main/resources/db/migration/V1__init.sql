CREATE TABLE identmapping (
    aktor_id VARCHAR(20),
    fnr VARCHAR(20),
    cachet_tidspunkt TIMESTAMP NOT NULL,
    CONSTRAINT uq_fnr_aktor_id UNIQUE(aktor_id, fnr)
);
