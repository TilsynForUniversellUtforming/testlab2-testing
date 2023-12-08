create table aggregering_testregel
(
    id   serial primary key,
    maalingId int references maalingv1 (id),
    loeysingId int,
    suksesskriterium varchar,
    fleireSuksesskriterium varchar[],
    testregelId int references testregel (id),
    talElementSamsvar int,
    talElementBrot int,
    talElementVarsel int,
    talElementIkkjeForekomst int,
    talSiderSamsvar int,
    talSiderBrot int,
    talSiderIkkjeForekomst int,
    testregelGjennomsnittlegSideBrotProsent double precision,
    testregelGjennomsnittlegSideSamsvarProsent double precision
);

create table aggregering_side(
                                 id serial primary key,
                                 maalingId int references maalingv1 (id),
                                 loeysingId int,
                                 side int references crawlresultat (id),
                                 gjennomsnittligBruddProsentTR int,
                                 talSamsvar int,
                                 talBrot int,
                                 talIkkjeForekomst int
);

create table aggregering_suksesskriterium(
                                             id serial primary key,
                                             maalingId int references maalingv1 (id),
                                             loeysingId int,
                                             suksesskriteriumId varchar,
                                             talSiderSamsvar int,
                                             talSiderBrot int,
                                             talSiderSiderIkkjeForekomst int
);

alter table nettside
    add column sideNivaa int;
