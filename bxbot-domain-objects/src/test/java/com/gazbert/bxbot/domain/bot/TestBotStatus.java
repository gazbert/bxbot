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

package com.gazbert.bxbot.domain.bot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests a BotStatus domain object behaves as expected.
 *
 * @author gazbert
 */
public class TestBotStatus {

  private static final String BOT_ID = "avro-707_1";
  private static final String DISPLAY_NAME = "Avro 707";
  private static final String STATUS = "running";

  @Test
  public void testInitialisationWorksAsExpected() {
    final BotStatus botStatus = new BotStatus(BOT_ID, DISPLAY_NAME, STATUS);
    assertEquals(BOT_ID, botStatus.getBotId());
    assertEquals(DISPLAY_NAME, botStatus.getDisplayName());
    assertEquals(STATUS, botStatus.getStatus());
  }

  @Test
  public void testSettersWorkAsExpected() {
    final BotStatus botStatus = new BotStatus();
    assertNull(botStatus.getBotId());
    assertNull(botStatus.getDisplayName());
    assertNull(botStatus.getStatus());

    botStatus.setBotId(BOT_ID);
    assertEquals(BOT_ID, botStatus.getBotId());

    botStatus.setDisplayName(DISPLAY_NAME);
    assertEquals(DISPLAY_NAME, botStatus.getDisplayName());

    botStatus.setStatus(STATUS);
    assertEquals(STATUS, botStatus.getStatus());
  }
}
