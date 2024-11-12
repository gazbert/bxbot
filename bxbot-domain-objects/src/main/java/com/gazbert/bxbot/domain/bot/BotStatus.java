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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import lombok.Data;

/**
 * Domain object representing the Bot's status.
 *
 * @author gazbert
 */
@Data
@Schema
public class BotStatus {

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "The Bot ID.")
  private String botId;

  @Schema(description = "The friendly name for the Bot.")
  private String displayName;

  @Schema(description = "The Bot's runtime status.")
  private String status;

  @Schema(description = "The current datetime.")
  private Date datetime;

  /** Creates a new BotStatus. Required by ConfigurableComponentFactory. */
  public BotStatus() {
    // noimpl
  }

  /**
   * Creates a new BotStatus.
   *
   * @param botId the bot id.
   * @param displayName the bot display name.
   * @param status the bot's status.
   * @param datetime the current datetime.
   */
  public BotStatus(String botId, String displayName, String status, Date datetime) {
    this.botId = botId;
    this.displayName = displayName;
    this.status = status;
    setDatetime(datetime);
  }

  /**
   * Returns the datetime.
   *
   * @return the datetime.
   */
  public Date getDatetime() {
    return datetime != null ? new Date(datetime.getTime()) : null;
  }

  /**
   * Sets the datetime.
   *
   * @param datetime the datetime.
   */
  public void setDatetime(Date datetime) {
    this.datetime = datetime != null ? new Date(datetime.getTime()) : null;
  }
}
