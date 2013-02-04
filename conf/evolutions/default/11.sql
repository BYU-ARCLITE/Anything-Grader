# --- Add content type

# --- !Ups

alter table problem add column multipleGradeModifier boolean not null default false;

# --- !Downs

alter table problem drop column multipleGradeModifier;