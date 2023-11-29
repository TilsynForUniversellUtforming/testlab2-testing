alter table regelsett
    add column type text;

alter table regelsett
    add column standard boolean;

alter table regelsett
    add column aktiv boolean;

alter table regelsetttestregel
    rename to regelsett_testregel;

alter table regelsett_testregel
    rename column idregelsett to regelsett_id;

alter table regelsett_testregel
    rename column idtestregel to testregel_id;