create table if not exists innhaldstype_testing (
id serial primary key,
innhaldstype text);

insert into innhaldstype_testing (innhaldstype) values ('Bilde og grafikk');
insert into innhaldstype_testing (innhaldstype) values ('Captcha');
insert into innhaldstype_testing (innhaldstype) values ('Heile nettsida');
insert into innhaldstype_testing (innhaldstype) values ('Iframe');
insert into innhaldstype_testing (innhaldstype) values ('Innhald med tidsavgrensing');
insert into innhaldstype_testing (innhaldstype) values ('Innhald som blinkar og/eller oppdaterer automatisk');
insert into innhaldstype_testing (innhaldstype) values ('Kjeldekode');
insert into innhaldstype_testing (innhaldstype) values ('Lenke');
insert into innhaldstype_testing (innhaldstype) values ('Liste');
insert into innhaldstype_testing (innhaldstype) values ('Lyd og video');
insert into innhaldstype_testing (innhaldstype) values ('Overskrift');
insert into innhaldstype_testing (innhaldstype) values ('Skjema');
insert into innhaldstype_testing (innhaldstype) values ('Skjemaelement');
insert into innhaldstype_testing (innhaldstype) values ('Statusmelding');
insert into innhaldstype_testing (innhaldstype) values ('Sveiping');
insert into innhaldstype_testing (innhaldstype) values ('Tabell');
insert into innhaldstype_testing (innhaldstype) values ('Tastatur');
insert into innhaldstype_testing (innhaldstype) values ('Tekst');

alter table testregel
add column innhaldstype_testing int references innhaldstype_testing(id);
