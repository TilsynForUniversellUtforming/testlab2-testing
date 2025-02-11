# Testlab2-testing

## Utvikling

For å komme i gang:

1. Autentiser deg mot docker registry: `az acr login -n acrddtprod`.
2. Start lokale versjoner av eksterne systemer (se under) med `docker compose -f docker-compose.dev.yml up`.
3. Kjør alle testene med `./mvnw test`
4. Start applikasjonen med `./mvnw spring-boot:run`

### Eksterne systemer

Testlab2-testing er avhengig av noen eksterne systemer for å kjøre, og også for å kjøre noen av integrasjonstestene.
Disse systemene er definert i fila docker-compose.dev.yml, og kan kjøres lokalt med
kommandoen `docker compose -f docker-compose.dev.yml up`.

Siden noen av containerene kjører versjon `latest`, er det viktig å oppdatere dem med jevne mellomrom. Det kan gjøres
med `docker compose -f docker-compose.dev.yml pull`, etterfulgt av `docker compose -f docker-compose.dev.yml up`.


#### Oversikt eksterne tjenester
* Postgres
* Azure Storage Datalake Gen2

#### Eksterne komponenter
* Loeysingsregister (java/kotlin-komponent)
* Kravregister (java/kotlin-komponent)
* Rapportvektøy (java/kotlin-komponent/Wordrapportgenerering)
* Autotester (Qualweb-wrapper/ Azure Functions)
* Crawler (Java/Kotlin-komponent / Crawlee-wrapper)

### Lokale properties

`application.yml` importerer en optional fil som heter `dev.properties`, som også blir ignorert av git. Der kan
vi legge hemmeligheter eller andre verdier som ikke skal sjekkes inn.

Properties som må finnes for at applikasjonen skal fungere, er definert i `application.yml`, men med tom verdi.
For eksempel `autotester.code`. 

### Struktur
* Aggregering: Generering og prosessering av aggregerte datasett
* Eksternresultat: Eksponering av resultat til eksterne tjenester
* Forenkletkontroll: Utføring av automatisk testing
* Inngaendekontroll : Utføring av manuell testing
* Kontroll: Klasser knytt til oppretting av kontroll
* Krav: Integrasjon med kravregister
* Loeysing: Integrasjon med loeysingsregister
* Regelsett: Klassar knytt til oppretting av regelsett
* Resultat: Uthenting av resultat til resultatvisninger
* Security: Spring Security konfigurasjon som er i tillegg til security-lib
* Sideutval.Crawling: Klassar knytt til automatisk og manuelt sideutval
* Styringsdata: Klassar knytt til styringsdata
* Testing.Automatisktesting: Integrasjon med autotester
* Testregel: Klassar knytt til testregler
* Wordrapport: Integrasjon med Rapportverktøy

  ### Auth
  * https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
  
  
