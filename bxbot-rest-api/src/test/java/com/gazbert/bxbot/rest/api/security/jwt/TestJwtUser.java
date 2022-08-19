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

package com.gazbert.bxbot.rest.api.security.jwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.rest.api.security.model.Role;
import com.gazbert.bxbot.rest.api.security.model.RoleName;
import com.gazbert.bxbot.rest.api.security.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Tests JWT User is created and behaves as expected.
 * 测试 JWT 用户已创建并按预期运行。
 *
 * @author gazbert
 */
class TestJwtUser {

  private static final Long ADMIN_ROLE_ID = Long.valueOf("213443242342");
  private static final Long USER_ROLE_ID = Long.valueOf("21344565442342");

  private static final Long USER_ID = Long.valueOf("2323267789789");
  private static final String USERNAME = "hansolo";
  private static final String PASSWORD = "password";
  private static final String FIRSTNAME = "Han";
  private static final String LASTNAME = "Solo";
  private static final String EMAIL = "han@falcon";
  private static final boolean USER_ENABLED = true;
  private static final Date LAST_PASSWORD_RESET_DATE = new Date();

  @Test
  void testInitialisationWorksAsExpected() {
    final List<Role> roles = createRoles(createUser());

    final JwtUser jwtUser =
        new JwtUser(
            USER_ID,
            USERNAME,
            FIRSTNAME,
            LASTNAME,
            PASSWORD,
            EMAIL,
            USER_ENABLED,
            LAST_PASSWORD_RESET_DATE.getTime(),
            createAuthorities(),
            roles);

    assertEquals(USER_ID, jwtUser.getId());
    assertEquals(USERNAME, jwtUser.getUsername());
    assertEquals(PASSWORD, jwtUser.getPassword());
    assertEquals(FIRSTNAME, jwtUser.getFirstname());
    assertEquals(LASTNAME, jwtUser.getLastname());
    assertEquals(EMAIL, jwtUser.getEmail());
    assertEquals(USER_ENABLED, jwtUser.isEnabled());
    assertEquals(LAST_PASSWORD_RESET_DATE.getTime(), jwtUser.getLastPasswordResetDate());

    assertTrue(jwtUser.getRoles().contains(RoleName.ROLE_ADMIN.name()));
    assertTrue(jwtUser.getRoles().contains(RoleName.ROLE_USER.name()));

    assertTrue(
        jwtUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.name())));
    assertTrue(
        jwtUser.getAuthorities().contains(new SimpleGrantedAuthority(RoleName.ROLE_USER.name())));

    assertTrue(jwtUser.isCredentialsNonExpired());
    assertTrue(jwtUser.isAccountNonLocked());
    assertTrue(jwtUser.isAccountNonExpired());
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

  private User createUser() {
    final User user = new User();
    user.setId(USER_ID);
    user.setUsername(USERNAME);
    user.setPassword(PASSWORD);
    user.setFirstname(FIRSTNAME);
    user.setLastname(LASTNAME);
    user.setEmail(EMAIL);
    user.setEnabled(USER_ENABLED);
    user.setLastPasswordResetDate(LAST_PASSWORD_RESET_DATE);

    final List<Role> roles = createRoles(user);
    user.setRoles(roles);
    return user;
  }

  private List<GrantedAuthority> createAuthorities() {
    final SimpleGrantedAuthority admin = new SimpleGrantedAuthority(RoleName.ROLE_ADMIN.name());
    final SimpleGrantedAuthority user = new SimpleGrantedAuthority(RoleName.ROLE_USER.name());
    final List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(admin);
    authorities.add(user);
    return authorities;
  }
}
