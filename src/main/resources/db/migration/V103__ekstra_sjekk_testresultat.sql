ALTER TABLE testresultat
ADD COLUMN ekstra_sjekk BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE testresultat
ADD COLUMN element_plassering TEXT;