create table styringsdata_paalegg
(
    id          serial primary key,
    vedtak_dato timestamp not null,
    frist       timestamp
);

create table styringsdata_klage
(
    id                         serial primary key,
    klage_type                 varchar   not null,
    klage_mottatt_dato         timestamp not null,
    klage_avgjort_dato         timestamp,
    resultat_klage_tilsyn      varchar,
    klage_dato_departement     timestamp,
    resultat_klage_departement varchar
);

create table styringsdata_bot
(
    id                 serial primary key,
    beloep_dag         int       not null,
    oeking_etter_dager int       not null,
    oekning_type       varchar   not null,
    oeking_sats        int       not null,
    vedtak_dato        timestamp not null,
    start_dato         timestamp not null,
    slutt_dato         timestamp,
    kommentar          text
);

create table styringsdata
(
    id               serial primary key,
    kontroll_id      int references kontroll (id),
    loeysing_id      int,
    ansvarleg        varchar   not null,
    oppretta         timestamp not null,
    frist            timestamp not null,
    reaksjon         varchar   not null,
    paalegg_id       int references styringsdata_paalegg (id),
    paalegg_klage_id int references styringsdata_klage (id),
    bot_id           int references styringsdata_bot (id),
    bot_klage_id     int references styringsdata_klage (id)
);