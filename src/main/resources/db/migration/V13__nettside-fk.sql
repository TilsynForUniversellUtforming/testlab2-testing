alter table Nettside
    drop constraint nettside_crawlresultat_id_fkey,
    add constraint nettside_crawlresultat_id_fkey
        foreign key (crawlresultat_id)
            references crawlresultat (id)
            on delete cascade;