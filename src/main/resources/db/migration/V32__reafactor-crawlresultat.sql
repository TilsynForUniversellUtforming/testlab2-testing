update crawlresultat
set status = replace(status, 'ikke_ferdig', 'ikkje_starta');

update crawlresultat
set status = replace(status, 'feilet', 'feila');