create table utval
(
    id   serial primary key,
    namn text
);

create table utval_loeysing
(
    utval_id    int references utval (id) on delete cascade,
    loeysing_id int references loeysing (id)
);