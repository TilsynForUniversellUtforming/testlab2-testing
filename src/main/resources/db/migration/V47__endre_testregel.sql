create table testregeltema (
  id serial primary key,
  namn varchar(255) not null
);

alter table testregel
rename column type to modus,
rename column name to namn,
add column testregelId varchar(255),
add column versjon int not null default 1,
add column status varchar(255) not null default 'publisert',
add column testregeltype varchar(255) not null default 'nett',
add column spraak varchar(5) not null default 'nb',
add column tema int references testregeltema(id),
add column innholdstype varchar(255)