/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.rest.api.security.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests Role model behaves as expected.
 * 测试角色模型的行为符合预期。
 *
 * @author gazbert
 */
class TestRole {

  private static final Long USER_ROLE_ID = Long.valueOf("213443242342");
  private static final Long ADMIN_ROLE_ID = Long.valueOf("55687686786");

  private static final Long USER_1_ID = Long.valueOf("2323267789789");
  private static final String USER_1_USERNAME = "hansolo";
  private static final String USER_1_PASSWORD = "password";
  private static final String USER_1_FIRSTNAME = "Han";
  private static final String USER_1_LASTNAME = "Solo";
  private static final String USER_1_EMAIL = "han@falcon";
  private static final boolean USER_1_ENABLED = true;
  private static final Date USER_1_LAST_PASSWORD_RESET_DATE = new Date();

  private static final Long USER_2_ID = Long.valueOf("563454654");
  private static final String USER_2_USERNAME = "chewie";
  private static final String USER_2_PASSWORD = "password";
  private static final String USER_2_FIRSTNAME = "Chew";
  private static final String USER_2_LASTNAME = "Bacca";
  private static final String USER_2_EMAIL = "chewie@falcon";
  private static final boolean USER_2_ENABLED = true;
  private static final Date USER_2_LAST_PASSWORD_RESET_DATE = new Date();

  private User user1;
  private User user2;

  @Test
  void testInitialisationWorksAsExpected() {
    final Role role = new Role();
    assertNull(role.getId());
    assertNull(role.getName());
    assertNull(role.getUsers());
  }

  @Test
  void testSettersWorkAsExpected() {
    final Role role = new Role();

    role.setId(ADMIN_ROLE_ID);
    assertEquals(ADMIN_ROLE_ID, role.getId());

    role.setName(RoleName.ROLE_ADMIN);
    assertEquals(RoleName.ROLE_ADMIN, role.getName());

    final List<User> users = createUsers();
    role.setUsers(users);
    assertEquals(users, role.getUsers());
  }

  // ------------------------------------------------------------------------
  // Private utils
  // ------------------------------------------------------------------------

  private List<User> createUsers() {
    final List<User> users = new ArrayList<>();

    user1 = new User();
    user1.setId(USER_1_ID);
    user1.setUsername(USER_1_USERNAME);
    user1.setPassword(USER_1_PASSWORD);
    user1.setFirstname(USER_1_FIRSTNAME);
    user1.setLastname(USER_1_LASTNAME);
    user1.setEmail(USER_1_EMAIL);
    user1.setEnabled(USER_1_ENABLED);
    user1.setLastPasswordResetDate(USER_1_LAST_PASSWORD_RESET_DATE);
    user1.setRoles(createUser1Roles());

    user2 = new User();
    user2.setId(USER_2_ID);
    user2.setUsername(USER_2_USERNAME);
    user2.setPassword(USER_2_PASSWORD);
    user2.setFirstname(USER_2_FIRSTNAME);
    user2.setLastname(USER_2_LASTNAME);
    user2.setEmail(USER_2_EMAIL);
    user2.setEnabled(USER_2_ENABLED);
    user2.setLastPasswordResetDate(USER_2_LAST_PASSWORD_RESET_DATE);
    user2.setRoles(createUser2Roles());

    users.add(user1);
    users.add(user2);
    return users;
  }

  private List<Role> createUser1Roles() {
    final List<User> users = Collections.singletonList(user1);
    final List<Role> roles = new ArrayList<>();

    final Role role1 = new Role();
    role1.setId(ADMIN_ROLE_ID);
    role1.setName(RoleName.ROLE_ADMIN);
    role1.setUsers(users);

    final Role role2 = new Role();
    role2.setId(USER_ROLE_ID);
    role2.setName(RoleName.ROLE_USER);
    role2.setUsers(users);

    roles.add(role1);
    roles.add(role2);
    return roles;
  }

  private List<Role> createUser2Roles() {
    final List<User> users = Collections.singletonList(user2);
    final List<Role> roles = new ArrayList<>();

    final Role role1 = new Role();
    role1.setId(USER_ROLE_ID);
    role1.setName(RoleName.ROLE_USER);
    role1.setUsers(users);

    roles.add(role1);
    return roles;
  }
}
