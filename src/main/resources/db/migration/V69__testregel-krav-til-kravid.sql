alter table testregel
    add column krav_id int not null default 1;

alter table testregel
    alter column krav drop not null;

-- Gjøres manuelt

-- update testregel t
-- set krav_id = (select k.id
--                from testlab2_krav.wcag2krav k
--                where split_part(t.krav, ' ', 1) = k.suksesskriterium);