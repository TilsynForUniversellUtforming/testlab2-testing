insert into innhaldstype_testing (innhaldstype)
values ('Forside');

insert into innhaldstype_testing (innhaldstype)
values ('Egendefinert');

create table kontroll_sideutval
(
    kontroll_id              int references kontroll (id),
    innhaldstype_id          int references innhaldstype_testing (id),
    loeysing_id              int,
    innhaldstype_beskrivelse varchar,
    url                      varchar,
    beskrivelse              varchar
)