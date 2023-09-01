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

package com.gazbert.bxbot.rest.api.security.service;

import static org.easymock.EasyMock.expect;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gazbert.bxbot.rest.api.security.repository.UserRepository;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Tests the JWT User Details service behaves as expected.
 *
 * @author gazbert
 */
class TestJwtUserDetailsService {

  private static final String UNKNOWN_USERNAME = "unknown-username";

  private UserRepository userRepository;

  @BeforeEach
  void setup() {
    userRepository = EasyMock.createMock(UserRepository.class);
  }

  @Test
  void testServiceCreationIsAsExpected() {
    final JwtUserDetailsService jwtUserDetailsService = new JwtUserDetailsService(userRepository);
    assertNotNull(jwtUserDetailsService);
  }

  @Test
  void whenLoadByUsernameCalledWithUnknownUsernameThenExpectUsernameNotFoundException() {
    expect(userRepository.findByUsername(UNKNOWN_USERNAME)).andStubReturn(null);
    EasyMock.replay(userRepository);

    final JwtUserDetailsService jwtUserDetailsService = new JwtUserDetailsService(userRepository);
    assertThrows(
        UsernameNotFoundException.class,
        () -> jwtUserDetailsService.loadUserByUsername(UNKNOWN_USERNAME));

    EasyMock.verify(userRepository);
  }
}
