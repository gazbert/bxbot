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

/**
 * Domain object representing the Email Alerts config.
 *
 * @author gazbert
 */
public class EmailAlertsConfig {

  private boolean enabled;
  private SmtpConfig smtpConfig;

  // Required by ConfigurableComponentFactory
  public EmailAlertsConfig() {
  }

  /** Creates a new EmailAlertsConfig. */
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
