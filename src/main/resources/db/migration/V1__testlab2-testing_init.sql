create table Testregel (
    id serial primary key,
    kravId int, --references Krav(id)
    referanseAct varchar not null,
    kravTilSamsvar text not null,
    type varchar not null
);

create table Maaling (
    id serial primary key,
    namn varchar not null,
    idLoeysing int, -- references Verksemd(id)
    idSak int, -- references Sak(id)
    datoStart date not null,
    datoSlutt date not null
);

create table Test (
    id serial primary key,
    testresultat varchar not null,
    testresultatBeskrivelse varchar not null,
    side varchar not null,
    elementbeskrivelse varchar not null,
    elementHtmlkode varchar not null,
    elementPeikar varchar not null,
    idTestregel int references Testregel(id),
    idMaaling int references Maaling(id)
);

create table Steg (
    id serial primary key,
    spm varchar not null,
    hjelpetekst text not null,
    type varchar not null,
    kilde varchar not null,
    idTestregel int references Testregel(id)
);

create table Ruting (
    id serial primary key,
    svar varchar not null,
    type varchar not null,
    idSteg int references Steg(id),
    rutingSteg int -- references Steg(id)
);