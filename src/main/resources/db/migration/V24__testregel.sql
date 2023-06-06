create table testregel
(
    id             serial primary key,
    krav           varchar not null,
    referanseAct   varchar not null,
    kravtilsamsvar varchar not null
);

insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('2.4.2 Sidetitler', 'QW-ACT-R1', 'HTML Page has a title');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('3.1.1 Språk på siden', 'QW-ACT-R2', 'HTML page has lang attribute');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('3.1.1 Språk på siden', 'QW-ACT-R3', 'HTML lang and xml:lang match');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('2.2.1 Justerbar hastighet', 'QW-ACT-R4', 'Meta element has no refresh delay');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('3.1.1 Språk på siden', 'QW-ACT-R5', 'Validity of HTML Lang attribute');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R6', 'Image button has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.4 Visningsretning', 'QW-ACT-R7', 'Orientation of the page is not restricted using CSS transform property');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R11', 'Button has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R12', 'Link has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R13', 'Element with aria-hidden has no focusable content');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.4.4 Endring av tekststørrelse', 'QW-ACT-R14', 'Meta viewport does not prevent zoom');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R16', 'Form control has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.1.1 Ikke-tekstlig innhold', 'QW-ACT-R17', 'Image has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.1 Parsing (oppdeling)', 'QW-ACT-R18', 'Id attribute value is unique');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R19', 'Iframe element has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R20', 'Role attribute has valid value');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.1.1 Ikke-tekstlig innhold', 'QW-ACT-R21', 'SVG element with explicit role has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('3.1.2 Språk på deler av innhold', 'QW-ACT-R22', 'Element within body has valid lang attribute');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.5 Identifiser formål med inndata', 'QW-ACT-R24', 'autocomplete attribute has valid value');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R28', 'Element with role attribute has required states and properties');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('2.5.3 Ledetekst i navn', 'QW-ACT-R30', 'Visible label is part of accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R33', 'ARIA required context role');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R35', 'Heading has accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R36',
        'Headers attribute specified on a cell refers to cells in the same table element');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.4.3 Kontrast (minimum)', 'QW-ACT-R37', 'Text has minimum contrast');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R38', 'ARIA required owned elements');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.3.1 Informasjon og relasjoner', 'QW-ACT-R39', 'All table header cells have assigned data cells');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.1.1 Ikke-tekstlig innhold', 'QW-ACT-R42', 'Object element has non-empty accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('2.1.1 Tastatur', 'QW-ACT-R43', 'Scrollable element is keyboard accessible');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R65', 'Element with presentational children has no focusable content');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('4.1.2 Navn, rolle, verdi', 'QW-ACT-R66', 'Menuitem has non-empty accessible name');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.4.12 Tekstavstand', 'QW-ACT-R67', 'Letter spacing in style attributes is not !important');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.4.12 Tekstavstand', 'QW-ACT-R68', 'Line height in style attributes is not !important');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('1.4.12 Tekstavstand', 'QW-ACT-R69', 'Word spacing in style attributes is not !important');
insert into testregel (krav, referanseAct, kravtilsamsvar)
values ('2.1.1 Tastatur', 'QW-ACT-R70', 'Frame with negative tabindex has no interactive elements');

create table maaling_testregel
(
    id           serial primary key,
    maaling_id   integer not null,
    testregel_id integer not null,
    FOREIGN KEY (maaling_id) REFERENCES maalingv1 (id) ON DELETE CASCADE
);

INSERT INTO maaling_testregel (testregel_id, maaling_id)
SELECT testregel.id, maalingv1.id
FROM testregel
         CROSS JOIN maalingv1;