ALTER TABLE kontroll
    ADD COLUMN utval_id INTEGER REFERENCES utval (id);