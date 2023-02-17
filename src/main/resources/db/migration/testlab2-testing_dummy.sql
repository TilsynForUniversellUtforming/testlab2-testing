drop table if exists testregel_temp;

create temporary table testregel_temp (
    krav varchar,
    referanseAct varchar not null,
    kravTilSamsvar text not null,
    type varchar not null,
    status varchar
);

insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', '1.1.1a', 'Bilde har tekstalternativ', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', '1.1.1b', 'Formål med lenka bilde går fram av lenketekst og/eller tekstalternativ', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', '1.1.1c', 'Formål med klikkbare område i bilde, går fram av tekstalternativ', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', '1.1.1d', 'CAPTCHA har tekstalternativ og alternativ form', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.1 Bare lyd og bare video (forhåndsinnspilt)', '1.2.1a', 'Førehandsinnspelt lyd har alternativ i form av tekst', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.1 Bare lyd og bare video (forhåndsinnspilt)', '1.2.1b', 'Førehandsinnspelt video utan lyd, har alternativ i form av tekst eller lyd', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.2 Teksting (forhåndsinnspilt)', '1.2.2a', 'Førehandsinnspelt video med lyd, er teksta', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.5 Synstolking (forhåndsinnpilt)', '1.2.5a', 'Synstolking', 'Web', 'Ikkje starta');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', '1.3.1a', 'Overskrifter er rett koda', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', '1.3.1b', 'Tabellar og overskriftsceller er rett koda', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', '1.3.1c', 'Lister er rett koda', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.2 Meningsfylt rekkefølge', '1.3.2a', 'Meiningsfylt leserekkefølge er ivareteken i koden.', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.3 Sensoriske egenskaper', '1.3.3a', 'Instruksjonar for betening av skjema, er ikkje utelukkande avhengige av sensoriske eigenskapar', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.4 Visningsretning', '1.3.4a', 'Visningsretning-2022', 'Web', 'Utgår');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.10 Dynamisk tilpasning (Reflow)', '1.4.10a', 'Dynamisk tilpasning av nettsider', 'Web', 'Under arbeid');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.11 Kontrast for ikke-tekstlig innhold', '1.4.11a', 'Kontrast for ikkje-tekstleg innhald', 'Web', 'Under arbeid');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', '1.4.1a', 'Lenka tekst skil seg frå annan tekst med meir enn berre farge', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', '1.4.1b', 'Informasjon i grafiske framstillingar skil seg ut med meir enn berre farge', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', '1.4.1c', 'Skjemaelement og feilmeldingar er merka med meir enn berre farge', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.2 Styring av lyd', '1.4.2a', 'Det er mogleg å styre lyd som startar automatisk og ikkje stoppar innan 3 sekund', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.3 Kontrast (minimum)', '1.4.3a', 'Det er tilstrekkeleg kontrast mellom tekst og bakgrunn', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.3 Kontrast (minimum)', '1.4.3b', 'Det er tilstrekkeleg kontrast mellom tekst og bakgrunn i bilde av tekst', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.3 Kontrast (minimum)', '1.4.3c', 'Det er tilstrekkeleg kontrast mellom tekst og bakgrunn', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.4 Endring av tekststørrelse', '1.4.4a', 'Tekst kan forstørrast til minst 200 % utan tap av innhald og funksjonalitet', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.5 Bilder av tekst', '1.4.5a', 'Bilde av tekst er ikkje brukt unødvendig ', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.1.1 Tastatur', '2.1.1a', 'Det er mogleg å nå og betene innhald med tastatur', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.1.2 Ingen tastaturfelle', '2.1.2a', 'Det finst ingen tastaturfeller på nettsida', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.1.4 Hurtigtaster som består av ett tegn', '2.1.4a', 'Hurtigtastar som består av eit tegn', 'Web', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.2.1 Justerbar hastighet', '2.2.1a', 'Det er mogleg å slå av, justere eller forlenge tidsavgrensingar', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.2.2 Pause, stopp, skjul', '2.2.2a', 'Det er mogleg å pause, stoppe eller skjule innhald som bevegar seg, blinkar eller rullar', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.2.2 Pause, stopp, skjul', '2.2.2b', 'Det er mogleg å pause, stoppe, skjule eller endre oppdateringsfrekvensen for automatisk oppdatert innhald', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.3.1 Terskelverdi på maksimalt tre glimt', '2.3.1a', 'Nettsida har ikkje innhald som glimtar', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.1 Hoppe over blokker', '2.4.1a', 'Det finst ein mekanisme for å hoppe til hovudinnhaldet på nettsida', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.2 Sidetitler ', '2.4.2a', 'Nettsida har beskrivande sidetittel', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.3 Fokusrekkefølge ', '2.4.3a', 'Tastaturekkefølgje ivaretek meiningsinnhald og betening', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.4 Formål med lenke (i kontekst)', '2.4.4a', 'Formål med lenker går fram av lenketeksten, eller lenketeksten og konteksten saman', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.5 Flere måter', '2.4.5a', 'Det er fleire måtar å navigere på', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.6 Overskrifter og ledetekster', '2.4.6a', 'Overskrifter beskriv innhaldet', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.6 Overskrifter og ledetekster', '2.4.6b', 'Ledetekstar beskriv skjemaelement ', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.7 Synlig fokus', '2.4.7a', 'Innhald som kan betenast med tastatur, får synleg fokusmarkering', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.5.3 Ledetekst i navn', '2.5.3a', 'Ledetekst i namn', 'Web', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.1.1 Språk på siden', '3.1.1a', 'Hovudspråket på nettsida er rett koda', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.1.2 Språk på deler av innhold', '3.1.2a', 'Innhald på anna språk enn hovudspråket, er rett koda', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.2.1 Fokus', '3.2.1a', 'Fokus ved tastaturnavigasjon, gir ikkje kontekstendring', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.2.2 Inndata', '3.2.2a', 'Endra innstilling i skjemaelement fører ikkje til kontekstendring', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.2.3 Konsekvent navigering ', '3.2.3a', 'Navigasjonselement i blokker blir gjentekne i konsekvent rekkefølgje', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.2.4 Konsekvent identifikasjon', '3.2.4a', 'Søkefunksjonen har lik visuell utforming og identifikasjon i koden', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.1 Identifikasjon av feil', '3.3.1a', 'Skjema gir feilmelding viss tomme obligatoriske skjemafelt blir oppdaga automatisk', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.1 Identifikasjon av feil', '3.3.1b', 'Skjema gir feilmelding viss feil inndata blir oppdaga automatisk', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.2 Ledetekster eller instruksjoner', '3.3.2a', 'Skjemaelement har instruksjon eller ledetekst', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.3 Forslag ved feil', '3.3.3a', 'Skjema gir forslag til retting, viss feil i inndata blir oppdaga automatisk ', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.4 Forhindring av feil (juridiske feil, økonomiske feil, datafeil)', '3.3.4a', 'Skjema med viktig formål let brukaren kontrollere og endre informasjon, eller angre innsending', 'Web', 'Treng avklaring');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.4 Forhindring av feil (juridiske feil, økonomiske feil, datafeil)', '3.3.4a-2018', '3.3.4a Skjema med viktig formål let brukaren kontrollere og endre informasjon, eller angre innsending (2018)', 'Web', 'Utgår');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.4 Forhindring av feil (juridiske feil, økonomiske feil, datafeil)', '3.3.4b', 'Brukaren kan bekrefte eller angre sletting av lagra informasjon', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.1 Parsing (oppdeling)', '4.1.1a', 'Koden inneheld ikkje syntaksfeil', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', '4.1.2a', 'Skjemaelement er identifiserte i koden', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', '4.1.2b', 'Knappar er identifiserte i koden', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', '4.1.2c', 'Iframe er identifiserte i koden', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.3 Statusbeskjeder', '4.1.3a', 'Statusmeldingar', 'Web', 'Under arbeid');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', 'app-1.1.1a', 'App 1.1.1a Bilde har alt-tekst som beskriv formålet', 'App', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.4 Visningsretning', 'app-1.3.4a', 'App 1.3.4a Visningsretning', 'App', 'Ferdig testa');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.3 Kontrast (minimum)', 'app-1.4.3a', 'App 1.4.3a Det er tilstrekkeleg kontrast mellom tekst og bakgrunn', 'App', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.1.1 Tastatur', 'app-2.1.1a', 'App 2.1.1a Nå alt med sveiping', 'App', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.1.2 Ingen tastaturfelle', 'app-2.1.2a', 'App 2.1.2a Det finst ingen sveipefeller i skjermbildet', 'App', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.1 Identifikasjon av feil', 'app-3.3.1a', 'App 3.3.1a Feilmelding til tomme obligatoriske skjemaelement', 'App', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.2 Ledetekster eller instruksjoner', 'app-3.3.2a', 'App 3.3.2a Skjemaelement er identifisert ved hjelp av instruksjonar eller ledetekstar', 'App', 'Under arbeid');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'app-4.1.2a', 'App 4.1.2a Interaktive element er identifisert', 'App', 'Klar for testing');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.4 Visningsretning', 'app-1.3.4a', 'App-1.3.4a Visningsretning 2023', 'App', 'Klar for kvalitetssikring');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', 'app-1.4.1a', 'App-1.4.1a Lenka tekst skil seg frå annan tekst med meir enn berre farge - 2023', 'App', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', 'app-1.4.1b', 'App-1.4.1b Informasjon i grafiske framstillingar skil seg ut med meir enn berre farge - 2023', 'App', 'Klar for kvalitetssikring');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', 'app-1.4.1c', 'App-1.4.1c Skjemaelement, instruksjonar og feilmeldingar er merka med meir enn berre farge - 2023', 'App', 'Klar for kvalitetssikring');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.5 Identifiser formål med inndata', 'app-android-1.3.5a', 'App-Android-1.3.5a Identifiser formål med inndata ', 'App', 'Under arbeid');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.5 Identifiser formål med inndata', 'app-iOS-1.3.5a', 'App-iOS-1.3.5a Identifiser formål med inndata ', 'App', 'Under arbeid');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.4 Visningsretning', 'nett-1.3.4a ', 'Nett-1.3.4a Visningsretning 2023', 'Web', 'Ikkje starta');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.12 Tekstavstand', 'nett-1.4.12a ', 'Nett-1.4.12a Det er tilstrekkelig tekstavstand', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', 'nett-1.4.1a', 'Nett-1.4.1a Lenka tekst skil seg frå annan tekst med meir enn berre farge - 2023', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', 'nett-1.4.1b', 'Nett-1.4.1b Informasjon i grafiske framstillingar skil seg ut med meir enn berre farge - 2023', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.1 Bruk av farge', 'nett-1.4.1c', 'Nett-1.4.1c Skjemaelement, instruksjonar eller feilmeldingar er formidla med meir enn berre farge - 2023', 'Web', 'Klar for kvalitetssikring');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.2 Sidetitler ', 'QW-ACT-R1', 'HTML Page has a title', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.1.1 Språk på siden', 'QW-ACT-R2', 'HTML page has lang attribute', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.1.1 Språk på siden', 'QW-ACT-R3', 'HTML lang and xml:lang match', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.2.1 Justerbar hastighet', 'QW-ACT-R4', 'Meta element has no refresh delay', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.1.1 Språk på siden', 'QW-ACT-R5', 'Validity of HTML Lang attribute', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R6', 'Image button has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.4 Visningsretning', 'QW-ACT-R7', 'Orientation of the page is not restricted using CSS transform property', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R9', 'Links with identical accessible names have equivalent purpose', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R10', 'Iframe elements with identical accessible names have equivalent purpose', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R11', 'Button has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R12', 'Link has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R13', 'Element with aria-hidden has no focusable content', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.4 Endring av tekststørrelse', 'QW-ACT-R14', 'Meta viewport does not prevent zoom', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.2 Styring av lyd', 'QW-ACT-R15', 'Audio or video has no audio that plays automatically', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R16', 'Form control has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', 'QW-ACT-R17', 'Image has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.1 Parsing (oppdeling)', 'QW-ACT-R18', 'Id attribute value is unique', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R19', 'Iframe element has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R20', 'Role attribute has valid value', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', 'QW-ACT-R21', 'SVG element with explicit role has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.1.2 Språk på deler av innhold', 'QW-ACT-R22', 'Element within body has valid lang attribute', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.3 Synstolking eller mediealternativ', 'QW-ACT-R23', 'Video element visual content has accessible alternative', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.5 Identifiser formål med inndata', 'QW-ACT-R24', 'autocomplete attribute has valid value', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R25', 'ARIA state or property is permitted', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.2 Teksting (forhåndsinnspilt)', 'QW-ACT-R26', 'Video element auditory content has accessible alternative', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R27', 'ARIA attribute is defined in WAI-ARIA', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R28', 'Element with role attribute has required states and properties', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.1 Bare lyd og bare video (forhåndsinnspilt)', 'QW-ACT-R29', 'Audio element content has text alternative', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.5.3 Ledetekst i navn', 'QW-ACT-R30', 'Visible label is part of accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.1 Bare lyd og bare video (forhåndsinnspilt)', 'QW-ACT-R31', 'Video element visual-only content has accessible alternative', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.2.5 Synstolking (forhåndsinnpilt)', 'QW-ACT-R32', 'Video element visual content has strict accessible alternative', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R33', 'ARIA required context role', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R34', 'ARIA state or property has valid value', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R35', 'Heading has accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R36', 'Headers attribute specified on a cell refers to cells in the same table element', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.3 Kontrast (minimum)', 'QW-ACT-R37', 'Text has minimum contrast', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R38', 'ARIA required owned elements', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R39', 'All table header cells have assigned data cells', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.4 Endring av tekststørrelse', 'QW-ACT-R40', 'Zoomed text node is not clipped with CSS overflow', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('3.3.1 Identifikasjon av feil', 'QW-ACT-R41', 'Error message describes invalid form field value', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.1.1 Ikke-tekstlig innhold', 'QW-ACT-R42', 'Object element has non-empty accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.1.1 Tastatur', 'QW-ACT-R43', 'Scrollable element is keyboard accessible', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.4 Formål med lenke (i kontekst)', 'QW-ACT-R44', 'Links with identical accessible names and context serve equivalent purpose', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R48', 'Element marked as decorative is not exposed', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R49', 'Audio or video that plays automatically has no audio that lasts more than 3 seconds', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R50', 'Audio or video that plays automatically has a control mechanism', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R51', 'Video element visual-only content is media alternative for text', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R52', 'Video element visual-only content has description track', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R53', 'Video element visual-only content has transcript', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R54', 'Video element visual-only content has audio track alternative', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R55', 'Video element visual content has audio description', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R56', 'Video element content is media alternative for text', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R57', 'Video element visual content has description track', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R58', 'Audio element content has transcript', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R59', 'Audio Element Content Is Media Alternative For Text', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R60', 'Video element auditory content has captions', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R61', 'Video element visual content has transcript', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.7 Synlig fokus', 'QW-ACT-R62', 'Element in sequential focus order has visible focus', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R63', 'Document has a landmark with non-repeated content', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R64', 'Document has heading for non-repeated content', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R65', 'Element with presentational children has no focusable content', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R66', 'Menuitem has non-empty accessible name', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.12 Tekstavstand', 'QW-ACT-R67', 'Letter spacing in style attributes is not !important', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.12 Tekstavstand', 'QW-ACT-R68', 'Line height in style attributes is not !important', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('1.4.12 Tekstavstand', 'QW-ACT-R69', 'Word spacing in style attributes is not !important', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.1.1 Tastatur', 'QW-ACT-R70', 'Frame with negative tabindex has no interactive elements', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R71', 'Meta element has no refresh delay (no exception)', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R72', 'First focusable element is link to non-repeated content', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R73', 'Block of repeated content is collapsible', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R74', 'Document has an instrument to move focus to non-repeated content', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values ('2.4.1 Hoppe over blokker', 'QW-ACT-R75', 'Bypass Blocks of Repeated Content', 'Web', 'Publisert');
insert into testregel_temp (krav, referanseact, kravtilsamsvar, type, status) values (null, 'QW-ACT-R76', 'Text has enhanced contrast (AAA)', 'Web', 'Publisert');


insert into testregel (kravid, referanseact, kravtilsamsvar, type, status)
select k.id, case when substr(referanseact, 1, 2) = 'QW'then referanseact end as referanseact, t.kravtilsamsvar, t.type, t.status from testregel_temp t
    left join testlab2_krav.krav k on t.krav = k.tittel;

drop table if exists testregel_temp;

insert into regelsett (namn) values ('Standard');

insert into regelsetttestregel (idregelsett, idtestregel)
select currval('regelsett_id_seq') as idregelsett, t.id as idtestregel
from testregel t
where t.referanseact is not null;

insert into Loeysing (namn, url) values
    ('Harstad Kommune', 'https://www.harstad.kommune.no/'),
    ('Spring', 'https://spring.io/');