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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.gazbert.bxbot.core.engine.TradingEngine;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.rest.api.security.jwt.JwtUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.logging.LogFileWebEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Tests the JWT Authentication Entry Point behaves as expected.
 *
 * @author gazbert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestJwtAuthenticationEntryPoint {

  @MockBean private HttpServletRequest request;
  @MockBean private HttpServletResponse response;
  @MockBean private AuthenticationException authException;

  // Need these even though not used in the test directly because Spring loads them on startup...
  @MockBean private EmailAlerter emailAlerter;
  @MockBean private TradingEngine tradingEngine;
  @MockBean private RestartEndpoint restartEndpoint;
  @MockBean private LogFileWebEndpoint logFileWebEndpoint;
  @MockBean private JwtUtils jwtUtils;
  @MockBean private AuthenticationManager authenticationManager;

  @Test
  public void whenCommenceCalledThenExpectUnauthorizedResponse() throws Exception {
    final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint =
        new JwtAuthenticationEntryPoint();
    jwtAuthenticationEntryPoint.commence(request, response, authException);
    verify(response, times(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
  }
}
