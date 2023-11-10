# Testlab2-testing

## Utvikling

For å komme i gang:

1. Start lokale versjoner av eksterne systemer (se under) med `docker compose -f docker-compose.dev.yml up`.
2. Kjør alle testene med `./mvnw test`
3. Start applikasjonen med `./mvnw spring-boot:run`

### Eksterne systemer

Testlab2-testing er avhengig av noen eksterne systemer for å kjøre, og også for å kjøre noen av integrasjonstestene.
Disse systemene er definert i fila docker-compose.dev.yml, og kan kjøres lokalt med
kommandoen `docker compose -f docker-compose.dev.yml up`.

Siden noen av containerene kjører versjon `latest`, er det viktig å oppdatere dem med jevne mellomrom. Det kan gjøres
med `docker compose -f docker-compose.dev.yml pull`, etterfulgt av `docker compose -f docker-compose.dev.yml up`.

### Lokale properties

`application.properties` importerer en optional fil som heter `dev.properties`, som også blir ignorert av git. Der kan
vi legge hemmeligheter eller andre verdier som ikke skal sjekkes inn.

Properties som må finnes for at applikasjonen skal fungere, er definert i `application.properties`, men med tom verdi.
For eksempel `autotester.code`. 