alter table testregel
    add column name text;

alter table testregel
    add column type text;

update testregel
set name = testregelnoekkel || ' ' || kravtilsamsvar,
    type = 'forenklet';

alter table testregel
    rename column testregelnoekkel to testregel_schema;

alter table testregel
    drop column kravtilsamsvar;