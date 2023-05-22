update testkoeyring
set lenker_testa = (select count(distinct tr.nettside) from testresultat tr where tr.testkoeyring_id = testkoeyring.id)
where testkoeyring.lenker_testa is null;