alter table crawlresultat
    add column status_url text;

alter table crawlresultat
    add column maaling_id serial references maalingv1 (id);