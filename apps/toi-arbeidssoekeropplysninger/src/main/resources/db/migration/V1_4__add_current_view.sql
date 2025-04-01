create or replace view nyeste_asr_periode as
select p1.id, p1.aktor_id, p1.periode_startet, p1.periode_avsluttet, p1.behandlet_dato
    from periodemelding p1
    where not exists (select 1
        from periodemelding p2
        where
            p1.aktor_id = p2.aktor_id
            and p2.periode_startet is not null
            and p2.periode_startet > p1.periode_startet
        )
        and p1.periode_startet is not null
;

