alter table sak
    add column namn text;
update sak
set namn = sak.virksomhet
where sak.namn is null;
alter table sak
    alter column namn set not null;