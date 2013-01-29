# --- Create the problem table

# --- !Ups

create table problem (
  id                    bigint not null auto_increment,
  answers               longtext not null,
  problemType           varchar(255) not null,
  acceptanceRate        double not null,
  caseModifier          boolean not null,
  punctuationModifier   boolean not null,
  wordOrderModifier     boolean not null,
  gradientGradeMethod   boolean not null,
  points                double not null,
  subtractiveModifier   boolean not null,
  primary key(id)
);


# --- !Downs

drop table if exists problem;