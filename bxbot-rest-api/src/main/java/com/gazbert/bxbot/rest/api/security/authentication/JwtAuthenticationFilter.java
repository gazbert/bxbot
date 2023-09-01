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

import com.gazbert.bxbot.rest.api.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The JWT Authentication Filter extracts the JWT from the Authorization header and validates it.
 *
 * <p>If no JWT is present, the next filter in the Spring Security filter chain is invoked.
 *
 * <p>The filter is invoked once for every request to validate the JWT - we don't use sessions.
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by <a
 * href="https://github.com/szerhusenBC/jwt-spring-security-demo">Stephan Zerhusen</a>.
 *
 * @author gazbert
 */
@Log4j2
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

  private JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException {

    try {
      // Extract token after Bearer prefix if present
      String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
      if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
        authorizationHeader = authorizationHeader.substring(BEARER_PREFIX_LENGTH);
      }

      // Might be null if client does not have a token yet.
      if (authorizationHeader != null) {
        final Claims claims = jwtUtils.validateTokenAndGetClaims(authorizationHeader);
        log.info("JWT is valid");
        final String username = jwtUtils.getUsernameFromTokenClaims(claims);
        log.info("Username in JWT: " + username);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
          // First time in - store user details in Spring's Security context
          final UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  username, null, jwtUtils.getRolesFromTokenClaims(claims));

          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);

          log.info("Authenticated User: " + username + " has been set in Spring SecurityContext.");
        }
      }

      chain.doFilter(request, response);

    } catch (Exception e) {
      log.error("JWT Authentication failure! Details: " + e.getMessage(), e);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
  }

  /**
   * Sets the JWT utils.
   *
   * @param jwtUtils the JWT utils.
   */
  @Autowired
  public void setJwtUtils(JwtUtils jwtUtils) {
    this.jwtUtils = jwtUtils;
  }
}
