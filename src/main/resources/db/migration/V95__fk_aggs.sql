ALTER TABLE aggregering_testregel
ADD CONSTRAINT aggregering_testregel_testgrunnlag_fk(testgrunnlag_id)
REFERENCES Testgrunnlag(id)
ON DELETE CASCADE;

ALTER TABLE aggregering_side
ADD CONSTRAINT aggregering_side_testgrunnlag_fk(testgrunnlag_id)
REFERENCES Testgrunnlag(id)
ON DELETE CASCADE;

ALTER TABLE aggregering_suksesskriterium
ADD CONSTRAINT aggregering_suksesskriterium_testgrunnlag_fk(testgrunnlag_id)
REFERENCES Testgrunnlag(id)
ON DELETE CASCADE;