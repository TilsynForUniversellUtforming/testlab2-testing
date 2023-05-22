alter table testkoeyring
    add lenker_testa int;


update testkoeyring
set lenker_testa = (select count(*) from testresultat tr where testkoeyring_id = testkoeyring.id)
where testkoeyring.lenker_testa is null;
