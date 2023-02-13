alter table MaalingV1
    add column status varchar(50);

update MaalingV1
set status = 'ikke_startet';

alter table MaalingV1
    alter column status set not null;