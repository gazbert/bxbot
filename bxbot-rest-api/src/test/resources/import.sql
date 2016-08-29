-- noinspection SqlNoDataSourceInspectionForFile
-- noinspection SqlDialectInspectionForFile

-------------------------------------------------------------------------------
-- Test users for testing REST API with OAuth2 enabled
-------------------------------------------------------------------------------
insert into user(id, name, login_id, password) values (1,'Test User 1','user1','user1-password');
insert into user(id, name, login_id, password) values (2,'Test User 2','user2','user2-password');

insert into role(id, name) values (1,'ROLE_USER');
insert into role(id, name) values (2,'ROLE_ADMIN');

insert into user_role(user_id, role_id) values (1,1);
insert into user_role(user_id, role_id) values (1,2);
insert into user_role(user_id, role_id) values (2,1);
