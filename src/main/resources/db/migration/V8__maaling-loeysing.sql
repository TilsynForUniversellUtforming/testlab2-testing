alter table maalingv1 drop column url;

create table Loeysing (
    id serial primary key,
    namn varchar,
    url varchar
);

insert into Loeysing (namn, url) values ('UUTilsynet', 'https://www.uutilsynet.no/'), ('Digdir', 'https://www.digdir.no/');

create table CrawlResultat (
   id serial primary key,
   loeysingId int not null references Loeysing(id),
   status varchar not null,
   nettstader varchar[]
);

create table MaalingLoeysing (
    idMaaling int,
    idLoeysing int
);