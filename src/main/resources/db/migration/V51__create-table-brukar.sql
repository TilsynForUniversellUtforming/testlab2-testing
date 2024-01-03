create table brukar
(
    id         serial primary key,
    brukarnamn text unique not null,
    namn       text        not null
)