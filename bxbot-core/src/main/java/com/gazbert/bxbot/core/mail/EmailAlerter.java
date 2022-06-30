/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.domain.emailalerts.SmtpConfig;
import com.gazbert.bxbot.services.config.EmailAlertsConfigService;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * //使用SMTP和TLS的简单邮件发件人。它只发送纯文本电子邮件。
 * A simple mail sender using SMTP and TLS. It sends plain/text email only.
 *
 * @author gazbert
 */
@Component
@ComponentScan(basePackages = {"com.gazbert.bxbot.repository"})
public class EmailAlerter {

  private static final Logger LOG = LogManager.getLogger();

  private SmtpConfig smtpConfig;
  private Properties smtpProps;
  private boolean sendEmailAlertsEnabled;

  private final EmailAlertsConfigService emailAlertsConfigService;

  @Autowired
  public EmailAlerter(EmailAlertsConfigService emailAlertsConfigService) {
    this.emailAlertsConfigService = emailAlertsConfigService;
    initialise();
  }

  /**
   * Sends an email message.
   * 发送电子邮件。
   *
   * @param subject the email subject.
   *                电子邮件主题。
   *
   * @param msgContent the email content.
   *                   电子邮件内容。
   */
  public void sendMessage(String subject, String msgContent) {
    if (sendEmailAlertsEnabled) {
      final Session session =
          Session.getInstance(
              smtpProps,
              new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                  return new PasswordAuthentication(
                      smtpConfig.getAccountUsername(), smtpConfig.getAccountPassword());
                }
              });

      try {
        final Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(smtpConfig.getFromAddress()));
        message.setRecipients(
            Message.RecipientType.TO, InternetAddress.parse(smtpConfig.getToAddress()));
        message.setSubject(subject);
        message.setText(msgContent);

        LOG.info(() -> "About to send following Email Alert with message content: 即将发送以下带有消息内容的电子邮件警报：" + msgContent);
        Transport.send(message);

      } catch (MessagingException e) {
        LOG.error(() -> "Failed to send Email Alert. Details: 未能发送电子邮件警报。细节：" + e.getMessage(), e);
      }
    } else {
      LOG.warn(
          () ->
              "Email Alerts are disabled. Not sending the following message: Subject: 电子邮件警报被禁用。未发送以下消息：主题："
                  + subject
                  + " Content: 内容："
                  + msgContent);
    }
  }

  // ------------------------------------------------------------------------
  // Private utils  // 私有工具
  // ------------------------------------------------------------------------

  private void initialise() {
    final EmailAlertsConfig emailAlertsConfig = emailAlertsConfigService.getEmailAlertsConfig();
    if (emailAlertsConfig != null) {
      sendEmailAlertsEnabled = emailAlertsConfig.isEnabled();

      if (sendEmailAlertsEnabled) {
        LOG.info(() -> "Email Alert for emergency bot shutdown is enabled. Loading SMTP config... 已启用紧急机器人关闭的电子邮件警报。正在加载 SMTP 配置...");
        smtpConfig = emailAlertsConfig.getSmtpConfig();

        if (smtpConfig == null) {
          final String errorMsg =
              "Failed to initialise Email Alerter. 无法初始化电子邮件警报。"
                  + "Alerts are enabled but no SMTP Config has been supplied in config. 警报已启用，但配置中未提供 SMTP 配置。";
          throw new IllegalStateException(errorMsg);
        }

        LOG.info(() -> "SMTP host:  MTP主机：" + smtpConfig.getHost());
        LOG.info(() -> "SMTP TLS Port: SMTP TLS 端口：" + smtpConfig.getTlsPort());
        LOG.info(() -> "Account username: 账户用户名：" + smtpConfig.getAccountUsername());
        // Account password not logged intentionally  // 没有刻意记录账户密码
        LOG.info(() -> "From address: 从地址：" + smtpConfig.getFromAddress());
        LOG.info(() -> "To address: 到地址：" + smtpConfig.getToAddress());

        smtpProps = new Properties();
        smtpProps.put("mail.smtp.auth", "true");
        smtpProps.put("mail.smtp.starttls.enable", "true");
        smtpProps.put("mail.smtp.host", smtpConfig.getHost());
        smtpProps.put("mail.smtp.port", smtpConfig.getTlsPort());

      } else {
        LOG.warn(() -> "Email Alerts are disabled. Are you sure you want to configure this?电子邮件警报被禁用。您确定要配置这个吗？ ");
      }
    }
  }
}
