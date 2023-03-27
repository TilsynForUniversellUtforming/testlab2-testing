alter table maalingv1
    add column max_links_per_page int not null default 100;

alter table maalingv1
    add column num_links_to_select int not null default 30;
