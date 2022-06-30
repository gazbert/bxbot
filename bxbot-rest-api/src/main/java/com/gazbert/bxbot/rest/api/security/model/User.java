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

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.util.Assert;

/**
 * Represents a BX-bot User.
 *
 * 代表 BX-bot 用户。 * @author gazbert
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

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  /**
   * Returns the last password reset date.  返回上次密码重置日期。
   *
   * @return last password reset date. @return 上次密码重置日期。
   */
  public Date getLastPasswordResetDate() {
    if (lastPasswordResetDate != null) {
      return new Date(lastPasswordResetDate.getTime());
    } else {
      return null;
    }
  }

  public void setLastPasswordResetDate(Date lastPasswordResetDate) {
    Assert.notNull(lastPasswordResetDate, "lastPasswordResetDate cannot be null! lastPasswordResetDate 不能为空！");
    this.lastPasswordResetDate = new Date(lastPasswordResetDate.getTime());
  }
}
