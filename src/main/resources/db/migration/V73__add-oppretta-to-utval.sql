alter table utval
    add column oppretta timestamp with time zone;

update utval
set oppretta = now()
where oppretta is null;

alter table utval
    alter column oppretta set not null;
