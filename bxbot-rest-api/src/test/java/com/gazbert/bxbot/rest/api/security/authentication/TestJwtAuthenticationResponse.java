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
 * Tests a JWT Authentication Response behaves as expected.
 * 测试 JWT 身份验证响应的行为是否符合预期。
 *
 * @author gazbert
 */
class TestJwtAuthenticationResponse {

  private static final String JWT = "the.jwt.string";
  private static final String ANOTHER_JWT = "another.jwt.string";

  @Test
  void testEmptyConstructorWorksAsExpected() {
    final JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
    assertEquals("", jwtAuthenticationResponse.getToken());
  }

  @Test
  void testSetterAndGettersWorkAsExpected() {
    final JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse(JWT);
    jwtAuthenticationResponse.setToken(ANOTHER_JWT);
    assertEquals(ANOTHER_JWT, jwtAuthenticationResponse.getToken());
  }
}
