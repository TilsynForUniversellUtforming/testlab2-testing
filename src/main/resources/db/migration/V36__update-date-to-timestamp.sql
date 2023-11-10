alter table maalingv1
    alter column dato_start
        type timestamp with time zone
        using dato_start::timestamp with time zone at time zone 'Europe/Oslo';