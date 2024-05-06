create table sideutval_type
(
    id   serial primary key,
    type varchar
);

insert into sideutval_type (type)
values ('Forside');
insert into sideutval_type (type)
values ('Egendefinert');
insert into sideutval_type (type)
values ('Logg inn');
insert into sideutval_type (type)
values ('Nettstadkart');
insert into sideutval_type (type)
values ('Kontaktinformasjon');
insert into sideutval_type (type)
values ('Hjelpeside');
insert into sideutval_type (type)
values ('Juridisk informasjon');
insert into sideutval_type (type)
values ('Tjenesteområde');
insert into sideutval_type (type)
values ('Sidemal');
insert into sideutval_type (type)
values ('Nedlastbart dokument');
insert into sideutval_type (type)
values ('Andre relevante sider');
insert into sideutval_type (type)
values ('Prosess');

create table kontroll_sideutval
(
    kontroll_id         int references kontroll (id),
    sideutval_type_id   int references sideutval_type (id),
    loeysing_id         int,
    egendefinert_objekt varchar,
    url                 varchar,
    begrunnelse         varchar
)