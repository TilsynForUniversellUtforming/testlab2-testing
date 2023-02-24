alter table crawlresultat
    add column sist_oppdatert timestamp with time zone;

update crawlresultat
set sist_oppdatert = '2023-02-24 00:00:00 CEST';

alter table crawlresultat
    alter column sist_oppdatert set not null;