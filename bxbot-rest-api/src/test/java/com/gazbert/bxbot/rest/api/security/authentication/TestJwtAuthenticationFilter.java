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

package com.gazbert.bxbot.rest.api.security.authentication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.rest.api.security.jwt.JwtUtils;
import io.jsonwebtoken.Claims;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests the JWT Authentication Filter behaves as expected.
 * * 测试 JWT 身份验证过滤器的行为是否符合预期。
 *
 * @author gazbert
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class TestJwtAuthenticationFilter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String USERNAME = "bobafett";

  @MockBean private HttpServletRequest request;
  @MockBean private HttpServletResponse response;
  @MockBean private FilterChain filterChain;
  @MockBean private JwtUtils jwtUtils;
  @MockBean private Claims claims;

  // Need these even though not used in the test directly because Spring loads them on startup...
  // 即使没有直接在测试中使用也需要这些，因为 Spring 在启动时加载它们...
  @MockBean private EmailAlerter emailAlerter;
  @MockBean private TradingEngine tradingEngine;
  @MockBean private RestartEndpoint restartEndpoint;
  @MockBean private LogFileWebEndpoint logFileWebEndpoint;
  @MockBean private AuthenticationManager authenticationManager;

  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setup() {
    jwtAuthenticationFilter = new JwtAuthenticationFilter();
    jwtAuthenticationFilter.setJwtUtils(jwtUtils);
  }

  @Test
  void whenFilterCalledWithValidTokenThenExpectSuccessfulAuthenticationAndCallNextFilter()
      throws Exception {

    // Need to reset this in case previous test sets it
    // 需要重置它以防之前的测试设置它
    SecurityContextHolder.getContext().setAuthentication(null);

    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("dummy-token");
    when(jwtUtils.validateTokenAndGetClaims((any()))).thenReturn(claims);
    when(jwtUtils.getUsernameFromTokenClaims((any()))).thenReturn(USERNAME);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(request, times(1)).getHeader(AUTHORIZATION_HEADER);
    verify(jwtUtils, times(1)).validateTokenAndGetClaims(any());
    verify(jwtUtils, times(1)).getUsernameFromTokenClaims(any());
    verify(jwtUtils, times(1)).getRolesFromTokenClaims(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void whenFilterCalledWithoutAuthorizationHeaderThenCallNextFilter() throws Exception {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(null);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(request, times(1)).getHeader(AUTHORIZATION_HEADER);
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void whenFilterCalledWithBearerTokenWithMissingUsernameThenCallNextFilter() throws Exception {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + "dummy-token");

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(request, times(1)).getHeader(AUTHORIZATION_HEADER);
    verify(jwtUtils, times(1)).getUsernameFromTokenClaims(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void whenFilterCalledWithTokenWithMissingUsernameThenCallNextFilter() throws Exception {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("dummy-token");
    when(jwtUtils.getUsernameFromTokenClaims((any()))).thenReturn(null);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(request, times(1)).getHeader(AUTHORIZATION_HEADER);
    verify(jwtUtils, times(1)).getUsernameFromTokenClaims(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void whenFilterCalledWithInvalidTokenThenCallNextFilter() throws Exception {
    when(request.getHeader(AUTHORIZATION_HEADER)).thenReturn("dummy-token");
    when(jwtUtils.getUsernameFromTokenClaims((any()))).thenReturn(USERNAME);
    when(jwtUtils.validateTokenAndGetClaims((any()))).thenReturn(claims);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(request, times(1)).getHeader(AUTHORIZATION_HEADER);
    verify(jwtUtils, times(1)).getUsernameFromTokenClaims(any());
    verify(jwtUtils, times(1)).validateTokenAndGetClaims(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }
}
