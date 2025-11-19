create index idx_crawlresultat_maalingid on testlab2_testing.crawlresultat (maaling_id);
create index idx_crawl_side_crawlresultatid on testlab2_testing.crawl_side (crawlresultat_id);
create index idx_testresultatv2_maalingid on testlab2_testing.testresultat (maaling_id);
create index idx_testresultatv2_testgrunnlag_id on testlab2_testing.testresultat (testgrunnlag_id);


alter table testresultat
add column crawl_side_id integer references testlab2_testing.crawl_side(id);

alter table testresultat
add column maaling_id integer references testlab2_testing.maalingv1(id);

alter table testresultat
add column element_omtale_html text;

alter table testresultat
add column element_omtale_pointer text;
