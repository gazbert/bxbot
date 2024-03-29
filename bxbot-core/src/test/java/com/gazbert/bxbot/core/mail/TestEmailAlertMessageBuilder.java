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

package com.gazbert.bxbot.core.mail;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests the behaviour of the Email Alert Message Builder is as expected.
 *
 * @author gazbert
 */
class TestEmailAlertMessageBuilder {

  private static final String NEWLINE = System.getProperty("line.separator");

  /*
   * Tough one to test with timestamp + stacktrace, so best (lazy!) effort here.
   */
  @Test
  void testBuildingCriticalMsgContent() {

    final String expectedStartOfMsg =
        "A CRITICAL error event has occurred on BX-bot."
            + NEWLINE
            + NEWLINE
            + "--------------------------------------------------"
            + NEWLINE
            + "Bot Id / Name:"
            + NEWLINE
            + NEWLINE
            + "seti-alpha-3 / The Pioneer"
            + NEWLINE
            + NEWLINE
            + "--------------------------------------------------"
            + NEWLINE
            + "Exchange Adapter:"
            + NEWLINE
            + NEWLINE
            + "Bitstamp"
            + NEWLINE
            + NEWLINE
            + "--------------------------------------------------"
            + NEWLINE
            + "Event Time:"
            + NEWLINE
            + NEWLINE;

    final String expectedMiddleOfMsg =
        NEWLINE
            + "--------------------------------------------------"
            + NEWLINE
            + "Event Details:"
            + NEWLINE
            + NEWLINE
            + "The trouble with Tribbles..."
            + NEWLINE
            + NEWLINE
            + "--------------------------------------------------"
            + NEWLINE
            + "Action Taken:"
            + NEWLINE
            + NEWLINE
            + "The bot will shut down NOW! Check the bot logs for more information."
            + NEWLINE
            + NEWLINE
            + "--------------------------------------------------"
            + NEWLINE
            + "Stacktrace:"
            + NEWLINE;

    final String errorMsg = "The trouble with Tribbles...";
    final Exception exception = new RuntimeException(errorMsg);
    final String botId = "seti-alpha-3";
    final String botName = "The Pioneer";
    final String adapterName = "Bitstamp";

    final String msgContent =
        EmailAlertMessageBuilder.buildCriticalMsgContent(
            errorMsg, exception, botId, botName, adapterName);

    assertTrue(msgContent.startsWith(expectedStartOfMsg));
    assertTrue(msgContent.contains(expectedMiddleOfMsg));
  }
}
