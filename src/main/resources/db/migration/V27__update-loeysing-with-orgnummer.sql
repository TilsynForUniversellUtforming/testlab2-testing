-- legger til orgnummer for Digdir og UUTilsynet
update loeysing
set orgnummer = '991825827'
where namn = 'Digdir'
   or namn = 'UUTilsynet'