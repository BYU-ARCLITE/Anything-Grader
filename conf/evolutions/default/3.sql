# --- Hook, ProblemSet, and User tables

# --- !Ups

create table user (
  id                    bigint not null auto_increment,
  username              varchar(255) not null,
  password              varchar(255) not null,
  problemSets           longtext not null,
  primary key(id)
);

create table problem_set(
  id                    bigint not null auto_increment,
  name                  varchar(255) not null,
  problems              longtext not null,
  hooks                 longtext not null,
  primary key(id)
);

create table hook(
  id                    bigint not null auto_increment,
  uri                   varchar(512) not null,
  method                varchar(8) not null,
  authScheme            bigint not null,
  scaled                boolean not null,
  additionalData        longtext not null,
  primary key(id)
);

# --- !Downs

drop table if exists user;
drop table if exists problem_set;
drop table if exists hook;