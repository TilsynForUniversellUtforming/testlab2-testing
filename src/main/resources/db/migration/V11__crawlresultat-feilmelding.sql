alter table crawlresultat
    add column feilmelding text;

update crawlresultat
set feilmelding = 'feilmelding har ikke blitt lagret'
where feilmelding = ''
   or feilmelding is null;