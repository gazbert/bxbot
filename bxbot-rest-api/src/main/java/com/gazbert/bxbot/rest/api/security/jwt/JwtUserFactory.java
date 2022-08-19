/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Stephan Zerhusen
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

import com.gazbert.bxbot.rest.api.security.model.Role;
import com.gazbert.bxbot.rest.api.security.model.User;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Creates a JWT User that has been authenticated successfully.
 * 创建一个已成功认证的 JWT 用户。
 *
 * @author gazbert
 */
public final class JwtUserFactory {

  private JwtUserFactory() {
  }

  /**
   * Creates a JWT User.
   * 创建一个 JWT 用户。
   *
   * @param user the user details from the database.
   *             数据库中的用户详细信息。
   * @return a JWT User.  智威汤逊用户。
   */
  public static JwtUser create(User user) {
    return new JwtUser(
        user.getId(),
        user.getUsername(),
        user.getFirstname(),
        user.getLastname(),
        user.getPassword(),
        user.getEmail(),
        user.getEnabled(),
        user.getLastPasswordResetDate().getTime(),
        mapUserRolesToGrantedAuthorities(user.getRoles()),
        user.getRoles());
  }

  private static List<GrantedAuthority> mapUserRolesToGrantedAuthorities(List<Role> roles) {
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
        .collect(Collectors.toList());
  }
}
