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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * Domain object representing the Engine config.
 *
 * @author gazbert
 */
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
  private String botName;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "This is normally the currency you intend to hold a long position in. It should be set "
              + "to the currency short code for the wallet, e.g. BTC, LTC, USD.")
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
  private BigDecimal emergencyStopBalance;

  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      description =
          "The interval in (secs) that the Trading Engine will wait/sleep before executing"
              + " each trade cycle. The minimum value is 1 second.")
  @Min(value = 1, message = "Trace Cycle Interval must be more than 1 second")
  private int tradeCycleInterval;

  /** Creates an EngineConfig. Required by ConfigurableComponentFactory */
  public EngineConfig() {
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

  /**
   * Returns the bot id.
   *
   * @return the bot id.
   */
  public String getBotId() {
    return botId;
  }

  /**
   * Sets the bot id.
   *
   * @param botId the bot id.
   */
  public void setBotId(String botId) {
    this.botId = botId;
  }

  /**
   * Returns the bot name.
   *
   * @return the bot name.
   */
  public String getBotName() {
    return botName;
  }

  /**
   * Sets the bot name.
   *
   * @param botName the bot name.
   */
  public void setBotName(String botName) {
    this.botName = botName;
  }

  /**
   * Returns the emergency stop currency.
   *
   * @return the emergency stop currency.
   */
  public String getEmergencyStopCurrency() {
    return emergencyStopCurrency;
  }

  /**
   * Sets the emergency stop currency.
   *
   * @param emergencyStopCurrency the emergency stop currency.
   */
  public void setEmergencyStopCurrency(String emergencyStopCurrency) {
    this.emergencyStopCurrency = emergencyStopCurrency;
  }

  /**
   * Returns the emergency stop balance.
   *
   * @return the emergency stop balance.
   */
  public BigDecimal getEmergencyStopBalance() {
    return emergencyStopBalance;
  }

  /**
   * Sets the emergency stop balance.
   *
   * @param emergencyStopBalance the emergency stop balance.
   */
  public void setEmergencyStopBalance(BigDecimal emergencyStopBalance) {
    this.emergencyStopBalance = emergencyStopBalance;
  }

  /**
   * Returns the trade cycle interval.
   *
   * @return the trade cycle interval.
   */
  public int getTradeCycleInterval() {
    return tradeCycleInterval;
  }

  /**
   * Sets the trade cycle interval.
   *
   * @param tradeCycleInterval the trade cycle interval.
   */
  public void setTradeCycleInterval(int tradeCycleInterval) {
    this.tradeCycleInterval = tradeCycleInterval;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final EngineConfig that = (EngineConfig) o;
    return Objects.equal(botId, that.botId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(botId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("botId", botId)
        .add("botName", botName)
        .add("emergencyStopCurrency", emergencyStopCurrency)
        .add("emergencyStopBalance", emergencyStopBalance)
        .add("tradeCycleInterval", tradeCycleInterval)
        .toString();
  }
}
