ALTER TABLE aggregering_testregel
DROP CONSTRAINT IF EXISTS aggregering_testregel_testregel_id_fkey;

ALTER TABLE maaling_testregel
DROP CONSTRAINT IF EXISTS maaling_testregel_testregel_id_fkey;

ALTER TABLE regelsett_testregel
DROP CONSTRAINT IF EXISTS  regelsetttestregel_idtestregel_fkey;

ALTER TABLE sak_testregel
DROP CONSTRAINT IF EXISTS sak_testregel_testregel_id_fkey;

ALTER TABLE testgrunnlag_testregel_kontroll
DROP CONSTRAINT IF EXISTS testgrunnlag_testregel_kontroll_testregel_id_fkey;

ALTER TABLE testresultat
DROP CONSTRAINT IF EXISTS testresultat_ik_testregel_id_fkey;

ALTER TABLE testresultat
DROP CONSTRAINT testresultat_sak_id_fkey;

ALTER TABLE testresultat
DROP COLUMN sak_id;

ALTER TABLE testgrunnlag
DROP COLUMN sak_id;

DROP TABLE testgrunnlag_testregel;

DROP TABLE sak_testregel;

DROP TABLE sak_loeysing_nettside;

DROP TABLE sak;