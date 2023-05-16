ALTER TABLE fritatt DROP COLUMN sending_aktivert_status;
ALTER TABLE fritatt DROP COLUMN forsoktsendt_aktivert;
ALTER TABLE fritatt DROP COLUMN sending_deaktivert_status;
ALTER TABLE fritatt DROP COLUMN forsoktsendt_deaktivert;
CREATE TABLE sendingstatus
(
    fnr            VARCHAR(11)              NOT NULL,
    status         TEXT                     NOT NULL,
    opprettet_rad  TIMESTAMP WITH TIME ZONE NOT NULL
);