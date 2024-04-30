insert into testobjekt (testobjekt)
values ('Forside');

insert into testobjekt (testobjekt)
values ('Egendefinert');

create table kontroll_sideutval
(
    kontroll_id         int references kontroll (id),
    testobjekt_id       int references testobjekt (id),
    loeysing_id         int,
    egendefinert_objekt varchar,
    url                 varchar,
    begrunnelse         varchar
)