-- BX-bot Users and Roles for loading into the H2 in-memory database.
--
-- You MUST change the bcrypt passwords for your bot!
--
-- See:
-- https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html
--
-- An example of using BCryptPasswordEncoder can be found here:
-- https://www.mkyong.com/spring-security/spring-security-password-hashing-example/
--
-- DO NOT use online bcrypt password generators unless you want to risk having your passwords harvested!
--
INSERT INTO USER (ID, USERNAME, PASSWORD, FIRSTNAME, LASTNAME, EMAIL, ENABLED, LASTPASSWORDRESETDATE) VALUES (1, 'admin', '$2a$08$lDnHPz7eUkSi6ao14Twuau08mzhWrL4kyZGGU5xfiGALO/Vxd5DOi', 'admin', 'admin', 'admin@admin.com', 1, PARSEDATETIME('01-01-2016', 'dd-MM-yyyy'));
INSERT INTO USER (ID, USERNAME, PASSWORD, FIRSTNAME, LASTNAME, EMAIL, ENABLED, LASTPASSWORDRESETDATE) VALUES (2, 'user', '$2a$06$Ut3LoKEuuhVBJObJA.nw.OwD8CcacRachaFIUU0TcI3vghz4MZS9K', 'user', 'user', 'enabled@user.com', 1, PARSEDATETIME('01-01-2016','dd-MM-yyyy'));

INSERT INTO ROLE (ID, NAME) VALUES (1, 'ROLE_USER');
INSERT INTO ROLE (ID, NAME) VALUES (2, 'ROLE_ADMIN');

INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (1, 1);
INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (1, 2);
INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (2, 1);
