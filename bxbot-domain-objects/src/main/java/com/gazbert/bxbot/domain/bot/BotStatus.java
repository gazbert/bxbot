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

import com.google.common.base.MoreObjects;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;

/**
 * Domain object representing the Bot's status.
 * 表示 Bot 状态的域对象。
 *
 * @author gazbert
 */
@Schema
public class BotStatus {

  @Schema(required = true, description = "The Bot ID.")
  private String botId;

  @Schema(description = "The friendly name for the Bot.")
  private String displayName;

  @Schema(description = "The Bot's runtime status.")
  private String status;

  @Schema(description = "The current datetime.")
  private Date datetime;

  // Required by ConfigurableComponentFactory
  public BotStatus() {
  }

  /**
   * Creates a new BotStatus.
   * 创建一个新的 BotStatus。
   *
   * @param botId the bot Id.
   *              机器人 ID。
   *
   * @param displayName the bot display name.
   *                    机器人显示名称
   *
   * @param status the bot's status.
   *               机器人的状态
   *
   * @param datetime the current datetime.
   *                 当前日期时间。
   */
  public BotStatus(String botId, String displayName, String status, Date datetime) {
    this.botId = botId;
    this.displayName = displayName;
    this.status = status;
    setDatetime(datetime);
  }

  public String getBotId() {
    return botId;
  }

  public void setBotId(String botId) {
    this.botId = botId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Date getDatetime() {
    return datetime != null ? new Date(datetime.getTime()) : null;
  }

  public void setDatetime(Date datetime) {
    this.datetime = datetime != null ? new Date(datetime.getTime()) : null;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("botId", botId)
        .add("displayName", displayName)
        .add("status", status)
        .add("datetime", getDatetime())
        .toString();
  }
}
