create table periodemelding (
   id bigserial primary key, -- autoid - ikke id til periodemeldingen
   periode_id uuid not null,
   identitetsnummer varchar(255) null, -- fnr eller dnr
   periode_startet timestamp with time zone null,
   periode_avsluttet timestamp with time zone null,
   periode_mottatt_dato timestamp with time zone null,

   opplysninger_mottatt_dato timestamp with time zone null,
   behandlet_dato timestamp with time zone null,
   helsetilstand_hindrer_arbeid boolean null,
   andre_forhold_hindrer_arbeid boolean null
);
create unique index IF NOT EXISTS periode_id_idx on periodemelding(periode_id);
create index IF NOT EXISTS behandlet_dato_idx on periodemelding(behandlet_dato);
create index IF NOT EXISTS periode_mottatt_dato_idx on periodemelding(periode_mottatt_dato);
create index IF NOT EXISTS opplysninger_mottatt_dato_idx on periodemelding(opplysninger_mottatt_dato);
