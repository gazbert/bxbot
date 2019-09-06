/*
 * BX-bot Users and Roles for loading into the H2 in-memory database.
 *
 * You MUST change the bcrypt passwords for your bot!
 *
 * See:
 * https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html
 *
 * An example of using BCryptPasswordEncoder can be found here:
 * https://www.mkyong.com/spring-security/spring-security-password-hashing-example/
 *
 * DO NOT use online bcrypt password generators unless you want to risk having your passwords
 * harvested!
 */
insert into USER (ID, USERNAME, PASSWORD, FIRSTNAME, LASTNAME, EMAIL, ENABLED, LASTPASSWORDRESETDATE)
values (1, 'admin', '$2a$08$lDnHPz7eUkSi6ao14Twuau08mzhWrL4kyZGGU5xfiGALO/Vxd5DOi', 'admin', 'admin', 'admin@bxbot.com', 1, PARSEDATETIME('01-09-2019', 'dd-MM-yyyy'));

insert into USER (ID, USERNAME, PASSWORD, FIRSTNAME, LASTNAME, EMAIL, ENABLED, LASTPASSWORDRESETDATE)
values (2, 'user', '$2a$06$Ut3LoKEuuhVBJObJA.nw.OwD8CcacRachaFIUU0TcI3vghz4MZS9K', 'user', 'user', 'user@bxbot.com', 1, PARSEDATETIME('01-09-2019','dd-MM-yyyy'));

insert into USER (ID, USERNAME, PASSWORD, FIRSTNAME, LASTNAME, EMAIL, ENABLED, LASTPASSWORDRESETDATE)
values (3, 'blocked-user', '$2a$08$UkVvwpULis18S19S5pZFn.YHPZt3oaqHZnDwqbCW9pft6uFtkXKDC', 'blocked-user', 'blocked-user', 'blocked-user@bxbot.com', 0, PARSEDATETIME('01-09-2019','dd-MM-yyyy'));

insert into ROLE (ID, NAME) values (1, 'ROLE_USER');
insert into ROLE (ID, NAME) values (2, 'ROLE_ADMIN');

insert into USER_ROLE (USER_ID, ROLE_ID) values (1, 1);
insert into USER_ROLE (USER_ID, ROLE_ID) values (1, 2);
insert into USER_ROLE (USER_ID, ROLE_ID) values (2, 1);
insert into USER_ROLE (USER_ID, ROLE_ID) values (3, 1);