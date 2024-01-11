CREATE TABLE IF NOT EXISTS testobjekt(
    id serial primary key,
    testobjekt text
);

CREATE TABLE IF NOT EXISTS tema (
    id serial primary key,
    tema text)
;


INSERT INTO testobjekt(testobjekt) VALUES ('Alt innhald');
INSERT INTO testobjekt(testobjekt) VALUES ('Alt tekstinnhald');
INSERT INTO testobjekt(testobjekt) VALUES ('Betjeningskomponentar');
INSERT INTO testobjekt(testobjekt) VALUES ('Bilde og grafikk');
INSERT INTO testobjekt(testobjekt) VALUES ('CAPTCHA');
INSERT INTO testobjekt(testobjekt) VALUES ('Iframe');
INSERT INTO testobjekt(testobjekt) VALUES ('Innhald med tidsavgrensing');
INSERT INTO testobjekt(testobjekt) VALUES ('Innhald som blinkar og/eller oppdaterer automatisk');
INSERT INTO testobjekt(testobjekt) VALUES ('Kjeldekode');
INSERT INTO testobjekt(testobjekt) VALUES ('Lenker');
INSERT INTO testobjekt(testobjekt) VALUES ('Liste');
INSERT INTO testobjekt(testobjekt) VALUES ('Lyd og video');
INSERT INTO testobjekt(testobjekt) VALUES ('Overskrift');
INSERT INTO testobjekt(testobjekt) VALUES ('Sidetittel');
INSERT INTO testobjekt(testobjekt) VALUES ('Skjema');
INSERT INTO testobjekt(testobjekt) VALUES ('Statusmelding');
INSERT INTO testobjekt(testobjekt) VALUES ('Tabell');


INSERT INTO tema(tema) VALUES ('Alternativt format');
INSERT INTO tema(tema) VALUES ('Kontrast');
INSERT INTO tema(tema) VALUES ('Navigasjon');
INSERT INTO tema(tema) VALUES ('Presentasjon');
INSERT INTO tema(tema) VALUES ('Skjema');
INSERT INTO tema(tema) VALUES ('Struktur');
INSERT INTO tema(tema) VALUES ('Styring av lyd');
INSERT INTO tema(tema) VALUES ('Tastaturbetjening');
