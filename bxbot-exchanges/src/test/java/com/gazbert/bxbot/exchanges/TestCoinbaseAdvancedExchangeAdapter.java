/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 gazbert, davidhuertas
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

package com.gazbert.bxbot.exchanges;

import static org.junit.Assert.assertEquals;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.NetworkConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests the behaviour of the Coinbase Advanced Trade Exchange Adapter.
 *
 * @author gazbert, davidhuertas
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
  "javax.crypto.*",
  "javax.management.*",
  "com.sun.org.apache.xerces.*",
  "javax.xml.parsers.*",
  "org.xml.sax.*",
  "org.w3c.dom.*",
  "javax.xml.datatype.*"
})
@PrepareForTest(CoinbaseAdvancedExchangeAdapter.class)
public class TestCoinbaseAdvancedExchangeAdapter extends AbstractExchangeAdapterTest {

  private ExchangeConfig exchangeConfig;
  private AuthenticationConfig authenticationConfig;
  private NetworkConfig networkConfig;
  private OtherConfig otherConfig;

  /** Create some exchange config - the TradingEngine would normally do this. */
  @Before
  public void setupForEachTest() {
    authenticationConfig = PowerMock.createMock(AuthenticationConfig.class);
    networkConfig = PowerMock.createMock(NetworkConfig.class);
    otherConfig = PowerMock.createMock(OtherConfig.class);
    exchangeConfig = PowerMock.createMock(ExchangeConfig.class);
  }

  @Test
  public void testGettingImplNameIsAsExpected() {
    PowerMock.replayAll();
    final CoinbaseAdvancedExchangeAdapter exchangeAdapter = new CoinbaseAdvancedExchangeAdapter();
    exchangeAdapter.init(exchangeConfig);

    assertEquals("Coinbase Advanced Trade REST API v3", exchangeAdapter.getImplName());
    PowerMock.verifyAll();
  }
}
