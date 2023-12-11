create table aggregering_testregel
(
    id   serial primary key,
    maaling_id int references maalingv1 (id),
    loeysing_id int,
    suksesskriterium varchar,
    fleire_suksesskriterium varchar[],
    testregel_id int references testregel (id),
    tal_element_samsvar int,
    tal_element_brot int,
    tal_element_varsel int,
    tal_element_ikkje_forekomst int,
    tal_sider_samsvar int,
    tal_sider_brot int,
    tal_sider_ikkje_forekomst int,
    testregel_gjennomsnittleg_side_brot_prosent double precision,
    testregel_gjennomsnittleg_side_samsvar_prosent double precision
);

create table aggregering_side(
    id serial primary key,
    maaling_id int references maalingv1 (id),
    loeysing_id int,
    side int references crawlresultat (id),
    gjennomsnittlig_brudd_prosent_tr int,
    tal_samsvar int,
    tal_brot int,
    tal_ikkje_forekomst int
    );

create table aggregering_suksesskriterium(
    id serial primary key,
    maaling_id int references maalingv1 (id),
    loeysing_id int,
    suksesskriterium_id varchar,
    tal_sider_samsvar int,
    tal_sider_brot int,
    tal_sider_sider_ikkje_forekomst int
);
