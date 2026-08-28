ALTER TABLE aggregering_testregel ADD COLUMN testrun_uuid UUID;

ALTER TABLE aggregering_suksesskriterium ADD COLUMN testrun_uuid UUID;

ALTER TABLE aggregering_side ADD COLUMN testrun_uuid UUID;

UPDATE aggregering_testregel
SET testrun_uuid = (SELECT DISTINCT uuid
FROM maalingv1
WHERE maalingv1.id = aggregering_testregel.maaling_id)
WHERE testrun_uuid IS NULL;

UPDATE aggregering_suksesskriterium
SET testrun_uuid = (SELECT DISTINCT uuid
FROM maalingv1
WHERE maalingv1.id  = aggregering_suksesskriterium.maaling_id)
WHERE testrun_uuid IS NULL;

UPDATE aggregering_side
SET testrun_uuid = (SELECT DISTINCT uuid
FROM maalingv1
WHERE maalingv1.id  = aggregering_side.maaling_id)
WHERE testrun_uuid IS NULL;

UPDATE aggregering_testregel
SET testrun_uuid = (SELECT DISTINCT uuid
FROM testgrunnlag
WHERE testgrunnlag.id = aggregering_testregel.testgrunnlag_id)
WHERE testrun_uuid IS NULL;

UPDATE aggregering_suksesskriterium
SET testrun_uuid = (SELECT DISTINCT uuid
FROM testgrunnlag
WHERE testgrunnlag.id = aggregering_suksesskriterium.testgrunnlag_id)
WHERE testrun_uuid IS NULL;

UPDATE aggregering_side
SET testrun_uuid = (SELECT DISTINCT uuid
FROM testgrunnlag
WHERE testgrunnlag.id = aggregering_side.testgrunnlag_id)
WHERE testrun_uuid IS NULL;