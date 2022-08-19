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
 * Tests User model behaves as expected.
 * * 测试用户模型的行为符合预期。
 *
 * @author gazbert
 */
class TestUser {

  private static final Long ADMIN_ROLE_ID = Long.valueOf("213443242342");
  private static final Long USER_ROLE_ID = Long.valueOf("21344565442342");

  private static final Long USER_1_ID = Long.valueOf("2323267789789");
  private static final String USER_1_USERNAME = "hansolo";
  private static final String USER_1_PASSWORD = "password";
  private static final String USER_1_FIRSTNAME = "Han";
  private static final String USER_1_LASTNAME = "Solo";
  private static final String USER_1_EMAIL = "han@falcon";
  private static final boolean USER_1_ENABLED = true;
  private static final Date USER_1_LAST_PASSWORD_RESET_DATE = new Date();

  @Test
  void testInitialisationWorksAsExpected() {
    final User user = new User();
    assertNull(user.getId());
    assertNull(user.getUsername());
    assertNull(user.getPassword());
    assertNull(user.getFirstname());
    assertNull(user.getLastname());
    assertNull(user.getEmail());
    assertNull(user.getEnabled());
    assertNull(user.getLastPasswordResetDate());
    assertNull(user.getRoles());
  }

  @Test
  void testSettersWorkAsExpected() {
    final User user = new User();

    user.setId(USER_1_ID);
    assertEquals(USER_1_ID, user.getId());

    user.setUsername(USER_1_USERNAME);
    assertEquals(USER_1_USERNAME, user.getUsername());

    user.setPassword(USER_1_PASSWORD);
    assertEquals(USER_1_PASSWORD, user.getPassword());

    user.setFirstname(USER_1_FIRSTNAME);
    assertEquals(USER_1_FIRSTNAME, user.getFirstname());

    user.setLastname(USER_1_LASTNAME);
    assertEquals(USER_1_LASTNAME, user.getLastname());

    user.setEmail(USER_1_EMAIL);
    assertEquals(USER_1_EMAIL, user.getEmail());

    user.setEnabled(USER_1_ENABLED);
    assertEquals(USER_1_ENABLED, user.getEnabled());

    user.setLastPasswordResetDate(USER_1_LAST_PASSWORD_RESET_DATE);
    assertEquals(USER_1_LAST_PASSWORD_RESET_DATE, user.getLastPasswordResetDate());

    final List<Role> roles = createRoles(user);
    user.setRoles(roles);
    assertEquals(roles, user.getRoles());
  }

  // ------------------------------------------------------------------------
  // Private utils
  // 私有工具
  // ------------------------------------------------------------------------

  private List<Role> createRoles(User user) {
    final List<User> users = Collections.singletonList(user);
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
}
