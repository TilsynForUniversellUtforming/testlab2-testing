alter table rapport
add column maaling_id int references maalingv1;

alter table rapport
add constraint unique_testgrunnlag_loeysing_id unique (testgrunnlag_id, loeysing_id);

alter table rapport
add constraint unique_maaling_loeysing_id unique (maaling_id, loeysing_id);
