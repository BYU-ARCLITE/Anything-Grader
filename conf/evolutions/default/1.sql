# --- First database schema

# --- !Ups

create table auth_scheme (
  id             bigint not null auto_increment,
  publicKey      varchar(255) not null,
  privateKey     varchar(255) not null,
  authType       varchar(255) not null,
  primary key(id)
);


# --- !Downs

drop table if exists auth_scheme;