-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlDialectInspectionForFile

-------------------------------------------------------------------------------
-- TODO Work in progress... not safe for production!
-------------------------------------------------------------------------------
insert into user(id, name, login_id, password) values (1,'BX-bot UI','bxbot-ui','bxbot-ui');
insert into user(id, name, login_id, password) values (2,'Another User','anotheruser','password');

insert into role(id, name) values (1,'ROLE_USER');
insert into role(id, name) values (2,'ROLE_ADMIN');

insert into user_role(user_id, role_id) values (1,1);
insert into user_role(user_id, role_id) values (1,2);
insert into user_role(user_id, role_id) values (2,1);
