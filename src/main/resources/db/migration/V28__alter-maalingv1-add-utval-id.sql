alter table maalingv1
    add column utval_id int default null references utval (id)