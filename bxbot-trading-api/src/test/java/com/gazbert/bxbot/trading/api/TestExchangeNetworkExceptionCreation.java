/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.trading.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests Exchange Network Exception is created as expected.
 * * 按预期创建测试 Exchange 网络异常。
 *
 * @author gazbert
 */
class TestExchangeNetworkExceptionCreation {

  private static final String ERROR_MSG = "Network timeout connecting to exchange 连接交换的网络超时";
  private static final RuntimeException CAUSE = new RuntimeException("The cause of the exception");

  @Test
  void testCreationOfExceptionIsAsExpected() {
    final ExchangeNetworkException exception = new ExchangeNetworkException(ERROR_MSG);
    assertEquals(ERROR_MSG, exception.getMessage());
  }

  @Test
  void testCreationOfExceptionWithCauseIsAsExpected() {
    final ExchangeNetworkException exception = new ExchangeNetworkException(ERROR_MSG, CAUSE);
    assertEquals(ERROR_MSG, exception.getMessage());
    assertEquals(CAUSE, exception.getCause());
  }
}
