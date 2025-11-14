create table testlab2_testing.testresultatv2
(
    id                         serial primary key,
    testregel_id               integer   not null,
    loeysing_id                integer   not null,
    sideutval_id               integer   not null,
    test_utfoert               timestamp not null,
    element_utfall             varchar(255),
    element_resultat           varchar(100),
    element_omtale_pointer     text,
    elment_omtale_html         text,
    element_omtale_description text,
    brukarid                   integer   not null,
    testgrunnlag_id            integer,
    maaling_id                 integer
);

create index idx_crawlresultat_maalingid on testlab2_testing.crawlresultat (maaling_id);
create index idx_crawl_side_crawlresultatid on testlab2_testing.crawl_side (crawlresultat_id);
create index idx_testresultatv2_maalingid on testlab2_testing.testresultatv2 (maaling_id);
create index idx_testresultatv2_testgrunnlag_id on testlab2_testing.testresultatv2 (testgrunnlag_id);
