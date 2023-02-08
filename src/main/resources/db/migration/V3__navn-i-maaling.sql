alter table MaalingV1
    add column navn text not null default '';
update MaalingV1
set navn = url
where navn = '';
