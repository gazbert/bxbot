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
import java.math.BigDecimal;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;

/**
 * Domain object representing the Engine config.
 * 表示引擎配置的域对象。
 *
 * @author gazbert
 */
@Schema
public class EngineConfig {

  @Schema(
      required = true,
      description =
          "A unique identifier for the bot. Value must be an alphanumeric string."
              + " Underscores and dashes are also permitted.")
  private String botId;

  @Schema(description = "A friendly name for the bot. Value must be an alphanumeric string. 机器人的友好名称。值必须是字母数字字符串。"
      + "Spaces are allowed. 允许有空格。”")
  private String botName;

  @Schema(
      required = true,
      description =
          "This is normally the currency you intend to hold a long position in. It should be set to the currency short code for the wallet, e.g. BTC, LTC, USD. 这通常是您打算持有多头头寸的货币。它应该设置为钱包的货币短代码，例如比特币、莱特币、美元。")
  private String emergencyStopCurrency;

  @Schema(
      required = true,
      description =
          "The Trading Engine checks this value at the start of every trade cycle: 交易引擎在每个交易周期开始时检查此值："
              + " if your emergencyStopCurrency wallet balance on the exchange drops below this value, the Trading Engine will log it, send an Email Alert (if configured) and then shut down. "
                  +"如果您在交易所的 EmergencyStopCurrency 钱包余额低于此值，交易引擎将记录它，发送电子邮件警报（如果已配置），然后关闭。"
                  +"If you set this value to 0, the bot will bypass the check - be careful. 如果将此值设置为 0，机器人将绕过检查 - 请小心。")
  @DecimalMin(message = "Emergency Stop Balance must be 0 or more 紧急停止余额必须为 0 或更大", value = "0")
  private BigDecimal emergencyStopBalance;

  @Schema(
      required = true,
      description =
          "The interval in (secs) that the Trading Engine will wait/sleep before executing each trade cycle. The minimum value is 1 second. 交易引擎在执行每个交易周期之前等待/休眠的时间间隔（秒）。最小值为 1 秒。")
  @Min(value = 1, message = "Trace Cycle Interval must be more than 1 second . Trace Cycle Interval 必须大于 1 秒")
  private int tradeCycleInterval;

  // Required by ConfigurableComponentFactory  // ConfigurableComponentFactory 需要
  public EngineConfig() {
  }

  /**
   * Creates an EngineConfig.
   * 创建引擎配置。
   *
   * @param botId the bot ID.
   * @param botName the bot name.
   * @param emergencyStopCurrency  the emergency stop currency.
   *                               紧急停止货币。
   *
   * @param emergencyStopBalance the emergency stop balance.
   *                             紧急停止账户余额
   *
   * @param tradeCycleInterval the trade cycle interval (in secs).
   *                           交易周期间隔（以秒为单位）。
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

  public String getBotId() {
    return botId;
  }

  public void setBotId(String botId) {
    this.botId = botId;
  }

  public String getBotName() {
    return botName;
  }

  public void setBotName(String botName) {
    this.botName = botName;
  }

  public String getEmergencyStopCurrency() {
    return emergencyStopCurrency;
  }

  public void setEmergencyStopCurrency(String emergencyStopCurrency) {
    this.emergencyStopCurrency = emergencyStopCurrency;
  }

  public BigDecimal getEmergencyStopBalance() {
    return emergencyStopBalance;
  }

  public void setEmergencyStopBalance(BigDecimal emergencyStopBalance) {
    this.emergencyStopBalance = emergencyStopBalance;
  }

  public int getTradeCycleInterval() {
    return tradeCycleInterval;
  }

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
