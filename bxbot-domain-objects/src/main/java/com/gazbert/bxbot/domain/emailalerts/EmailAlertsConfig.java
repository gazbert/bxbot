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
 *
 * @author gazbert
 */
@Schema
public class EmailAlertsConfig {

  @Schema(
      required = true,
      description =
          "If set to true, the bot will send email alerts if it needs to shut down due to a "
              + " critical error.")
  private boolean enabled;

  @Schema(description = "The SMTP details. Only required if enabled is set to true.")
  private SmtpConfig smtpConfig;

  /** Creates a new EmailAlertsConfig. Required by ConfigurableComponentFactory. */
  public EmailAlertsConfig() {
  }

  /**
   * Creates a new EmailAlertsConfig.
   *
   * @param enabled is enabled?
   * @param smtpConfig the SMTP config.
   */
  public EmailAlertsConfig(boolean enabled, SmtpConfig smtpConfig) {
    this.enabled = enabled;
    this.smtpConfig = smtpConfig;
  }

  /**
   * Returns alerts enabled.
   *
   * @return alerts enabled.
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets alerts enabled.
   *
   * @param enabled alerts enabled.
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns the SMTP config.
   *
   * @return the SMTP config.
   */
  public SmtpConfig getSmtpConfig() {
    return smtpConfig;
  }

  /**
   * Sets the SMTP config.
   *
   * @param smtpConfig the SMTP config.
   */
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
