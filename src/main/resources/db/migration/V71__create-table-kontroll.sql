create table kontroll(
    id serial primary key,
    tittel text not null,
    saksbehandler text not null,
    sakstype text not null,
    arkivreferanse text not null
)