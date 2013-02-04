# --- Add content type

# --- !Ups

alter table `user` add column floatingProblems longtext not null;

# --- !Downs

alter table `problem` drop column floatingProblems;