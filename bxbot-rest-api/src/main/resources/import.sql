-- BX-bot Users and Roles for loading into the H2 in-memory database.
-- BX-bot 用户和角色，用于加载到 H2 内存数据库中。
--
-- You MUST change the bcrypt passwords for your bot!
-- 您必须更改您的机器人的 bcrypt 密码！
--
-- See:
-- 看:
-- https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html
--
-- An example of using BCryptPasswordEncoder can be found here:
-- 可以在此处找到使用 BCryptPasswordEncoder 的示例：
-- https://www.mkyong.com/spring-security/spring-security-password-hashing-example/
--
-- DO NOT use online bcrypt password generators unless you want to risk having your passwords harvested!
-- 不要使用在线 bcrypt 密码生成器，除非您想冒险收集密码！
--
INSERT INTO BXBOT_USER (ID, USERNAME, PASSWORD, FIRSTNAME, LASTNAME, EMAIL, ENABLED, LASTPASSWORDRESETDATE) VALUES (1, 'admin', '$2a$08$lDnHPz7eUkSi6ao14Twuau08mzhWrL4kyZGGU5xfiGALO/Vxd5DOi', 'admin', 'admin', 'admin@admin.com', 1, PARSEDATETIME('01-01-2016', 'dd-MM-yyyy'));
INSERT INTO BXBOT_USER (ID, USERNAME, PASSWORD, FIRSTNAME, LASTNAME, EMAIL, ENABLED, LASTPASSWORDRESETDATE) VALUES (2, 'user', '$2a$06$Ut3LoKEuuhVBJObJA.nw.OwD8CcacRachaFIUU0TcI3vghz4MZS9K', 'user', 'user', 'enabled@user.com', 1, PARSEDATETIME('01-01-2016','dd-MM-yyyy'));

INSERT INTO ROLE (ID, NAME) VALUES (1, 'ROLE_USER');
INSERT INTO ROLE (ID, NAME) VALUES (2, 'ROLE_ADMIN');

INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (1, 1);
INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (1, 2);
INSERT INTO USER_ROLE (USER_ID, ROLE_ID) VALUES (2, 1);
