create table testkoeyring
(
    id             serial primary key,
    maaling_id     int references maalingv1 (id),
    loeysing_id    int references loeysing (id),
    status         text                     not null,
    status_url     text                     not null,
    sist_oppdatert timestamp with time zone not null,
    feilmelding    text
)