alter table rapport
rename column testgrunnlag_id to kontroll_id;

alter table rapport
    add constraint fk_rapport_kontroll_id foreign key (kontroll_id) references kontroll (id);
alter table rapport
    drop constraint rapport_testgrunnlag_id_fkey;