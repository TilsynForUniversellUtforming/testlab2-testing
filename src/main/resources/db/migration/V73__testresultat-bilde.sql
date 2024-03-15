create table testresultat_bilde
(
    testresultat_id int,
    bilde           varchar,
    thumbnail       varchar,
    foreign key (testresultat_id) references testresultat (id) on delete cascade
);