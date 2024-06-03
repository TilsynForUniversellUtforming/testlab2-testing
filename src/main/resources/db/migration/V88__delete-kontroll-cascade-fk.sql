alter table kontroll_loeysing
    drop constraint kontroll_loeysing_kontroll_id_fkey,
    add constraint kontroll_loeysing_kontroll_id_fkey
        foreign key (kontroll_id)
            references kontroll (id)
            on delete cascade;

alter table kontroll_testreglar
    drop constraint kontroll_testreglar_kontroll_id_fkey,
    add constraint kontroll_testreglar_kontroll_id_fkey
        foreign key (kontroll_id)
            references kontroll (id)
            on delete cascade;

alter table kontroll_sideutval
    drop constraint kontroll_sideutval_kontroll_id_fkey,
    add constraint kontroll_sideutval_kontroll_id_fkey
        foreign key (kontroll_id)
            references kontroll (id)
            on delete cascade;

alter table testgrunnlag
    drop constraint testgrunnlag_kontroll_id_fkey,
    add constraint testgrunnlag_kontroll_id_fkey
        foreign key (kontroll_id)
            references kontroll (id)
            on delete cascade;

alter table testresultat
    add constraint testresultat_sideutval_id_fkey
        foreign key (sideutval_id) references kontroll_sideutval (id);