create table testgrunnlag
(
    id                serial primary key,
    sak_id            integer references sak (id),
    namn              text                     not null,
    type              text                     not null default 'OPPRINNELEG_TEST',
    dato_oppretta     timestamp with time zone not null default now()
);

create table testgrunnlag_loeysing_nettside
(
    testgrunnlag_id integer references testgrunnlag (id) on delete cascade,
    loeysing_id     int,
    nettside_id     int references nettside (id)
);

create table testgrunnlag_testregel
(
    testgrunnlag_id integer references testgrunnlag (id) on delete cascade,
    testregel_id    int references testregel (id)
);

alter table testresultat
add column testgrunnlag_id integer references testgrunnlag (id) on delete cascade;

alter table aggregering_testregel drop constraint aggregering_testregel_testgrunnlag_id_fkey;
--alter table aggregering_testregel add constraint aggregering_testregel_testgrunnlag_id_fkey foreign key (testgrunnlag_id) references testgrunnlag (id) on delete cascade;-->

alter table aggregering_side drop constraint aggregering_side_testgrunnlag_id_fkey;
--alter table aggregering_side add constraint aggregering_side_testgrunnlag_id_fkey foreign key (testgrunnlag_id) references testgrunnlag (id) on delete cascade;

alter table aggregering_suksesskriterium drop constraint aggregering_suksesskriterium_testgrunnlag_id_fkey;
--alter table aggregering_suksesskriterium add constraint aggregering_suksesskriterium_testgrunnlag_id_fkey foreign key (testgrunnlag_id) references testgrunnlag (id) on delete cascade;