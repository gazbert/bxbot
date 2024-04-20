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
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * A simple mail sender using SMTP and TLS. It sends plain/text email only.
 *
 * @author gazbert
 */
@Component
@ComponentScan(basePackages = {"com.gazbert.bxbot.repository"})
@Log4j2
public class EmailAlerter {

  private SmtpConfig smtpConfig;
  private Properties smtpProps;
  private boolean sendEmailAlertsEnabled;

  private final EmailAlertsConfigService emailAlertsConfigService;

  /**
   * Creates the Email Alerter.
   *
   * @param emailAlertsConfigService the email alerts config service.
   */
  @Autowired
  public EmailAlerter(EmailAlertsConfigService emailAlertsConfigService) {
    this.emailAlertsConfigService = emailAlertsConfigService;
    initialise();
  }

  /**
   * Sends an email message.
   *
   * @param subject the email subject.
   * @param msgContent the email content.
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

        log.info("About to send following Email Alert with message content: " + msgContent);
        Transport.send(message);

      } catch (MessagingException e) {
        log.error("Failed to send Email Alert. Details: " + e.getMessage(), e);
      }
    } else {
      log.warn(
          "Email Alerts are disabled. Not sending the following message: Subject: "
              + subject
              + " Content: "
              + msgContent);
    }
  }

  // ------------------------------------------------------------------------
  // Private utils
  // ------------------------------------------------------------------------

  private void initialise() {
    final EmailAlertsConfig emailAlertsConfig = emailAlertsConfigService.getEmailAlertsConfig();
    if (emailAlertsConfig != null) {
      sendEmailAlertsEnabled = emailAlertsConfig.isEnabled();

      if (sendEmailAlertsEnabled) {
        log.info("Email Alert for emergency bot shutdown is enabled. Loading SMTP config...");
        smtpConfig = emailAlertsConfig.getSmtpConfig();

        if (smtpConfig == null) {
          final String errorMsg =
              "Failed to initialise Email Alerter. "
                  + "Alerts are enabled but no SMTP Config has been supplied in config.";
          log.error(errorMsg);
        }

        log.info("SMTP host: " + smtpConfig.getHost());
        log.info("SMTP TLS Port: " + smtpConfig.getTlsPort());
        log.info("Account username: " + smtpConfig.getAccountUsername());
        // Account password not logged intentionally
        log.info("From address: " + smtpConfig.getFromAddress());
        log.info("To address: " + smtpConfig.getToAddress());

        smtpProps = new Properties();
        smtpProps.put("mail.smtp.auth", "true");
        smtpProps.put("mail.smtp.starttls.enable", "true");
        smtpProps.put("mail.smtp.host", smtpConfig.getHost());
        smtpProps.put("mail.smtp.port", smtpConfig.getTlsPort());

      } else {
        log.warn("Email Alerts are disabled. Are you sure you want to configure this?");
      }
    }
  }
}
