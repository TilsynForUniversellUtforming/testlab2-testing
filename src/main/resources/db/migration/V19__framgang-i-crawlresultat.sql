alter table crawlresultat
    add lenker_crawla int;

update crawlresultat
set lenker_crawla = (select max_links_per_page from maalingv1 where id = crawlresultat.maaling_id)
where crawlresultat.lenker_crawla is null;