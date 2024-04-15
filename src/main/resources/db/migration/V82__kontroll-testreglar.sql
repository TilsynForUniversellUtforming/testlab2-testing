alter table kontroll
    add column regelsett_id int null;

create table kontroll_testreglar
(
    kontroll_id  int references kontroll (id),
    testregel_id int references testregel (id)
);