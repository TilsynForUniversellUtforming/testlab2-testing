drop table test;

drop table maaling;

alter table aggregering_testregel
    drop constraint aggregering_testregel_maaling_id_fkey,
    add constraint aggregering_testregel_maaling_id_fkey
        foreign key (maaling_id)
            references maalingv1 (id)
            on delete cascade;