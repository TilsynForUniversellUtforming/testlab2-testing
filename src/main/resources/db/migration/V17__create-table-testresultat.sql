drop table testregel cascade;

CREATE TABLE testregel
(
    id            SERIAL PRIMARY KEY,
    act_referanse TEXT NOT NULL
);

CREATE TABLE element_utfall
(
    id     serial primary key,
    utfall text not null
);

CREATE TABLE element_resultat
(
    id       serial primary key,
    resultat text not null
);

CREATE TABLE testresultat
(
    id                SERIAL PRIMARY KEY,
    maaling_id        INTEGER REFERENCES maaling (id),
    loeysing_id       INTEGER REFERENCES loeysing (id),
    testregel_id      INTEGER REFERENCES testregel (id),
    element_utfall_id INTEGER REFERENCES element_utfall (id),
    element_result_id INTEGER REFERENCES element_resultat (id),
    side_nivaa        INTEGER,
    test_vart_utfoert TIMESTAMP,
    pointer           TEXT,
    html_code         TEXT
);

CREATE TABLE suksesskriterium
(
    id      serial primary key,
    wcag_id text not null
);

CREATE TABLE testregel_suksesskriterium
(
    testregel_id        integer references testregel (id),
    suksesskriterium_id integer references suksesskriterium (id)
);
