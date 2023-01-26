# Testlab2-testing

## Utvikling

### Lokale properties

`application.properties` importerer en optional fil som heter `dev.properties`, som også blir ignorert av git. Der kan
vi legge hemmeligheter eller andre verdier som ikke skal sjekkes inn.

Properties som må finnes for at applikasjonen skal fungere, er definert i `application.properties`, men med tom verdi.
For eksempel `autotester.code`. 