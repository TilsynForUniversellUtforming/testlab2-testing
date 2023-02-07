alter table testregel alter column referanseact drop not null;

create table Regelsett (
    id serial primary key,
    namn varchar not null
);

create table RegelsettTestregel (
    idRegelsett int references Regelsett(id),
    idTestregel int references testregel(id)
);