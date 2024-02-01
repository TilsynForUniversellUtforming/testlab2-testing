create table testgrunnlag
(
    id                serial primary key,
    sak_id            integer                  not null references sak (id),
    testgruppering_id integer,
    namn              text                     not null,
    utval_id          integer references utval (id),
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