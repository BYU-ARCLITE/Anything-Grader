# --- Add content type

# --- !Ups

alter table problem add column name varchar(255) not null default "Unnamed";

# --- !Downs

alter table problem drop column name;