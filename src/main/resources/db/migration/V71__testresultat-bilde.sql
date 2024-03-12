create table testresultat_bilde
(
    testresultat_id int,
    bilde_path      varchar,
    thumbnail       boolean,
    foreign key (testresultat_id) references testresultat (id) on delete cascade
);