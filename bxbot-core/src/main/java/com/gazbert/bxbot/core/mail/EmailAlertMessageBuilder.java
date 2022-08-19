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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * //Util类用于构建电子邮件警报消息。
 * Util class for building Email Alert messages.
 *
 * @author gazbert
 */
public final class EmailAlertMessageBuilder {

  private static final String NEWLINE = System.getProperty("line.separator");
  private static final String HORIZONTAL_RULE =
      "--------------------------------------------------" + NEWLINE;

  private EmailAlertMessageBuilder() {
  }

  /**
   * Builds critical message content.
   * 构建关键消息内容。
   *
   * @param errorDetails the error details.
   *                     错误详情。
   *
   * @param exception the Exception. 例外。
   * @param botId the bot ID. 机器人 ID。
   * @param botName the bot name. 机器人名称。
   * @param adapterName the Exhange Adapter name. Exchange 适配器名称。
   * @return the error message.  @return 错误信息。
   */
  public static String buildCriticalMsgContent(
      String errorDetails, Throwable exception, String botId, String botName, String adapterName) {

    final StringBuilder msgContent =
        new StringBuilder("A CRITICAL error event has occurred on BX-bot. BX-bot 上发生了严重错误事件。");
    msgContent.append(NEWLINE).append(NEWLINE);

    msgContent.append(HORIZONTAL_RULE);
    msgContent.append("Bot Id / Name: 机器人 ID/名称：");
    msgContent.append(NEWLINE).append(NEWLINE);
    msgContent.append(botId);
    msgContent.append(" / ");
    msgContent.append(botName);
    msgContent.append(NEWLINE).append(NEWLINE);

    msgContent.append(HORIZONTAL_RULE);
    msgContent.append("Exchange Adapter: 交换适配器：");
    msgContent.append(NEWLINE).append(NEWLINE);
    msgContent.append(adapterName);
    msgContent.append(NEWLINE).append(NEWLINE);

    msgContent.append(HORIZONTAL_RULE);
    msgContent.append("Event Time: 活动时间：");
    msgContent.append(NEWLINE).append(NEWLINE);
    msgContent.append(new Date());
    msgContent.append(NEWLINE).append(NEWLINE);

    msgContent.append(HORIZONTAL_RULE);
    msgContent.append("Event Details: 活动详情：");
    msgContent.append(NEWLINE).append(NEWLINE);
    msgContent.append(errorDetails);
    msgContent.append(NEWLINE).append(NEWLINE);

    msgContent.append(HORIZONTAL_RULE);
    msgContent.append("Action Taken: 采取的行动：");
    msgContent.append(NEWLINE).append(NEWLINE);
    msgContent.append("The bot will shut down NOW! Check the bot logs for more information. 机器人现在将关闭！检查机器人日志以获取更多信息。");
    msgContent.append(NEWLINE).append(NEWLINE);

    if (exception != null) {
      msgContent.append(HORIZONTAL_RULE);
      msgContent.append("Stacktrace: 堆栈跟踪：");
      msgContent.append(NEWLINE).append(NEWLINE);
      final StringWriter stringWriter = new StringWriter();
      final PrintWriter printWriter = new PrintWriter(stringWriter);
      exception.printStackTrace(printWriter);
      msgContent.append(stringWriter.toString());
    }

    return msgContent.toString();
  }
}
