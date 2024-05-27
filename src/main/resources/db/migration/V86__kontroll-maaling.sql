alter table kontroll
    add column kontrolltype text not null default 'InngaaendeKontroll';

alter table maalingv1
    add column kontrollId int null;