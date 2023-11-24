create table nettside
(
    id          serial primary key,
    type        text,
    url         text,
    beskrivelse text,
    begrunnelse text
);

create table sak_loeysing_nettside
(
    sak_id      int references sak (id),
    loeysing_id int,
    nettside_id int references nettside (id)
);