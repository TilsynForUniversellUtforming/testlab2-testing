create table rapport
(
    testgrunnlag_id int references testgrunnlag (id),
    id_ekstern      varchar(16) default substring(md5(random()::text), 1, 16),
    loeysing_id     int,
    publisert       timestamp with time zone
);

alter table rapport
    add constraint unique_id_ekstern unique (id_ekstern);
