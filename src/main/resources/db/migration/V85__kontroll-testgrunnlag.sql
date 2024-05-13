alter table kontroll_sideutval
    add id serial primary key;

alter table testgrunnlag
    alter column sak_id drop not null;
alter table testgrunnlag
    add column kontroll_id integer references kontroll (id) null;

create table testgrunnlag_sideutval_kontroll
(
    testgrunnlag_id int references testgrunnlag (id) on delete cascade,
    sideutval_id    int references kontroll_sideutval (id) on delete cascade
);

create table testgrunnlag_testregel_kontroll
(
    testgrunnlag_id integer references testgrunnlag (id) on delete cascade,
    testregel_id    int references testregel (id)
);

alter table testresultat
    drop constraint testresultat_ik_nettside_id_fkey;

alter table testresultat
    add column sideutval_id int null;