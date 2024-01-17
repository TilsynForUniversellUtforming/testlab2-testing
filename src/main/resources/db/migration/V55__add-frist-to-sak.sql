alter table sak
    add frist date;
update sak
set frist = '2024-06-30'
where frist is null;
alter table sak
    alter column frist set not null;