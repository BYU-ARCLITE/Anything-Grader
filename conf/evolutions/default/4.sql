# --- Add response order column to problem

# --- !Ups

alter table problem add column responseOrderModifier boolean not null;

# --- !Downs

alter table problem drop column responseOrderModifier;