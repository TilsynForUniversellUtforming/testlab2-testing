alter table testkoeyring
    drop constraint testkoeyring_maaling_id_fkey,
    add constraint testkoeyring_maaling_id_fkey
        foreign key (maaling_id)
            references maalingv1 (id)
            on delete cascade;

alter table crawlresultat
    drop constraint crawlresultat_maaling_id_fkey,
    add constraint crawlresultat_maaling_id_fkey
        foreign key (maaling_id)
            references maalingv1 (id)
            on delete cascade;

delete
from maalingloeysing
where not exists(select m.id
                 from maalingv1 m
                          join maalingloeysing ml on m.id = ml.idmaaling);

alter table maalingloeysing
    add constraint maalingloeysing_idmaaling_fkey
        foreign key (idmaaling)
            references maalingv1 (id)
            on delete cascade;

alter table testresultat
    drop constraint testresultat_testkoeyring_id_fkey,
    add constraint testresultat_testkoeyring_id_fkey
        foreign key (testkoeyring_id)
            references testkoeyring (id)
            on delete cascade;