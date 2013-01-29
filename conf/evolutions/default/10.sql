# --- Add content type

# --- !Ups

alter table hook add column contentType varchar(255) not null;

# --- !Downs

alter table hook drop column contentType;