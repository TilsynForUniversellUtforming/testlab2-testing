create table testlab2_testing.testresultatv2
(
    id                         serial primary key,
    testregel_id               integer   not null,
    loeysing_id                integer   not null,
    sideutval_id               integer   not null,
    test_utfoert               timestamp not null,
    element_utfall             varchar(255),
    element_resultat           varchar(100),
    element_omtale_pointer     varchar(255),
    elment_omtale_html         text,
    element_omtale_description text,
    brukarid                   integer   not null,
    testgrunnlag_id            integer,
    maaling_id                 integer
);