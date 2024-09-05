alter table styringsdata
    rename to styringsdata_loeysing;
alter table styringsdata_bot
    rename to styringsdata_loeysing_bot;
alter table styringsdata_klage
    rename to styringsdata_loeysing_klage;
alter table styringsdata_paalegg
    rename to styringsdata_loeysing_paalegg;

create table styringsdata_kontroll
(
    id                           serial primary key,
    kontroll_id                  int     not null,
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