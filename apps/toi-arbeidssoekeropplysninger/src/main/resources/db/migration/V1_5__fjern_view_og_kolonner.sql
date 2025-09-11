drop view nyeste_asr_periode;
alter table periodemelding drop column opplysninger_mottatt_dato;
alter table periodemelding drop column helsetilstand_hindrer_arbeid;
alter table periodemelding drop column andre_forhold_hindrer_arbeid;

