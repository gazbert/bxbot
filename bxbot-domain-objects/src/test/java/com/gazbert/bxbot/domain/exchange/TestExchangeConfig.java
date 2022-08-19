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

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests ExchangeConfig domain object behaves as expected.
 * 测试 ExchangeConfig 域对象的行为是否符合预期。
 *
 * @author gazbert
 */
class TestExchangeConfig {

  private static final String EXCHANGE_NAME = "Bitstamp";
  private static final String EXCHANGE_ADAPTER = "com.gazbert.bxbot.exchanges.TestExchangeAdapter";
  private static final Map<String, String> AUTHENTICATION_CONFIG = new HashMap<>();
  private static final NetworkConfig NETWORK_CONFIG = new NetworkConfig();
  private static final Map<String, String> OTHER_CONFIG = new HashMap<>();

  @Test
  void testInitialisationWorksAsExpected() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    assertNull(exchangeConfig.getName());
    assertNull(exchangeConfig.getAdapter());
    assertNull(exchangeConfig.getAuthenticationConfig());
    assertNull(exchangeConfig.getNetworkConfig());
    assertNull(exchangeConfig.getOtherConfig());
  }

  @Test
  void testSettersWorkAsExpected() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();

    exchangeConfig.setName(EXCHANGE_NAME);
    assertEquals(EXCHANGE_NAME, exchangeConfig.getName());

    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    assertEquals(EXCHANGE_ADAPTER, exchangeConfig.getAdapter());

    exchangeConfig.setAuthenticationConfig(AUTHENTICATION_CONFIG);
    assertEquals(AUTHENTICATION_CONFIG, exchangeConfig.getAuthenticationConfig());

    exchangeConfig.setNetworkConfig(NETWORK_CONFIG);
    assertEquals(NETWORK_CONFIG, exchangeConfig.getNetworkConfig());

    exchangeConfig.setOtherConfig(OTHER_CONFIG);
    assertEquals(OTHER_CONFIG, exchangeConfig.getOtherConfig());
  }

  @Test
  void testToStringWorksAsExpected() {
    final ExchangeConfig exchangeConfig = new ExchangeConfig();
    exchangeConfig.setName(EXCHANGE_NAME);
    exchangeConfig.setAdapter(EXCHANGE_ADAPTER);
    exchangeConfig.setAuthenticationConfig(AUTHENTICATION_CONFIG);
    exchangeConfig.setNetworkConfig(NETWORK_CONFIG);
    exchangeConfig.setOtherConfig(OTHER_CONFIG);

    assertEquals(
        "ExchangeConfig{name=Bitstamp, "
            + "adapter=com.gazbert.bxbot.exchanges.TestExchangeAdapter, "
            + "networkConfig=NetworkConfig{connectionTimeout=null, nonFatalErrorCodes=[], "
            + "nonFatalErrorMessages=[]}, otherConfig={}}",
        exchangeConfig.toString());
  }
}
