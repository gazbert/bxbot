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

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests a JWT Authentication Request behaves as expected.
 * 测试 JWT 身份验证请求的行为是否符合预期。
 *
 * @author gazbert
 */
class TestJwtAuthenticationRequest {

  private static final String USERNAME = "bxbot-ui";
  private static final String PASSWORD = "InSearchOfLostTime";
  private static final String USERNAME2 = "bxbot-ui-2";
  private static final String PASSWORD2 = "InSearchOfLostTime2";

  @Test
  void testEmptyConstructorWorksAsExpected() {
    final JwtAuthenticationRequest jwtAuthenticationRequest = new JwtAuthenticationRequest();
    assertEquals("", jwtAuthenticationRequest.getUsername());
    assertEquals("", jwtAuthenticationRequest.getPassword());
  }

  @Test
  void testArgsConstructorWorksAsExpected() {
    final JwtAuthenticationRequest jwtAuthenticationRequest =
        new JwtAuthenticationRequest(USERNAME, PASSWORD);
    assertEquals(USERNAME, jwtAuthenticationRequest.getUsername());
    assertEquals(PASSWORD, jwtAuthenticationRequest.getPassword());
  }

  @Test
  void testSettersWorkAsExpected() {
    final JwtAuthenticationRequest jwtAuthenticationRequest =
        new JwtAuthenticationRequest(USERNAME, PASSWORD);
    jwtAuthenticationRequest.setUsername(USERNAME2);
    assertEquals(USERNAME2, jwtAuthenticationRequest.getUsername());
    jwtAuthenticationRequest.setPassword(PASSWORD2);
    assertEquals(PASSWORD2, jwtAuthenticationRequest.getPassword());
  }
}
