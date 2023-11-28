create table sak_testregel
(
    sak_id       int references sak (id),
    testregel_id int references testregel (id)
)