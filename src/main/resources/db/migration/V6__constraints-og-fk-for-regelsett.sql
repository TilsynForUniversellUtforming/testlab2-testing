alter table testregel add status varchar not null default 'Publisert';

alter table RegelsettTestregel drop constraint regelsetttestregel_idregelsett_fkey,
    add constraint regelsetttestregel_idregelsett_fkey
    foreign key (idtestregel)
    references testregel(id)
    on delete cascade;

alter table RegelsettTestregel drop constraint regelsetttestregel_idtestregel_fkey,
    add constraint regelsetttestregel_idtestregel_fkey
    foreign key (idregelsett)
    references regelsett(id)
    on delete cascade;

