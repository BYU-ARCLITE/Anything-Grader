# --- Add grade session table

# --- !Ups

create table response_data (
  id        bigint not null auto_increment,
  problem   bigint not null,
  data      longtext not null,
  grade     double not null,
  primary key(id)
);

# --- !Downs

drop table if exists response_data;