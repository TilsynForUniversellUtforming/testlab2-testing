drop table testregel cascade;

CREATE TABLE testresultat
(
    id                SERIAL PRIMARY KEY,
    testkoeyring_id   INTEGER REFERENCES testkoeyring (id),
    nettside          text,
    suksesskriterium  text,
    testregel         text,
    element_utfall    text,
    element_resultat  text,
    side_nivaa        INTEGER,
    test_vart_utfoert TIMESTAMP,
    pointer           TEXT,
    html_code         TEXT
);