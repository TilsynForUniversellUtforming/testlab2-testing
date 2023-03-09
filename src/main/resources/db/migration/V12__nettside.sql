create table nettside
(
    id               serial primary key,
    crawlresultat_id integer references crawlresultat (id),
    url              text
)