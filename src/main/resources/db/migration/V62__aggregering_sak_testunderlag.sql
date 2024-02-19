alter table aggregering_testregel
add column testgrunnlag_id int references sak(id);

alter table aggregering_side
add column testgrunnlag_id int references sak(id);

alter table aggregering_suksesskriterium
add column testgrunnlag_id int references sak(id);

update testregel
set krav = split_part(krav, ' ', 1);