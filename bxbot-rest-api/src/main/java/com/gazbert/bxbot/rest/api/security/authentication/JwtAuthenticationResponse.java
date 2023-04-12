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
import javax.validation.constraints.NotNull;

/**
 * Encapsulates a JWT Authentication response. It wraps the JWT (Base64 encoded String).
 *
 * @author gazbert
 */
public class JwtAuthenticationResponse {

  @Schema(required = true, description = "The JWT.")
  @NotNull(message = "Token cannot be null")
  private String token;

  /** Creates the JwtAuthenticationResponse. Required for Jackson. */
  public JwtAuthenticationResponse() {
    token = "";
  }

  /**
   * Creates the JwtAuthenticationResponse.
   *
   * @param token the JWT.
   */
  public JwtAuthenticationResponse(String token) {
    this.token = token;
  }

  /**
   * Returns the JWT.
   *
   * @return the JWT.
   */
  public String getToken() {
    return this.token;
  }

  /**
   * Sets the JWT.
   *
   * @param token the JWT.
   */
  public void setToken(String token) {
    this.token = token;
  }
}
