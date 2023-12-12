alter table testresultat_ik_svar
    add constraint testresultat_ik_id_steg_unique unique (testresultat_ik_id, steg);