# --- Add started and finished columns

# --- !Ups

alter table grade_session add column started bigint not null, add column finished bigint not null;

# --- !Downs

alter table grade_session drop column started, drop column finished;