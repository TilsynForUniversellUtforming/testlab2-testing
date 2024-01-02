alter table testregel
rename column type to modus;

alter table testregel
rename column name to namn;

alter  table testregel
add column testregel_id text,
add column versjon int not null default 1,
add column status text not null default 'publisert',
add column dato_sist_endra  date not null default now(),
add column testregeltype text not null default 'nett',
add column spraak text not null default 'nb',
add column tema int,
add column type text not null default 'nett',
add column testobjekt int,
add column krav_til_samsvar text;