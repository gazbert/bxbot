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
import javax.validation.constraints.Email;
import javax.validation.constraints.Positive;

/**
 * Domain object representing the SMTP config used for Email Alerts.
 * 表示用于电子邮件警报的 SMTP 配置的域对象。
 *
 * @author gazbert
 */
@Schema(required = true)
public class SmtpConfig {

  @Schema(required = true, description = "The SMTP hostname. SMTP 主机名。")
  private String host;

  @Schema(required = true, description = "The SMTP TLS port. SMTP TLS 端口。")
  @Positive(message = "Port must be positive integer 端口必须是正整数") //@Positive(message = "端口必须是正整数")
  private int tlsPort;

  @Schema(required = true, description = "发件人的电子邮件帐户名称。")
  private String accountUsername;

  @Schema(required = true, description = "The sender email account password. 发件人电子邮件帐户密码。")  //@Schema(required = true, description = "发件人电子邮件帐户密码。")
  private String accountPassword;

  @Schema(required = true, description = "The email From address. 电子邮件发件人地址。")
  @Email(message = " From Address must be a valid email address 发件人地址必须是有效的电子邮件地址")
  private String fromAddress;

  @Schema(required = true, description = "The email To address. 电子邮件地址。")
  @Email(message = "To Address must be a valid email address 收件人地址必须是有效的电子邮件地址")
  private String toAddress;

  // required for jackson  //jackson需要
  public SmtpConfig() {
  }

  /**
   * Creates a new SmtpConfig.
   * 创建一个新的 SmtpConfig。
   *
   * @param host the SMTP host.
   *             SMTP 主机。
   *
   * @param tlsPort the TLS port to use.
   *                要使用的 TLS 端口。
   *
   * @param accountUsername the SMTP account name.
   *                        accountUsername SMTP 帐户名称。
   *
   * @param accountPassword the SMTP account password.
   *                        SMTP 帐户密码。
   *
   * @param fromAddress the email From address.
   *                    电子邮件发件人地址。
   *
   * @param toAddress the email To address.
   *                  电子邮件地址。
   */
  public SmtpConfig(
      String host,
      int tlsPort,
      String accountUsername,
      String accountPassword,
      String fromAddress,
      String toAddress) {

    this.host = host;
    this.tlsPort = tlsPort;
    this.accountUsername = accountUsername;
    this.accountPassword = accountPassword;
    this.fromAddress = fromAddress;
    this.toAddress = toAddress;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getTlsPort() {
    return tlsPort;
  }

  public void setTlsPort(int tlsPort) {
    this.tlsPort = tlsPort;
  }

  public String getAccountUsername() {
    return accountUsername;
  }

  public void setAccountUsername(String accountUsername) {
    this.accountUsername = accountUsername;
  }

  public String getAccountPassword() {
    return accountPassword;
  }

  public void setAccountPassword(String accountPassword) {
    this.accountPassword = accountPassword;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getToAddress() {
    return toAddress;
  }

  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("host", host)
        .add("tlsPort", tlsPort)
        .add("accountUsername", accountUsername)
        // accountPassword is not included
        .add("fromAddress", fromAddress)
        .add("toAddress", toAddress)
        .toString();
  }
}
