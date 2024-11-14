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

package com.gazbert.bxbot.rest.api.security.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Encapsulates a JWT Authentication Request containing username/password sent from the client.
 *
 * <p>When a client first connects, it will need to pass credentials in order to be authenticated
 * and issued a JWT for use in subsequent requests.
 *
 * @author gazbert
 */
@Setter
@Getter
public class JwtAuthenticationRequest {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The username.")
  @NotNull(message = "Username cannot be null")
  private String username;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The password.")
  @NotNull(message = "Password cannot be null")
  private String password;

  /** Creates the JwtAuthenticationRequest. Required for Jackson. */
  public JwtAuthenticationRequest() {
    username = "";
    password = "";
  }

  /**
   * Creates the JwtAuthenticationRequest.
   *
   * @param username the username.
   * @param password the password.
   */
  public JwtAuthenticationRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }
}
