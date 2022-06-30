/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

package com.gazbert.bxbot.domain.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests NetworkConfig domain object behaves as expected.
 * 测试 NetworkConfig 域对象的行为是否符合预期。
 *
 * @author gazbert
 */
class TestNetworkConfig {

  private static final Integer CONNECTION_TIMEOUT = 30;
  private static final List<Integer> NON_FATAL_ERROR_CODES = Arrays.asList(502, 503, 504);
  private static final List<String> NON_FATAL_ERROR_MESSAGES =
      Arrays.asList(
          "Connection refused",
          "Connection reset",
          "Remote host closed connection during handshake 握手期间远程主机关闭连接");

  @Test
  void testInitialisationWorksAsExpected() {
    final NetworkConfig networkConfig = new NetworkConfig();
    assertNull(networkConfig.getConnectionTimeout());
    assertTrue(networkConfig.getNonFatalErrorCodes().isEmpty());
    assertTrue(networkConfig.getNonFatalErrorMessages().isEmpty());
  }

  @Test
  void testSettersWorkAsExpected() {
    final NetworkConfig networkConfig = new NetworkConfig();

    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    assertEquals(CONNECTION_TIMEOUT, networkConfig.getConnectionTimeout());

    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    assertEquals(NON_FATAL_ERROR_CODES, networkConfig.getNonFatalErrorCodes());

    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);
    assertEquals(NON_FATAL_ERROR_MESSAGES, networkConfig.getNonFatalErrorMessages());
  }

  @Test
  void testToStringWorksAsExpected() {
    final NetworkConfig networkConfig = new NetworkConfig();
    networkConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
    networkConfig.setNonFatalErrorCodes(NON_FATAL_ERROR_CODES);
    networkConfig.setNonFatalErrorMessages(NON_FATAL_ERROR_MESSAGES);

    assertEquals(
        "NetworkConfig{connectionTimeout=30, nonFatalErrorCodes=[502, 503, 504],"
            + " nonFatalErrorMessages=[Connection refused, Connection reset, "
            + "Remote host closed connection during handshake]}",
        networkConfig.toString());
  }
}
