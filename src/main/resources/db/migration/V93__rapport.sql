alter table testgrunnlag
    drop column sak_id;

create table rapport
(
    testgrunnlag_id int references testgrunnlag (id),
    id_ekstern      varchar(16) default substring(md5(random()::text), 1, 16),
    publisert       timestamp with time zone null
);

alter table rapport
    add constraint unique_id_ekstern unique (id_ekstern);
