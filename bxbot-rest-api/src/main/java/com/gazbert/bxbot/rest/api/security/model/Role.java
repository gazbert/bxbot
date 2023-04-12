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

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Represents a Role for a BX-bot User.
 *
 * @author gazbert
 */
@Entity
@Table(name = "ROLE")
public class Role {

  @Id
  @Column(name = "ID")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq")
  @SequenceGenerator(name = "role_seq", sequenceName = "role_seq", allocationSize = 1)
  private Long id;

  @Column(name = "NAME", length = 50)
  @NotNull
  @Enumerated(EnumType.STRING)
  private RoleName name;

  @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
  private List<User> users;

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
   * Returns the name.
   *
   * @return the name.
   */
  public RoleName getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the name.
   */
  public void setName(RoleName name) {
    this.name = name;
  }

  /**
   * Returns the users.
   *
   * @return the users.
   */
  public List<User> getUsers() {
    return users;
  }

  /**
   * Sets the users.
   *
   * @param users the users.
   */
  public void setUsers(List<User> users) {
    this.users = users;
  }
}
