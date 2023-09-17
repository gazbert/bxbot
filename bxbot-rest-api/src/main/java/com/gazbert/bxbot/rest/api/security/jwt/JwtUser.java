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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gazbert.bxbot.rest.api.security.model.Role;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Encapsulates the User details stored in the JWT.
 *
 * @author gazbert
 */
public class JwtUser implements UserDetails {

  @Serial private static final long serialVersionUID = -7857515944595149222L;

  /** User id. */
  private final Long id;

  /** Username. */
  private final String username;

  /** Firstname. */
  private final String firstname;

  /** Lastname. */
  private final String lastname;

  /** Password. */
  private final String password;

  /** Email. */
  private final String email;

  /** Granted authorities. */
  private final Collection<? extends GrantedAuthority> authorities;

  /** Is enabled. */
  private final boolean enabled;

  /** Last password reset date. */
  private final long lastPasswordResetDate;

  /** The user's roles. */
  private final List<String> roles;

  /**
   * Creates a JWT User.
   *
   * @param id the user's id.
   * @param username the user's name.
   * @param firstname the user's first name.
   * @param lastname the user's last name.
   * @param password the use's password.
   * @param email the user's email.
   * @param enabled is the user enabled or disabled?
   * @param lastPasswordResetDate the date the user's password was reset.
   * @param authorities the user's authorities.
   * @param roles the user's roles.
   */
  public JwtUser(
      Long id,
      String username,
      String firstname,
      String lastname,
      String password,
      String email,
      boolean enabled,
      long lastPasswordResetDate,
      Collection<? extends GrantedAuthority> authorities,
      List<Role> roles) {

    this.id = id;
    this.username = username;
    this.firstname = firstname;
    this.lastname = lastname;
    this.password = password;
    this.email = email;
    this.enabled = enabled;
    this.lastPasswordResetDate = lastPasswordResetDate;
    this.authorities = authorities;
    this.roles = new ArrayList<>();
    for (final Role role : roles) {
      this.roles.add(role.getName().name());
    }
  }

  /**
   * Returns the id.
   *
   * @return the id.
   */
  @JsonIgnore
  public Long getId() {
    return id;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @JsonIgnore
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @JsonIgnore
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @JsonIgnore
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * Returns the firstname.
   *
   * @return the firstname.
   */
  public String getFirstname() {
    return firstname;
  }

  /**
   * Returns the lastname.
   *
   * @return the lastname.
   */
  public String getLastname() {
    return lastname;
  }

  /**
   * Returns the email.
   *
   * @return the email.
   */
  public String getEmail() {
    return email;
  }

  @JsonIgnore
  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Returns the last password reset date.
   *
   * @return the last password reset date.
   */
  public long getLastPasswordResetDate() {
    return lastPasswordResetDate;
  }

  /**
   * Returns the roles.
   *
   * @return the roles.
   */
  public List<String> getRoles() {
    return roles;
  }
}
