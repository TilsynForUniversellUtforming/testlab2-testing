alter table testresultat_svar
    drop constraint testresultat_ik_svar_testresultat_ik_id_fkey,
    add constraint testresultat_svar_testresultat_id_fkey
        foreign key (testresultat_id)
            references testresultat (id)
            on delete cascade;

alter table sak_loeysing_nettside
    drop constraint sak_loeysing_nettside_sak_id_fkey,
    add constraint sak_loeysing_nettside_sak_id_fkey
        foreign key (sak_id)
            references sak (id)
            on delete cascade;

alter table testresultat
    drop constraint testresultat_ik_sak_id_fkey,
    add constraint testresultat_sak_id_fkey
        foreign key (sak_id)
            references sak (id)
            on delete cascade;