create table kontroll_loeysing
(
    kontroll_id int not null references kontroll (id),
    loeysing_id int not null,
    primary key (kontroll_id, loeysing_id)
)