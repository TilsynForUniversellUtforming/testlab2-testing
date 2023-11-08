begin;

alter table utval_loeysing
    drop constraint utval_loeysing_loeysing_id_fkey;

alter table crawlresultat
    drop constraint crawlresultat_loeysingid_fkey;

alter table testkoeyring
    drop constraint testkoeyring_loeysing_id_fkey;

commit;