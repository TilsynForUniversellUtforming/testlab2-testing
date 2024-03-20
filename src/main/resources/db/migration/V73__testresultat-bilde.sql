create table testresultat_bilde
(
    id              serial primary key,
    testresultat_id int,
    bilde           varchar,
    thumbnail       varchar,
    opprettet       timestamptz,
    foreign key (testresultat_id) references testresultat (id) on delete cascade,
    unique (testresultat_id, bilde, thumbnail)
);