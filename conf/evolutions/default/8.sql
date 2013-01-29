# --- Add userInfo to grade session

# --- !Ups

alter table grade_session add column token varchar(255) not null;

# --- !Downs

alter table grade_session drop column token;