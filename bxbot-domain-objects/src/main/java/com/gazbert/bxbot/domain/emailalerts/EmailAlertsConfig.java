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

package com.gazbert.bxbot.domain.emailalerts;

import com.google.common.base.MoreObjects;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Domain object representing the Email Alerts config.
 * 表示电子邮件警报配置的域对象。
 *
 * @author gazbert
 */
@Schema
public class EmailAlertsConfig {
//  描述 =
//          “如果设置为 true，如果机器人因以下原因需要关闭，它将发送电子邮件警报”
//          +“严重错误。”）
  @Schema(
      required = true,
      description =
          "If set to true, the bot will send email alerts if it needs to shut down due to a  critical error. 如果设置为 true，如果机器人因严重错误而需要关闭，它将发送电子邮件警报。")

  private boolean enabled;

  @Schema(description = "The SMTP details. Only required if enabled is set to true. SMTP 详细信息。仅当启用设置为 true 时才需要。") //@Schema(description = "SMTP 详细信息。仅当启用设置为 true 时才需要。")
  private SmtpConfig smtpConfig;

  // Required by ConfigurableComponentFactory  // ConfigurableComponentFactory 需要
  public EmailAlertsConfig() {
  }

  /**
   * Creates a new EmailAlertsConfig.
   * 创建一个新的 EmailAlertsConfig。
   *
   * @param enabled is enabled?
   *                是否启用？
   *
   * @param smtpConfig the SMTP config.
   *                   SMTP 配置。
   */
  public EmailAlertsConfig(boolean enabled, SmtpConfig smtpConfig) {
    this.enabled = enabled;
    this.smtpConfig = smtpConfig;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public SmtpConfig getSmtpConfig() {
    return smtpConfig;
  }

  public void setSmtpConfig(SmtpConfig smtpConfig) {
    this.smtpConfig = smtpConfig;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("enabled", enabled)
        .add("smtpConfig", smtpConfig)
        .toString();
  }
}
