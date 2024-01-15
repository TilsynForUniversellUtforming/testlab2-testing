alter table sak
    add column ansvarleg int references brukar (id);