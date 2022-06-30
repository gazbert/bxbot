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

package com.gazbert.bxbot.domain.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests a EngineConfig domain object behaves as expected.
 * 测试 EngineConfig 域对象的行为是否符合预期。
 *
 * @author gazbert
 */
class TestEngineConfig {

  private static final String BOT_ID = "avro-707_1";
  private static final String BOT_NAME = "Avro 707";
  private static final String EMERGENCY_STOP_CURRENCY = "BTC";
  private static final BigDecimal EMERGENCY_STOP_BALANCE = new BigDecimal("1.5");
  private static final int TRADE_CYCLE_INTERVAL = 30;

  @Test
  void testInitialisationWorksAsExpected() {
    final EngineConfig engineConfig =
        new EngineConfig(
            BOT_ID,
            BOT_NAME,
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    assertEquals(BOT_ID, engineConfig.getBotId());
    assertEquals(BOT_NAME, engineConfig.getBotName());
    assertEquals(EMERGENCY_STOP_CURRENCY, engineConfig.getEmergencyStopCurrency());
    assertEquals(EMERGENCY_STOP_BALANCE, engineConfig.getEmergencyStopBalance());
    assertEquals(TRADE_CYCLE_INTERVAL, engineConfig.getTradeCycleInterval());
  }

  @Test
  void testSettersWorkAsExpected() {
    final EngineConfig engineConfig = new EngineConfig();
    assertNull(engineConfig.getBotId());
    assertNull(engineConfig.getBotName());
    assertNull(engineConfig.getEmergencyStopCurrency());
    assertNull(engineConfig.getEmergencyStopBalance());
    assertEquals(0, engineConfig.getTradeCycleInterval());

    engineConfig.setBotId(BOT_ID);
    assertEquals(BOT_ID, engineConfig.getBotId());

    engineConfig.setBotName(BOT_NAME);
    assertEquals(BOT_NAME, engineConfig.getBotName());

    engineConfig.setEmergencyStopCurrency(EMERGENCY_STOP_CURRENCY);
    assertEquals(EMERGENCY_STOP_CURRENCY, engineConfig.getEmergencyStopCurrency());

    engineConfig.setEmergencyStopBalance(EMERGENCY_STOP_BALANCE);
    assertEquals(EMERGENCY_STOP_BALANCE, engineConfig.getEmergencyStopBalance());

    engineConfig.setTradeCycleInterval(TRADE_CYCLE_INTERVAL);
    assertEquals(TRADE_CYCLE_INTERVAL, engineConfig.getTradeCycleInterval());
  }

  @Test
  void testEqualsWorksAsExpected() {
    final EngineConfig engineConfig1 =
        new EngineConfig(
            BOT_ID,
            BOT_NAME,
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    final EngineConfig engineConfig2 =
        new EngineConfig(
            "different-id",
            BOT_NAME,
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    final EngineConfig engineConfig3 =
        new EngineConfig(
            BOT_ID,
            "different-name",
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    assertEquals(engineConfig1, engineConfig1);
    assertNotEquals(engineConfig1, engineConfig2);
    assertEquals(engineConfig1, engineConfig3);
  }

  @Test
  void testHashCodeWorksAsExpected() {
    final EngineConfig engineConfig1 =
        new EngineConfig(
            BOT_ID,
            BOT_NAME,
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    final EngineConfig engineConfig2 =
        new EngineConfig(
            "different-id",
            BOT_NAME,
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    final EngineConfig engineConfig3 =
        new EngineConfig(
            BOT_ID,
            "different-name",
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    assertEquals(engineConfig1.hashCode(), engineConfig1.hashCode());
    assertNotEquals(engineConfig1.hashCode(), engineConfig2.hashCode());
    assertEquals(engineConfig1.hashCode(), engineConfig3.hashCode());
  }

  @Test
  void testToStringWorksAsExpected() {
    final EngineConfig engineConfig =
        new EngineConfig(
            BOT_ID,
            BOT_NAME,
            EMERGENCY_STOP_CURRENCY,
            EMERGENCY_STOP_BALANCE,
            TRADE_CYCLE_INTERVAL);

    assertEquals(
        "EngineConfig{botId=avro-707_1, botName=Avro 707, emergencyStopCurrency=BTC, "
            + "emergencyStopBalance=1.5, tradeCycleInterval=30}",
        engineConfig.toString());
  }
}
