ALTER TABLE testgrunnlag
ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE maalingv1
ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();