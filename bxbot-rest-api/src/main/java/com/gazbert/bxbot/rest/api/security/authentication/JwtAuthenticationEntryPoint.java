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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * JWT 'login' entry point - Spring does not supply one for JWT Authentication.
 *
 * <p>We just send a 401 response, to which the client should call the /auth endpoint to fetch a JWT
 * for use in all subsequent requests.
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by
 * <a href="https://github.com/szerhusenBC/jwt-spring-security-demo">Stephan Zerhusen</a>.
 *
 * @author gazbert
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  /** Creates the JWT Authentication Entry Point. */
  public JwtAuthenticationEntryPoint() {
    // No extra init needed.
  }

  /**
   * This is invoked when a user tries to access a secured REST resource without supplying any
   * credentials in the HTTP Authorization header.
   *
   * <p>We just send a 401 Unauthorized response because there is no 'login page' to redirect to.
   * The client should then post username/password to the /auth endpoint to obtain a JWT.
   *
   * @param request the incoming request.
   * @param response the outbound response.
   * @param authException the exception that got us here.
   */
  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
  }
}
