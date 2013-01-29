# --- Add grade session table

# --- !Ups

create table grade_session (
  id                    bigint not null auto_increment,
  problemSet            bigint not null,
  responseData          longtext not null,
  primary key(id)
);

# --- !Downs

drop table if exists grade_session;