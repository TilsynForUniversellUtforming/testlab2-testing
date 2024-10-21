alter table rapport
add constraint unique_kontroll_loeysing_id unique (kontroll_id, loeysing_id);