alter table testgrunnlag_loeysing_nettside
drop constraint testgrunnlag_loeysing_nettside_nettside_id_fkey,
add constraint testgrunnlag_loeysing_nettside_nettside_id_fkey foreign key (nettside_id) references nettside(id) on delete cascade;