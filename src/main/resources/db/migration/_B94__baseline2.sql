create sequence nettside_id_seq
    as integer;

create sequence nettside_id_seq1
    as integer;
create sequence testresultat_ik_id_seq
    as integer;

create sequence testresultat_ik_svar_id_seq
    as integer;

create sequence styringsdata_paalegg_id_seq
    as integer;

create sequence styringsdata_klage_id_seq
    as integer;

create sequence styringsdata_bot_id_seq
    as integer;

create sequence styringsdata_id_seq
    as integer;

create table "steg"
(
    id          serial
        primary key,
    spm         varchar not null,
    hjelpetekst text    not null,
    type        varchar not null,
    kilde       varchar not null,
    idtestregel integer
);

create table "ruting"
(
    id         serial
        primary key,
    svar       varchar not null,
    type       varchar not null,
    idsteg     integer
        references "steg",
    rutingsteg integer
);

create table "regelsett"
(
    id       serial
        primary key,
    namn     varchar not null,
    modus    text,
    standard boolean,
    aktiv    boolean
);


create table "regelsett_testregel"
(
    regelsett_id integer
        constraint "regelsetttestregel_idtestregel_fkey"
            references "regelsett"
            on delete cascade,
    testregel_id integer
);

create table "utval"
(
    id       serial
        primary key,
    namn     text,
    oppretta timestamp with time zone not null
);


create table "maalingv1"
(
    id         serial
        primary key,
    navn       text                     default ''::text not null,
    status     varchar(50)                               not null,
    max_lenker integer                  default 100      not null,
    tal_lenker integer                  default 30       not null,
    utval_id   integer
        references "utval",
    dato_start timestamp with time zone default '2023-01-01'::date,
    kontrollid integer
);

create table "crawlresultat"
(
    id             serial
        primary key,
    loeysingid     integer                  not null,
    status         varchar                  not null,
    status_url     text,
    maaling_id     serial
        references "maalingv1"
            on delete cascade,
    sist_oppdatert timestamp with time zone not null,
    feilmelding    text,
    lenker_crawla  integer
);


create table "maalingloeysing"
(
    idmaaling  integer
        references "maalingv1"
            on delete cascade,
    idloeysing integer
);

create table "crawl_side"
(
    id               serial primary key,
    crawlresultat_id integer
        constraint "nettside_crawlresultat_id_fkey"
            references "crawlresultat"
            on delete cascade,
    url              text
);


create table "testkoeyring"
(
    id                 serial
        primary key,
    maaling_id         integer
        references "maalingv1"
            on delete cascade,
    loeysing_id        integer,
    status             text                     not null,
    status_url         text,
    sist_oppdatert     timestamp with time zone not null,
    feilmelding        text,
    lenker_testa       integer,
    url_fullt_resultat text,
    url_brot           text,
    url_agg_tr         text,
    url_agg_sk         text,
    url_agg_side       text,
    url_agg_side_tr    text,
    url_agg_loeysing   text,
    brukar_id          integer
);

create table "maaling_testregel"
(
    id           serial
        primary key,
    maaling_id   integer not null
        references "maalingv1"
            on delete cascade,
    testregel_id integer not null
);


create table "utval_loeysing"
(
    utval_id    integer
        references "utval"
            on delete cascade,
    loeysing_id integer
);

create table "nettside"
(
    id           serial
        primary key,
    type        text,
    url         text,
    beskrivelse text,
    begrunnelse text
);


create table "brukar"
(
    id         serial
        primary key,
    brukarnamn text not null
        unique,
    namn       text not null
);

create table "aggregering_side"
(
    id                               serial
        primary key,
    maaling_id                       integer
        references "maalingv1",
    loeysing_id                      integer           not null,
    side                             text,
    gjennomsnittlig_brudd_prosent_tr integer,
    tal_element_samsvar              integer,
    tal_element_brot                 integer,
    tal_element_ikkje_forekomst      integer,
    side_nivaa                       integer default 1 not null,
    tal_element_varsel               integer default 0 not null,
    testgrunnlag_id                  integer
);


create table "aggregering_suksesskriterium"
(
    id                        serial
        primary key,
    maaling_id                integer
        references "maalingv1",
    loeysing_id               integer not null,
    suksesskriterium_id       integer not null,
    tal_sider_samsvar         integer,
    tal_sider_brot            integer,
    tal_sider_ikkje_forekomst integer,
    testgrunnlag_id           integer
);

create table "testobjekt"
(
    id         serial
        primary key,
    testobjekt text
);

create table "tema"
(
    id   serial
        primary key,
    tema text
);

create table "innhaldstype_testing"
(
    id           serial
        primary key,
    innhaldstype text
);

create table "testregel"
(
    id                   serial
        primary key,
    krav                 varchar,
    testregel_schema     varchar                                            not null,
    namn                 text,
    modus                text,
    testregel_id         text,
    versjon              integer                  default 1                 not null,
    status               text                     default 'publisert'::text not null,
    dato_sist_endra      timestamp with time zone default now()             not null,
    spraak               text                     default 'nb'::text        not null,
    tema                 integer,
    type                 text                     default 'nett'::text      not null,
    testobjekt           integer,
    krav_til_samsvar     text,
    innhaldstype_testing integer
        references "innhaldstype_testing",
    krav_id              integer                  default 1                 not null
);


create table "aggregering_testregel"
(
    id                                             serial
        primary key,
    maaling_id                                     integer
        references "maalingv1"
            on delete cascade,
    loeysing_id                                    integer not null,
    suksesskriterium                               integer not null,
    fleire_suksesskriterium                        text,
    testregel_id                                   integer
        references "testregel",
    tal_element_samsvar                            integer,
    tal_element_brot                               integer,
    tal_element_varsel                             integer,
    tal_element_ikkje_forekomst                    integer,
    tal_sider_samsvar                              integer,
    tal_sider_brot                                 integer,
    tal_sider_ikkje_forekomst                      integer,
    testregel_gjennomsnittleg_side_brot_prosent    double precision,
    testregel_gjennomsnittleg_side_samsvar_prosent double precision,
    testgrunnlag_id                                integer
);


create table "kontroll"
(
    id             serial
        primary key,
    tittel         text                                                        not null,
    saksbehandler  text                                                        not null,
    sakstype       text                                                        not null,
    arkivreferanse text                                                        not null,
    utval_id       integer,
    utval_namn     text,
    utval_oppretta timestamp with time zone,
    regelsett_id   integer,
    kontrolltype   text                     default 'InngaaendeKontroll'::text not null,
    oppretta_dato  timestamp with time zone default CURRENT_TIMESTAMP
);


create table "testresultat_bilde"
(
    id              serial
        primary key,
    testresultat_id integer,
    bilde           varchar,
    thumbnail       varchar,
    opprettet       timestamp with time zone,
    unique (testresultat_id, bilde, thumbnail)
);

create table "testgrunnlag"
(
    id            serial
        primary key,
    namn          text                                                      not null,
    type          text                     default 'OPPRINNELEG_TEST'::text not null,
    dato_oppretta timestamp with time zone default now()                    not null,
    kontroll_id   integer
        references "kontroll"
            on delete cascade
);
create table "testgrunnlag_loeysing_nettside"
(
    testgrunnlag_id integer
        references "testgrunnlag"
            on delete cascade,
    loeysing_id     integer,
    nettside_id     integer
        references "nettside"
            on delete cascade
);


create table "testgrunnlag_testregel"
(
    testgrunnlag_id integer
        references "testgrunnlag"
            on delete cascade,
    testregel_id    integer
        references "testregel"
);

create table "kontroll_loeysing"
(
    kontroll_id integer not null
        references "kontroll"
            on delete cascade,
    loeysing_id integer not null,
    primary key (kontroll_id, loeysing_id)
);



create table "kontroll_testreglar"
(
    kontroll_id  integer
        references "kontroll"
            on delete cascade,
    testregel_id integer
        references "testregel"
);

create table "sideutval_type"
(
    id   serial
        primary key,
    type varchar
);



create table "kontroll_sideutval"
(
    kontroll_id         integer
        references "kontroll"
            on delete cascade,
    sideutval_type_id   integer
        references "sideutval_type",
    loeysing_id         integer,
    egendefinert_objekt varchar,
    url                 varchar,
    begrunnelse         varchar,
    id                  serial
        primary key
);


create table "testresultat"
(
    id            serial
        primary key,
    loeysing_id       integer                                                                                       not null,
    testregel_id      integer
        constraint "testresultat_ik_testregel_id_fkey"
            references "testregel",
    nettside_id       integer,
    element_omtale    text,
    element_resultat  text,
    element_utfall    text,
    test_vart_utfoert timestamp with time zone,
    brukar_id         integer
        references "brukar",
    status            text                     default 'IkkjePaabegynt'::text                                       not null,
    kommentar         text,
    testgrunnlag_id   integer
        references "testgrunnlag"
            on delete cascade,
    sideutval_id      integer
        references "kontroll_sideutval",
    sist_lagra        timestamp with time zone default '2024-06-17 00:00:00+00'::timestamp with time zone
);


create table "testresultat_svar"
(
    id serial primary key,
    testresultat_id integer
        references "testresultat"
            on delete cascade,
    steg            text                                                                              not null,
    svar            text                                                                              not null,
    constraint "testresultat_ik_id_steg_unique"
        unique (testresultat_id, steg)
);

create table "testgrunnlag_sideutval_kontroll"
(
    testgrunnlag_id integer
        references "testgrunnlag"
            on delete cascade,
    sideutval_id    integer
        references "kontroll_sideutval"
            on delete cascade
);


create table "testgrunnlag_testregel_kontroll"
(
    testgrunnlag_id integer
        references "testgrunnlag"
            on delete cascade,
    testregel_id    integer
        references "testregel"
);

create table "styringsdata_loeysing_paalegg"
(
    id  serial primary key,
    vedtak_dato timestamp                                                                         not null,
    frist       timestamp
);



create table "styringsdata_loeysing_klage"
(
    id serial primary key,
    klage_type                 varchar                                                                         not null,
    klage_mottatt_dato         timestamp                                                                       not null,
    klage_avgjort_dato         timestamp,
    resultat_klage_tilsyn      varchar,
    klage_dato_departement     timestamp,
    resultat_klage_departement varchar
);


create table "styringsdata_loeysing_bot"
(
    id                 serial primary key,
    beloep_dag         integer                                                                       not null,
    oeking_etter_dager integer                                                                       not null,
    oekning_type       varchar                                                                       not null,
    oeking_sats        integer                                                                       not null,
    vedtak_dato        timestamp                                                                     not null,
    start_dato         timestamp                                                                     not null,
    slutt_dato         timestamp,
    kommentar          text
);



create table "styringsdata_loeysing"
(
    id    serial          primary key,
    kontroll_id            integer
        constraint "styringsdata_kontroll_id_fkey"
            references "kontroll",
    loeysing_id            integer,
    ansvarleg              varchar                                                                   not null,
    oppretta               timestamp                                                                 not null,
    frist                  timestamp                                                                 not null,
    reaksjon               varchar                                                                   not null,
    paalegg_reaksjon       varchar                                                                   not null,
    paalegg_klage_reaksjon varchar                                                                   not null,
    bot_reaksjon           varchar                                                                   not null,
    bot_klage_reaksjon     varchar                                                                   not null,
    paalegg_id             integer
        constraint "styringsdata_paalegg_id_fkey"
            references "styringsdata_loeysing_paalegg",
    paalegg_klage_id       integer
        constraint "styringsdata_paalegg_klage_id_fkey"
            references "styringsdata_loeysing_klage",
    bot_id                 integer
        constraint "styringsdata_bot_id_fkey"
            references "styringsdata_loeysing_bot",
    bot_klage_id           integer
        constraint "styringsdata_bot_klage_id_fkey"
            references "styringsdata_loeysing_klage",
    sist_lagra             timestamp with time zone                                                  not null
);

create table "styringsdata_kontroll"
(
    id                           serial
        primary key,
    kontroll_id                  integer not null,
    ansvarleg                    varchar not null,
    oppretta                     timestamp,
    frist                        timestamp,
    varsel_sendt_dato            timestamp,
    status                       varchar,
    foerebels_rapport_sendt_dato timestamp,
    svar_foerebels_rapport_dato  timestamp,
    endelig_rapport_dato         timestamp,
    kontroll_avslutta_dato       timestamp,
    rapport_publisert_dato       timestamp,
    sist_lagra                   timestamp
);


create table "rapport"
(
    testgrunnlag_id integer
        constraint "fk_rapport_kontroll_id"
            references "kontroll"
        constraint "fk_rapport_testgrunnlag_id"
            references "testgrunnlag",
    id_ekstern      varchar(16)
        constraint "unique_id_ekstern"
            unique,
    loeysing_id     integer,
    publisert       timestamp with time zone,
    maaling_id      integer
        references "maalingv1",
    constraint "unique_kontroll_loeysing_id"
        unique (testgrunnlag_id, loeysing_id),
    constraint "unique_testgrunnlag_loeysing_id"
        unique (testgrunnlag_id, loeysing_id),
    constraint "unique_maaling_loeysing_id"
        unique (maaling_id, loeysing_id)
);


