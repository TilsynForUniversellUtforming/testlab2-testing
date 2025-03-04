alter table aggregering_side
drop constraint aggregering_side_side_fkey;

alter table aggregering_side
rename column tal_samsvar to tal_element_samsvar;
alter table aggregering_side
rename column tal_brot to tal_element_brot;
alter table aggregering_side
rename column tal_ikkje_forekomst to tal_element_ikkje_forekomst;
alter table aggregering_side
add column side_nivaa int not null default 1,
alter column side type text,
add column tal_element_varsel int not null default 0;

alter table aggregering_suksesskriterium
rename column tal_sider_sider_ikkje_forekomst to tal_sider_ikkje_forekomst;

alter table aggregering_testregel
alter column fleire_suksesskriterium type int[] using array[fleire_suksesskriterium]::int[];