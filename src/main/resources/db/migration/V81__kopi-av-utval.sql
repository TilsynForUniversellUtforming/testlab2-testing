-- legger til kolonner i kontroll, som skal holde på en kopi av utvalget som er valgt
alter table kontroll
    add column utval_id       int,
    add column utval_namn     text,
    add column utval_oppretta timestamp with time zone;