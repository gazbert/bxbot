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

package com.gazbert.bxbot.rest.api.security.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import org.springframework.util.Assert;

/**
 * Represents a BX-bot User.
 *
 * @author gazbert
 */
@Entity
@Table(name = "BXBOT_USER")
public class User {

  @Id
  @Column(name = "ID")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
  @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
  private Long id;

  @Column(name = "USERNAME", length = 50, unique = true)
  @NotNull
  @Size(min = 5, max = 50)
  private String username;

  @Column(name = "PASSWORD", length = 100)
  @NotNull
  @Size(min = 5, max = 100)
  private String password;

  @Column(name = "FIRSTNAME", length = 50)
  @NotNull
  @Size(min = 5, max = 50)
  private String firstname;

  @Column(name = "LASTNAME", length = 50)
  @NotNull
  @Size(min = 5, max = 50)
  private String lastname;

  @Column(name = "EMAIL", length = 100)
  @NotNull
  @Size(min = 5, max = 100)
  private String email;

  @Column(name = "ENABLED")
  @NotNull
  private Boolean enabled;

  @Column(name = "LASTPASSWORDRESETDATE")
  @Temporal(TemporalType.TIMESTAMP)
  @NotNull
  private Date lastPasswordResetDate;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "USER_ROLE",
      joinColumns = {@JoinColumn(name = "USER_ID", referencedColumnName = "ID")},
      inverseJoinColumns = {@JoinColumn(name = "ROLE_ID", referencedColumnName = "ID")})
  private List<Role> roles;

  /** Creates the User. */
  public User() {
    // No extra init needed.
  }

  /**
   * Returns the id.
   *
   * @return the id.
   */
  public Long getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the id.
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the username.
   *
   * @return the username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username.
   *
   * @param username the username.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Returns the password.
   *
   * @return the password.
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password.
   *
   * @param password the password.
   */
  public void setPassword(String password) {
    this.password = password;
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
   * Sets the firstname.
   *
   * @param firstname the firstname.
   */
  public void setFirstname(String firstname) {
    this.firstname = firstname;
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
   * Sets the lastname.
   *
   * @param lastname the lastname.
   */
  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  /**
   * Returns the email.
   *
   * @return the email.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the email.
   *
   * @param email the email.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Returns if user is enabled.
   *
   * @return user enabled?
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets if user is enabled.
   *
   * @param enabled user enabled?
   */
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns their roles.
   *
   * @return their roles.
   */
  public List<Role> getRoles() {
    return roles;
  }

  /**
   * Sets their roles.
   *
   * @param roles their roles.
   */
  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  /**
   * Returns the last password reset date.
   *
   * @return last password reset date.
   */
  public Date getLastPasswordResetDate() {
    if (lastPasswordResetDate != null) {
      return new Date(lastPasswordResetDate.getTime());
    } else {
      return null;
    }
  }

  /**
   * Sets last password reset date.
   *
   * @param lastPasswordResetDate last password reset date.
   */
  public void setLastPasswordResetDate(Date lastPasswordResetDate) {
    Assert.notNull(lastPasswordResetDate, "lastPasswordResetDate cannot be null!");
    this.lastPasswordResetDate = new Date(lastPasswordResetDate.getTime());
  }
}
