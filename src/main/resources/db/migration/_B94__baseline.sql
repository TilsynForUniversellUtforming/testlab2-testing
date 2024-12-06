CREATE TABLE aggregering_side (
                                                   id serial primary key,
                                                   maaling_id integer,
                                                   loeysing_id integer NOT NULL,
                                                   side text,
                                                   gjennomsnittlig_brudd_prosent_tr integer,
                                                   tal_element_samsvar integer,
                                                   tal_element_brot integer,
                                                   tal_element_ikkje_forekomst integer,
                                                   side_nivaa integer DEFAULT 1 NOT NULL,
                                                   tal_element_varsel integer DEFAULT 0 NOT NULL,
                                                   testgrunnlag_id integer
);

CREATE TABLE aggregering_suksesskriterium (
                                                               id serial primary key,
                                                               maaling_id integer,
                                                               loeysing_id integer NOT NULL,
                                                               suksesskriterium_id integer NOT NULL,
                                                               tal_sider_samsvar integer,
                                                               tal_sider_brot integer,
                                                               tal_sider_ikkje_forekomst integer,
                                                               testgrunnlag_id integer
);


CREATE TABLE aggregering_testregel (
                                                        id serial primary key,
                                                        maaling_id integer,
                                                        loeysing_id integer NOT NULL,
                                                        suksesskriterium integer NOT NULL,
                                                        fleire_suksesskriterium integer[],
                                                        testregel_id integer,
                                                        tal_element_samsvar integer,
                                                        tal_element_brot integer,
                                                        tal_element_varsel integer,
                                                        tal_element_ikkje_forekomst integer,
                                                        tal_sider_samsvar integer,
                                                        tal_sider_brot integer,
                                                        tal_sider_ikkje_forekomst integer,
                                                        testregel_gjennomsnittleg_side_brot_prosent double precision,
                                                        testregel_gjennomsnittleg_side_samsvar_prosent double precision,
                                                        testgrunnlag_id integer
);


CREATE TABLE brukar (
                                         id serial primary key,
                                         brukarnamn text NOT NULL,
                                         namn text NOT NULL
);






CREATE SEQUENCE brukar_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE brukar_id_seq OWNED BY brukar.id;






CREATE TABLE crawl_side (
                                             id serial primary key,
                                             crawlresultat_id integer,
                                             url text
);






CREATE TABLE crawlresultat (
                                                id serial primary key,
                                                loeysingid integer NOT NULL,
                                                status character varying NOT NULL,
                                                status_url text,
                                                maaling_id integer NOT NULL,
                                                sist_oppdatert timestamp with time zone NOT NULL,
                                                feilmelding text,
                                                lenker_crawla integer
);






CREATE SEQUENCE crawlresultat_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE crawlresultat_id_seq OWNED BY crawlresultat.id;






CREATE SEQUENCE crawlresultat_maaling_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE crawlresultat_maaling_id_seq OWNED BY crawlresultat.maaling_id;

CREATE TABLE innhaldstype_testing (
                                                       id integer NOT NULL,
                                                       innhaldstype text
);






CREATE SEQUENCE innhaldstype_testing_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE innhaldstype_testing_id_seq OWNED BY innhaldstype_testing.id;






CREATE TABLE kontroll (
                                           id serial primary key,
                                           tittel text NOT NULL,
                                           saksbehandler text NOT NULL,
                                           sakstype text NOT NULL,
                                           arkivreferanse text NOT NULL,
                                           utval_id integer,
                                           utval_namn text,
                                           utval_oppretta timestamp with time zone,
                                           regelsett_id integer,
                                           kontrolltype text DEFAULT 'InngaaendeKontroll'::text NOT NULL,
                                           oppretta_dato timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);






CREATE SEQUENCE kontroll_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE kontroll_id_seq OWNED BY kontroll.id;






CREATE TABLE kontroll_loeysing (
                                                    kontroll_id integer NOT NULL,
                                                    loeysing_id integer NOT NULL
);






CREATE TABLE kontroll_sideutval (
                                                     kontroll_id integer,
                                                     sideutval_type_id integer,
                                                     loeysing_id integer,
                                                     egendefinert_objekt character varying,
                                                     url character varying,
                                                     begrunnelse character varying,
                                                     id integer NOT NULL
);






CREATE SEQUENCE kontroll_sideutval_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE kontroll_sideutval_id_seq OWNED BY kontroll_sideutval.id;






CREATE TABLE kontroll_testreglar (
                                                      kontroll_id integer,
                                                      testregel_id integer
);






CREATE TABLE maaling_testregel (
                                                    id integer NOT NULL,
                                                    maaling_id integer NOT NULL,
                                                    testregel_id integer NOT NULL
);






CREATE SEQUENCE maaling_testregel_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE maaling_testregel_id_seq OWNED BY maaling_testregel.id;






CREATE TABLE maalingloeysing (
                                                  idmaaling integer,
                                                  idloeysing integer
);






CREATE TABLE maalingv1 (
                                            id integer NOT NULL,
                                            navn text DEFAULT ''::text NOT NULL,
                                            status character varying(50) NOT NULL,
                                            max_lenker integer DEFAULT 100 NOT NULL,
                                            tal_lenker integer DEFAULT 30 NOT NULL,
                                            utval_id integer,
                                            dato_start timestamp with time zone DEFAULT '2023-01-01'::date,
                                            kontrollid integer
);






CREATE SEQUENCE maalingv1_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE maalingv1_id_seq OWNED BY maalingv1.id;






CREATE TABLE nettside (
                                           id integer NOT NULL,
                                           type text,
                                           url text,
                                           beskrivelse text,
                                           begrunnelse text
);






CREATE SEQUENCE nettside_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE nettside_id_seq OWNED BY crawl_side.id;






CREATE SEQUENCE nettside_id_seq1
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE nettside_id_seq1 OWNED BY nettside.id;






CREATE TABLE rapport (
                                          testgrunnlag_id integer,
                                          id_ekstern character varying(16) DEFAULT "substring"(md5((random())::text), 1, 16),
                                          loeysing_id integer,
                                          publisert timestamp with time zone,
                                          maaling_id integer
);






CREATE TABLE regelsett (
                                            id integer NOT NULL,
                                            namn character varying NOT NULL,
                                            modus text,
                                            standard boolean,
                                            aktiv boolean
);






CREATE SEQUENCE regelsett_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE regelsett_id_seq OWNED BY regelsett.id;






CREATE TABLE regelsett_testregel (
                                                      regelsett_id integer,
                                                      testregel_id integer
);






CREATE TABLE ruting (
                                         id integer NOT NULL,
                                         svar character varying NOT NULL,
                                         type character varying NOT NULL,
                                         idsteg integer,
                                         rutingsteg integer
);






CREATE SEQUENCE ruting_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE ruting_id_seq OWNED BY ruting.id;






CREATE TABLE sak (
                                      id integer NOT NULL,
                                      virksomhet character varying(9) NOT NULL,
                                      loeysingar integer[],
                                      opprettet timestamp with time zone DEFAULT '2023-11-24 07:00:00+00'::timestamp with time zone NOT NULL,
                                      namn text NOT NULL,
                                      ansvarleg integer,
                                      frist date NOT NULL
);






CREATE SEQUENCE sak_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE sak_id_seq OWNED BY sak.id;






CREATE TABLE sak_loeysing_nettside (
                                                        sak_id integer,
                                                        loeysing_id integer,
                                                        nettside_id integer
);






CREATE TABLE sak_testregel (
                                                sak_id integer,
                                                testregel_id integer
);






CREATE TABLE sideutval_type (
                                                 id integer NOT NULL,
                                                 type character varying
);






CREATE SEQUENCE sideutval_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE sideutval_type_id_seq OWNED BY sideutval_type.id;






CREATE TABLE steg (
                                       id integer NOT NULL,
                                       spm character varying NOT NULL,
                                       hjelpetekst text NOT NULL,
                                       type character varying NOT NULL,
                                       kilde character varying NOT NULL,
                                       idtestregel integer
);






CREATE SEQUENCE steg_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE steg_id_seq OWNED BY steg.id;






CREATE TABLE styringsdata_loeysing_bot (
                                                            id integer NOT NULL,
                                                            beloep_dag integer NOT NULL,
                                                            oeking_etter_dager integer NOT NULL,
                                                            oekning_type character varying NOT NULL,
                                                            oeking_sats integer NOT NULL,
                                                            vedtak_dato timestamp without time zone NOT NULL,
                                                            start_dato timestamp without time zone NOT NULL,
                                                            slutt_dato timestamp without time zone,
                                                            kommentar text
);






CREATE SEQUENCE styringsdata_bot_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE styringsdata_bot_id_seq OWNED BY styringsdata_loeysing_bot.id;






CREATE TABLE styringsdata_loeysing (
                                                        id integer NOT NULL,
                                                        kontroll_id integer,
                                                        loeysing_id integer,
                                                        ansvarleg character varying NOT NULL,
                                                        oppretta timestamp without time zone NOT NULL,
                                                        frist timestamp without time zone NOT NULL,
                                                        reaksjon character varying NOT NULL,
                                                        paalegg_reaksjon character varying NOT NULL,
                                                        paalegg_klage_reaksjon character varying NOT NULL,
                                                        bot_reaksjon character varying NOT NULL,
                                                        bot_klage_reaksjon character varying NOT NULL,
                                                        paalegg_id integer,
                                                        paalegg_klage_id integer,
                                                        bot_id integer,
                                                        bot_klage_id integer,
                                                        sist_lagra timestamp with time zone NOT NULL
);






CREATE SEQUENCE styringsdata_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE styringsdata_id_seq OWNED BY styringsdata_loeysing.id;






CREATE TABLE styringsdata_loeysing_klage (
                                                              id integer NOT NULL,
                                                              klage_type character varying NOT NULL,
                                                              klage_mottatt_dato timestamp without time zone NOT NULL,
                                                              klage_avgjort_dato timestamp without time zone,
                                                              resultat_klage_tilsyn character varying,
                                                              klage_dato_departement timestamp without time zone,
                                                              resultat_klage_departement character varying
);






CREATE SEQUENCE styringsdata_klage_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE styringsdata_klage_id_seq OWNED BY styringsdata_loeysing_klage.id;






CREATE TABLE styringsdata_kontroll (
                                                        id integer NOT NULL,
                                                        kontroll_id integer NOT NULL,
                                                        ansvarleg character varying NOT NULL,
                                                        oppretta timestamp without time zone,
                                                        frist timestamp without time zone,
                                                        varsel_sendt_dato timestamp without time zone,
                                                        status character varying,
                                                        foerebels_rapport_sendt_dato timestamp without time zone,
                                                        svar_foerebels_rapport_dato timestamp without time zone,
                                                        endelig_rapport_dato timestamp without time zone,
                                                        kontroll_avslutta_dato timestamp without time zone,
                                                        rapport_publisert_dato timestamp without time zone,
                                                        sist_lagra timestamp without time zone
);






CREATE SEQUENCE styringsdata_kontroll_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE styringsdata_kontroll_id_seq OWNED BY styringsdata_kontroll.id;






CREATE TABLE styringsdata_loeysing_paalegg (
                                                                id integer NOT NULL,
                                                                vedtak_dato timestamp without time zone NOT NULL,
                                                                frist timestamp without time zone
);






CREATE SEQUENCE styringsdata_paalegg_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE styringsdata_paalegg_id_seq OWNED BY styringsdata_loeysing_paalegg.id;






CREATE TABLE tema (
                                       id integer NOT NULL,
                                       tema text
);






CREATE SEQUENCE tema_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE tema_id_seq OWNED BY tema.id;






CREATE TABLE testgrunnlag (
                                               id integer NOT NULL,
                                               sak_id integer,
                                               namn text NOT NULL,
                                               type text DEFAULT 'OPPRINNELEG_TEST'::text NOT NULL,
                                               dato_oppretta timestamp with time zone DEFAULT now() NOT NULL,
                                               kontroll_id integer
);






CREATE SEQUENCE testgrunnlag_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE testgrunnlag_id_seq OWNED BY testgrunnlag.id;






CREATE TABLE testgrunnlag_loeysing_nettside (
                                                                 testgrunnlag_id integer,
                                                                 loeysing_id integer,
                                                                 nettside_id integer
);






CREATE TABLE testgrunnlag_sideutval_kontroll (
                                                                  testgrunnlag_id integer,
                                                                  sideutval_id integer
);






CREATE TABLE testgrunnlag_testregel (
                                                         testgrunnlag_id integer,
                                                         testregel_id integer
);






CREATE TABLE testgrunnlag_testregel_kontroll (
                                                                  testgrunnlag_id integer,
                                                                  testregel_id integer
);






CREATE TABLE testkoeyring (
                                               id integer NOT NULL,
                                               maaling_id integer,
                                               loeysing_id integer,
                                               status text NOT NULL,
                                               status_url text,
                                               sist_oppdatert timestamp with time zone NOT NULL,
                                               feilmelding text,
                                               lenker_testa integer,
                                               url_fullt_resultat text,
                                               url_brot text,
                                               url_agg_tr text,
                                               url_agg_sk text,
                                               url_agg_side text,
                                               url_agg_side_tr text,
                                               url_agg_loeysing text,
                                               brukar_id integer
);






CREATE SEQUENCE testkoeyring_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE testkoeyring_id_seq OWNED BY testkoeyring.id;






CREATE TABLE testobjekt (
                                             id integer NOT NULL,
                                             testobjekt text
);






CREATE SEQUENCE testobjekt_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE testobjekt_id_seq OWNED BY testobjekt.id;






CREATE TABLE testregel (
                                            id integer NOT NULL,
                                            krav character varying,
                                            testregel_schema character varying NOT NULL,
                                            namn text,
                                            modus text,
                                            testregel_id text,
                                            versjon integer DEFAULT 1 NOT NULL,
                                            status text DEFAULT 'publisert'::text NOT NULL,
                                            dato_sist_endra timestamp with time zone DEFAULT now() NOT NULL,
                                            spraak text DEFAULT 'nb'::text NOT NULL,
                                            tema integer,
                                            type text DEFAULT 'nett'::text NOT NULL,
                                            testobjekt integer,
                                            krav_til_samsvar text,
                                            innhaldstype_testing integer,
                                            krav_id integer DEFAULT 1 NOT NULL
);






CREATE SEQUENCE testregel_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE testregel_id_seq OWNED BY testregel.id;






CREATE TABLE testresultat (
                                               id integer NOT NULL,
                                               sak_id integer,
                                               loeysing_id integer NOT NULL,
                                               testregel_id integer,
                                               nettside_id integer,
                                               element_omtale text,
                                               element_resultat text,
                                               element_utfall text,
                                               test_vart_utfoert timestamp with time zone,
                                               brukar_id integer,
                                               status text DEFAULT 'IkkjePaabegynt'::text NOT NULL,
                                               kommentar text,
                                               testgrunnlag_id integer,
                                               sideutval_id integer,
                                               sist_lagra timestamp with time zone DEFAULT '2024-06-17 00:00:00+00'::timestamp with time zone
);






CREATE TABLE testresultat_bilde (
                                                     id integer NOT NULL,
                                                     testresultat_id integer,
                                                     bilde character varying,
                                                     thumbnail character varying,
                                                     opprettet timestamp with time zone
);






CREATE SEQUENCE testresultat_bilde_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE testresultat_bilde_id_seq OWNED BY testresultat_bilde.id;






CREATE SEQUENCE testresultat_ik_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE testresultat_ik_id_seq OWNED BY testresultat.id;






CREATE TABLE testresultat_svar (
                                                    id integer NOT NULL,
                                                    testresultat_id integer,
                                                    steg text NOT NULL,
                                                    svar text NOT NULL
);






CREATE SEQUENCE testresultat_ik_svar_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE testresultat_ik_svar_id_seq OWNED BY testresultat_svar.id;






CREATE TABLE utval (
                                        id integer NOT NULL,
                                        namn text,
                                        oppretta timestamp with time zone NOT NULL
);






CREATE SEQUENCE utval_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;






ALTER SEQUENCE utval_id_seq OWNED BY utval.id;






CREATE TABLE utval_loeysing (
                                                 utval_id integer,
                                                 loeysing_id integer
);






ALTER TABLE ONLY aggregering_side ALTER COLUMN id SET DEFAULT nextval('aggregering_side_id_seq'::regclass);






ALTER TABLE ONLY aggregering_suksesskriterium ALTER COLUMN id SET DEFAULT nextval('aggregering_suksesskriterium_id_seq'::regclass);






ALTER TABLE ONLY aggregering_testregel ALTER COLUMN id SET DEFAULT nextval('aggregering_testregel_id_seq'::regclass);






ALTER TABLE ONLY brukar ALTER COLUMN id SET DEFAULT nextval('brukar_id_seq'::regclass);






ALTER TABLE ONLY crawl_side ALTER COLUMN id SET DEFAULT nextval('nettside_id_seq'::regclass);






ALTER TABLE ONLY crawlresultat ALTER COLUMN id SET DEFAULT nextval('crawlresultat_id_seq'::regclass);






ALTER TABLE ONLY crawlresultat ALTER COLUMN maaling_id SET DEFAULT nextval('crawlresultat_maaling_id_seq'::regclass);






ALTER TABLE ONLY innhaldstype_testing ALTER COLUMN id SET DEFAULT nextval('innhaldstype_testing_id_seq'::regclass);






ALTER TABLE ONLY kontroll ALTER COLUMN id SET DEFAULT nextval('kontroll_id_seq'::regclass);






ALTER TABLE ONLY kontroll_sideutval ALTER COLUMN id SET DEFAULT nextval('kontroll_sideutval_id_seq'::regclass);






ALTER TABLE ONLY maaling_testregel ALTER COLUMN id SET DEFAULT nextval('maaling_testregel_id_seq'::regclass);






ALTER TABLE ONLY maalingv1 ALTER COLUMN id SET DEFAULT nextval('maalingv1_id_seq'::regclass);






ALTER TABLE ONLY nettside ALTER COLUMN id SET DEFAULT nextval('nettside_id_seq1'::regclass);






ALTER TABLE ONLY regelsett ALTER COLUMN id SET DEFAULT nextval('regelsett_id_seq'::regclass);






ALTER TABLE ONLY ruting ALTER COLUMN id SET DEFAULT nextval('ruting_id_seq'::regclass);






ALTER TABLE ONLY sak ALTER COLUMN id SET DEFAULT nextval('sak_id_seq'::regclass);






ALTER TABLE ONLY sideutval_type ALTER COLUMN id SET DEFAULT nextval('sideutval_type_id_seq'::regclass);






ALTER TABLE ONLY steg ALTER COLUMN id SET DEFAULT nextval('steg_id_seq'::regclass);






ALTER TABLE ONLY styringsdata_kontroll ALTER COLUMN id SET DEFAULT nextval('styringsdata_kontroll_id_seq'::regclass);






ALTER TABLE ONLY styringsdata_loeysing ALTER COLUMN id SET DEFAULT nextval('styringsdata_id_seq'::regclass);






ALTER TABLE ONLY styringsdata_loeysing_bot ALTER COLUMN id SET DEFAULT nextval('styringsdata_bot_id_seq'::regclass);






ALTER TABLE ONLY styringsdata_loeysing_klage ALTER COLUMN id SET DEFAULT nextval('styringsdata_klage_id_seq'::regclass);






ALTER TABLE ONLY styringsdata_loeysing_paalegg ALTER COLUMN id SET DEFAULT nextval('styringsdata_paalegg_id_seq'::regclass);






ALTER TABLE ONLY tema ALTER COLUMN id SET DEFAULT nextval('tema_id_seq'::regclass);






ALTER TABLE ONLY testgrunnlag ALTER COLUMN id SET DEFAULT nextval('testgrunnlag_id_seq'::regclass);






ALTER TABLE ONLY testkoeyring ALTER COLUMN id SET DEFAULT nextval('testkoeyring_id_seq'::regclass);






ALTER TABLE ONLY testobjekt ALTER COLUMN id SET DEFAULT nextval('testobjekt_id_seq'::regclass);






ALTER TABLE ONLY testregel ALTER COLUMN id SET DEFAULT nextval('testregel_id_seq'::regclass);






ALTER TABLE ONLY testresultat ALTER COLUMN id SET DEFAULT nextval('testresultat_ik_id_seq'::regclass);






ALTER TABLE ONLY testresultat_bilde ALTER COLUMN id SET DEFAULT nextval('testresultat_bilde_id_seq'::regclass);






ALTER TABLE ONLY testresultat_svar ALTER COLUMN id SET DEFAULT nextval('testresultat_ik_svar_id_seq'::regclass);






ALTER TABLE ONLY utval ALTER COLUMN id SET DEFAULT nextval('utval_id_seq'::regclass);






ALTER TABLE ONLY aggregering_side
    ADD CONSTRAINT aggregering_side_pkey PRIMARY KEY (id);






ALTER TABLE ONLY aggregering_suksesskriterium
    ADD CONSTRAINT aggregering_suksesskriterium_pkey PRIMARY KEY (id);






ALTER TABLE ONLY aggregering_testregel
    ADD CONSTRAINT aggregering_testregel_pkey PRIMARY KEY (id);






ALTER TABLE ONLY brukar
    ADD CONSTRAINT brukar_brukarnamn_key UNIQUE (brukarnamn);






ALTER TABLE ONLY brukar
    ADD CONSTRAINT brukar_pkey PRIMARY KEY (id);






ALTER TABLE ONLY crawlresultat
    ADD CONSTRAINT crawlresultat_pkey PRIMARY KEY (id);


ALTER TABLE ONLY innhaldstype_testing
    ADD CONSTRAINT innhaldstype_testing_pkey PRIMARY KEY (id);






ALTER TABLE ONLY kontroll_loeysing
    ADD CONSTRAINT kontroll_loeysing_pkey PRIMARY KEY (kontroll_id, loeysing_id);






ALTER TABLE ONLY kontroll
    ADD CONSTRAINT kontroll_pkey PRIMARY KEY (id);






ALTER TABLE ONLY kontroll_sideutval
    ADD CONSTRAINT kontroll_sideutval_pkey PRIMARY KEY (id);






ALTER TABLE ONLY maaling_testregel
    ADD CONSTRAINT maaling_testregel_pkey PRIMARY KEY (id);






ALTER TABLE ONLY maalingv1
    ADD CONSTRAINT maalingv1_pkey PRIMARY KEY (id);






ALTER TABLE ONLY crawl_side
    ADD CONSTRAINT nettside_pkey PRIMARY KEY (id);






ALTER TABLE ONLY nettside
    ADD CONSTRAINT nettside_pkey1 PRIMARY KEY (id);






ALTER TABLE ONLY regelsett
    ADD CONSTRAINT regelsett_pkey PRIMARY KEY (id);






ALTER TABLE ONLY ruting
    ADD CONSTRAINT ruting_pkey PRIMARY KEY (id);






ALTER TABLE ONLY sak
    ADD CONSTRAINT sak_pkey PRIMARY KEY (id);






ALTER TABLE ONLY sideutval_type
    ADD CONSTRAINT sideutval_type_pkey PRIMARY KEY (id);






ALTER TABLE ONLY steg
    ADD CONSTRAINT steg_pkey PRIMARY KEY (id);






ALTER TABLE ONLY styringsdata_loeysing_bot
    ADD CONSTRAINT styringsdata_bot_pkey PRIMARY KEY (id);






ALTER TABLE ONLY styringsdata_loeysing_klage
    ADD CONSTRAINT styringsdata_klage_pkey PRIMARY KEY (id);






ALTER TABLE ONLY styringsdata_kontroll
    ADD CONSTRAINT styringsdata_kontroll_pkey PRIMARY KEY (id);






ALTER TABLE ONLY styringsdata_loeysing_paalegg
    ADD CONSTRAINT styringsdata_paalegg_pkey PRIMARY KEY (id);






ALTER TABLE ONLY styringsdata_loeysing
    ADD CONSTRAINT styringsdata_pkey PRIMARY KEY (id);






ALTER TABLE ONLY tema
    ADD CONSTRAINT tema_pkey PRIMARY KEY (id);






ALTER TABLE ONLY testgrunnlag
    ADD CONSTRAINT testgrunnlag_pkey PRIMARY KEY (id);






ALTER TABLE ONLY testkoeyring
    ADD CONSTRAINT testkoeyring_pkey PRIMARY KEY (id);






ALTER TABLE ONLY testobjekt
    ADD CONSTRAINT testobjekt_pkey PRIMARY KEY (id);






ALTER TABLE ONLY testregel
    ADD CONSTRAINT testregel_pkey PRIMARY KEY (id);






ALTER TABLE ONLY testresultat_bilde
    ADD CONSTRAINT testresultat_bilde_pkey PRIMARY KEY (id);






ALTER TABLE ONLY testresultat_bilde
    ADD CONSTRAINT testresultat_bilde_testresultat_id_bilde_thumbnail_key UNIQUE (testresultat_id, bilde, thumbnail);






ALTER TABLE ONLY testresultat_svar
    ADD CONSTRAINT testresultat_ik_id_steg_unique UNIQUE (testresultat_id, steg);






ALTER TABLE ONLY testresultat
    ADD CONSTRAINT testresultat_ik_pkey PRIMARY KEY (id);






ALTER TABLE ONLY testresultat_svar
    ADD CONSTRAINT testresultat_ik_svar_pkey PRIMARY KEY (id);






ALTER TABLE ONLY rapport
    ADD CONSTRAINT unique_id_ekstern UNIQUE (id_ekstern);






ALTER TABLE ONLY rapport
    ADD CONSTRAINT unique_kontroll_loeysing_id UNIQUE (testgrunnlag_id, loeysing_id);






ALTER TABLE ONLY rapport
    ADD CONSTRAINT unique_maaling_loeysing_id UNIQUE (maaling_id, loeysing_id);






ALTER TABLE ONLY rapport
    ADD CONSTRAINT unique_testgrunnlag_loeysing_id UNIQUE (testgrunnlag_id, loeysing_id);






ALTER TABLE ONLY utval
    ADD CONSTRAINT utval_pkey PRIMARY KEY (id);


ALTER TABLE ONLY aggregering_side
    ADD CONSTRAINT aggregering_side_maaling_id_fkey FOREIGN KEY (maaling_id) REFERENCES maalingv1(id);






ALTER TABLE ONLY aggregering_suksesskriterium
    ADD CONSTRAINT aggregering_suksesskriterium_maaling_id_fkey FOREIGN KEY (maaling_id) REFERENCES maalingv1(id);






ALTER TABLE ONLY aggregering_testregel
    ADD CONSTRAINT aggregering_testregel_maaling_id_fkey FOREIGN KEY (maaling_id) REFERENCES maalingv1(id) ON DELETE CASCADE;






ALTER TABLE ONLY aggregering_testregel
    ADD CONSTRAINT aggregering_testregel_testregel_id_fkey FOREIGN KEY (testregel_id) REFERENCES testregel(id);






ALTER TABLE ONLY crawlresultat
    ADD CONSTRAINT crawlresultat_maaling_id_fkey FOREIGN KEY (maaling_id) REFERENCES maalingv1(id) ON DELETE CASCADE;






ALTER TABLE ONLY rapport
    ADD CONSTRAINT fk_rapport_kontroll_id FOREIGN KEY (testgrunnlag_id) REFERENCES kontroll(id);






ALTER TABLE ONLY rapport
    ADD CONSTRAINT fk_rapport_testgrunnlag_id FOREIGN KEY (testgrunnlag_id) REFERENCES testgrunnlag(id);






ALTER TABLE ONLY kontroll_loeysing
    ADD CONSTRAINT kontroll_loeysing_kontroll_id_fkey FOREIGN KEY (kontroll_id) REFERENCES kontroll(id) ON DELETE CASCADE;






ALTER TABLE ONLY kontroll_sideutval
    ADD CONSTRAINT kontroll_sideutval_kontroll_id_fkey FOREIGN KEY (kontroll_id) REFERENCES kontroll(id) ON DELETE CASCADE;






ALTER TABLE ONLY kontroll_sideutval
    ADD CONSTRAINT kontroll_sideutval_sideutval_type_id_fkey FOREIGN KEY (sideutval_type_id) REFERENCES sideutval_type(id);






ALTER TABLE ONLY kontroll_testreglar
    ADD CONSTRAINT kontroll_testreglar_kontroll_id_fkey FOREIGN KEY (kontroll_id) REFERENCES kontroll(id) ON DELETE CASCADE;






ALTER TABLE ONLY kontroll_testreglar
    ADD CONSTRAINT kontroll_testreglar_testregel_id_fkey FOREIGN KEY (testregel_id) REFERENCES testregel(id);






ALTER TABLE ONLY maaling_testregel
    ADD CONSTRAINT maaling_testregel_maaling_id_fkey FOREIGN KEY (maaling_id) REFERENCES maalingv1(id) ON DELETE CASCADE;






ALTER TABLE ONLY maalingloeysing
    ADD CONSTRAINT maalingloeysing_idmaaling_fkey FOREIGN KEY (idmaaling) REFERENCES maalingv1(id) ON DELETE CASCADE;






ALTER TABLE ONLY maalingv1
    ADD CONSTRAINT maalingv1_utval_id_fkey FOREIGN KEY (utval_id) REFERENCES utval(id);






ALTER TABLE ONLY crawl_side
    ADD CONSTRAINT nettside_crawlresultat_id_fkey FOREIGN KEY (crawlresultat_id) REFERENCES crawlresultat(id) ON DELETE CASCADE;






ALTER TABLE ONLY rapport
    ADD CONSTRAINT rapport_maaling_id_fkey FOREIGN KEY (maaling_id) REFERENCES maalingv1(id);






ALTER TABLE ONLY regelsett_testregel
    ADD CONSTRAINT regelsetttestregel_idtestregel_fkey FOREIGN KEY (regelsett_id) REFERENCES regelsett(id) ON DELETE CASCADE;






ALTER TABLE ONLY ruting
    ADD CONSTRAINT ruting_idsteg_fkey FOREIGN KEY (idsteg) REFERENCES steg(id);






ALTER TABLE ONLY sak
    ADD CONSTRAINT sak_ansvarleg_fkey FOREIGN KEY (ansvarleg) REFERENCES brukar(id);






ALTER TABLE ONLY sak_loeysing_nettside
    ADD CONSTRAINT sak_loeysing_nettside_nettside_id_fkey FOREIGN KEY (nettside_id) REFERENCES nettside(id);






ALTER TABLE ONLY sak_loeysing_nettside
    ADD CONSTRAINT sak_loeysing_nettside_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES sak(id) ON DELETE CASCADE;






ALTER TABLE ONLY sak_testregel
    ADD CONSTRAINT sak_testregel_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES sak(id);






ALTER TABLE ONLY sak_testregel
    ADD CONSTRAINT sak_testregel_testregel_id_fkey FOREIGN KEY (testregel_id) REFERENCES testregel(id);






ALTER TABLE ONLY styringsdata_loeysing
    ADD CONSTRAINT styringsdata_bot_id_fkey FOREIGN KEY (bot_id) REFERENCES styringsdata_loeysing_bot(id);






ALTER TABLE ONLY styringsdata_loeysing
    ADD CONSTRAINT styringsdata_bot_klage_id_fkey FOREIGN KEY (bot_klage_id) REFERENCES styringsdata_loeysing_klage(id);






ALTER TABLE ONLY styringsdata_loeysing
    ADD CONSTRAINT styringsdata_kontroll_id_fkey FOREIGN KEY (kontroll_id) REFERENCES kontroll(id);






ALTER TABLE ONLY styringsdata_loeysing
    ADD CONSTRAINT styringsdata_paalegg_id_fkey FOREIGN KEY (paalegg_id) REFERENCES styringsdata_loeysing_paalegg(id);






ALTER TABLE ONLY styringsdata_loeysing
    ADD CONSTRAINT styringsdata_paalegg_klage_id_fkey FOREIGN KEY (paalegg_klage_id) REFERENCES styringsdata_loeysing_klage(id);






ALTER TABLE ONLY testgrunnlag
    ADD CONSTRAINT testgrunnlag_kontroll_id_fkey FOREIGN KEY (kontroll_id) REFERENCES kontroll(id) ON DELETE CASCADE;






ALTER TABLE ONLY testgrunnlag_loeysing_nettside
    ADD CONSTRAINT testgrunnlag_loeysing_nettside_nettside_id_fkey FOREIGN KEY (nettside_id) REFERENCES nettside(id) ON DELETE CASCADE;






ALTER TABLE ONLY testgrunnlag_loeysing_nettside
    ADD CONSTRAINT testgrunnlag_loeysing_nettside_testgrunnlag_id_fkey FOREIGN KEY (testgrunnlag_id) REFERENCES testgrunnlag(id) ON DELETE CASCADE;






ALTER TABLE ONLY testgrunnlag
    ADD CONSTRAINT testgrunnlag_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES sak(id);






ALTER TABLE ONLY testgrunnlag_sideutval_kontroll
    ADD CONSTRAINT testgrunnlag_sideutval_kontroll_sideutval_id_fkey FOREIGN KEY (sideutval_id) REFERENCES kontroll_sideutval(id) ON DELETE CASCADE;






ALTER TABLE ONLY testgrunnlag_sideutval_kontroll
    ADD CONSTRAINT testgrunnlag_sideutval_kontroll_testgrunnlag_id_fkey FOREIGN KEY (testgrunnlag_id) REFERENCES testgrunnlag(id) ON DELETE CASCADE;






ALTER TABLE ONLY testgrunnlag_testregel_kontroll
    ADD CONSTRAINT testgrunnlag_testregel_kontroll_testgrunnlag_id_fkey FOREIGN KEY (testgrunnlag_id) REFERENCES testgrunnlag(id) ON DELETE CASCADE;






ALTER TABLE ONLY testgrunnlag_testregel_kontroll
    ADD CONSTRAINT testgrunnlag_testregel_kontroll_testregel_id_fkey FOREIGN KEY (testregel_id) REFERENCES testregel(id);






ALTER TABLE ONLY testgrunnlag_testregel
    ADD CONSTRAINT testgrunnlag_testregel_testgrunnlag_id_fkey FOREIGN KEY (testgrunnlag_id) REFERENCES testgrunnlag(id) ON DELETE CASCADE;






ALTER TABLE ONLY testgrunnlag_testregel
    ADD CONSTRAINT testgrunnlag_testregel_testregel_id_fkey FOREIGN KEY (testregel_id) REFERENCES testregel(id);






ALTER TABLE ONLY testkoeyring
    ADD CONSTRAINT testkoeyring_maaling_id_fkey FOREIGN KEY (maaling_id) REFERENCES maalingv1(id) ON DELETE CASCADE;






ALTER TABLE ONLY testregel
    ADD CONSTRAINT testregel_innhaldstype_testing_fkey FOREIGN KEY (innhaldstype_testing) REFERENCES innhaldstype_testing(id);






ALTER TABLE ONLY testresultat
    ADD CONSTRAINT testresultat_brukar_id_fkey FOREIGN KEY (brukar_id) REFERENCES brukar(id);






ALTER TABLE ONLY testresultat
    ADD CONSTRAINT testresultat_ik_testregel_id_fkey FOREIGN KEY (testregel_id) REFERENCES testregel(id);






ALTER TABLE ONLY testresultat
    ADD CONSTRAINT testresultat_sak_id_fkey FOREIGN KEY (sak_id) REFERENCES sak(id) ON DELETE CASCADE;






ALTER TABLE ONLY testresultat
    ADD CONSTRAINT testresultat_sideutval_id_fkey FOREIGN KEY (sideutval_id) REFERENCES kontroll_sideutval(id);






ALTER TABLE ONLY testresultat_svar
    ADD CONSTRAINT testresultat_svar_testresultat_id_fkey FOREIGN KEY (testresultat_id) REFERENCES testresultat(id) ON DELETE CASCADE;






ALTER TABLE ONLY testresultat
    ADD CONSTRAINT testresultat_testgrunnlag_id_fkey FOREIGN KEY (testgrunnlag_id) REFERENCES testgrunnlag(id) ON DELETE CASCADE;






ALTER TABLE ONLY utval_loeysing
    ADD CONSTRAINT utval_loeysing_utval_id_fkey FOREIGN KEY (utval_id) REFERENCES utval(id) ON DELETE CASCADE;






