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

package com.gazbert.bxbot.strategy.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests Strategy Exception is created as expected.
 * 测试策略异常按预期创建。
 *
 * @author gazbert
 */
class TestStrategyExceptionCreation {

  private static final String ERROR_MSG =
      "Received unknown order id in current active orders API call 在当前活动订单 API 调用中收到未知订单 ID";
  private static final RuntimeException CAUSE = new RuntimeException("The cause of the exception 异常的原因");

  @Test
  void testCreationOfExceptionIsAsExpected() {
    final StrategyException exception = new StrategyException(ERROR_MSG);
    assertEquals(ERROR_MSG, exception.getMessage());
  }

  @Test
  void testCreationOfExceptionWithCauseIsAsExpected() {
    final StrategyException exception = new StrategyException(ERROR_MSG, CAUSE);
    assertEquals(ERROR_MSG, exception.getMessage());
    assertEquals(CAUSE, exception.getCause());
  }

  @Test
  void testCreationOfExceptionWithThrowableIsAsExpected() {
    final StrategyException exception = new StrategyException(CAUSE);
    assertEquals(CAUSE, exception.getCause());
  }
}
