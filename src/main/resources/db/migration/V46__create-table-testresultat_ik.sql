create table testresultat_ik
(
    id                serial primary key,
    sak_id            int references sak (id),
    loeysing_id       int not null,
    testregel_id      int references testregel (id),
    nettside_id       int references nettside (id),
    element_omtale    text,
    element_resultat  text,
    element_utfall    text,
    test_vart_utfoert timestamp with time zone
);

create table testresultat_ik_svar
(
    id                 serial primary key,
    testresultat_ik_id int references testresultat_ik (id),
    steg               text not null,
    svar               text not null
);