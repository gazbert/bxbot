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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Domain object representing the Engine config.
 *
 * @author gazbert
 */
@Data
@Schema
public class EngineConfig {

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "A unique identifier for the bot. Value must be an alphanumeric string."
              + " Underscores and dashes are also permitted.")
  private String botId;

  @Schema(
      description =
          "A friendly name for the bot. Value must be an alphanumeric string. "
              + "Spaces are allowed.")
  @EqualsAndHashCode.Exclude
  private String botName;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "This is normally the currency you intend to hold a long position in. It should be set "
              + "to the currency short code for the wallet, e.g. BTC, LTC, USD.")
  @EqualsAndHashCode.Exclude
  private String emergencyStopCurrency;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "The Trading Engine checks this value at the start of every trade cycle: if "
              + "your emergencyStopCurrency wallet balance on the exchange drops below this "
              + "value, the Trading Engine will log it, send an Email Alert (if configured)"
              + " and then shut down. If you set this value to 0, the bot will bypass the check"
              + " - be careful.")
  @DecimalMin(message = "Emergency Stop Balance must be 0 or more", value = "0")
  @EqualsAndHashCode.Exclude
  private BigDecimal emergencyStopBalance;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "The interval in (secs) that the Trading Engine will wait/sleep before executing"
              + " each trade cycle. The minimum value is 1 second.")
  @Min(value = 1, message = "Trace Cycle Interval must be more than 1 second")
  @EqualsAndHashCode.Exclude
  private int tradeCycleInterval;

  /** Creates an EngineConfig. Required by ConfigurableComponentFactory */
  public EngineConfig() {
    // noimpl
  }

  /**
   * Creates an EngineConfig.
   *
   * @param botId the bot ID.
   * @param botName the bot name.
   * @param emergencyStopCurrency the emergency stop currency.
   * @param emergencyStopBalance the emergency stop balance.
   * @param tradeCycleInterval the trade cycle interval (in secs).
   */
  public EngineConfig(
      String botId,
      String botName,
      String emergencyStopCurrency,
      BigDecimal emergencyStopBalance,
      int tradeCycleInterval) {

    this.botId = botId;
    this.botName = botName;
    this.emergencyStopCurrency = emergencyStopCurrency;
    this.emergencyStopBalance = emergencyStopBalance;
    this.tradeCycleInterval = tradeCycleInterval;
  }
}
