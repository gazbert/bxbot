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
import com.gazbert.bxbot.services.EmailAlertsConfigService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
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

    public void sendMessage(String subject, String msgContent) {

        if (sendEmailAlertsEnabled) {

            final Session session = Session.getInstance(smtpProps, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpConfig.getAccountUsername(), smtpConfig.getAccountPassword());
                }
            });

            try {
                final Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(smtpConfig.getFromAddress()));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(smtpConfig.getToAddress()));
                message.setSubject(subject);
                message.setText(msgContent);

                LOG.info(() -> "About to send following Email Alert with message content: " + msgContent);
                Transport.send(message);

            } catch (MessagingException e) {
                // not much we can do here, especially if the alert was critical - the bot is shutting down; just log it.
                LOG.error("Failed to send Email Alert. Details: " + e.getMessage(), e);
            }
        } else {
            LOG.warn("Email Alerts are disabled. Not sending the following message: Subject: "
                    + subject + " Content: " + msgContent);
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

                LOG.info(() -> "Email Alert for emergency bot shutdown is enabled. Loading SMTP config...");

                smtpConfig = emailAlertsConfig.getSmtpConfig();

                if (smtpConfig == null) {
                    final String errorMsg = "Failed to initialise Email Alerter. " +
                            "Alerts are enabled but no SMTP Config has been supplied in config.";
                    throw new IllegalStateException(errorMsg);
                }

                LOG.info(() -> "SMTP host: " + smtpConfig.getHost());
                LOG.info(() -> "SMTP TLS Port: " + smtpConfig.getTlsPort());
                LOG.info(() -> "Account username: " + smtpConfig.getAccountUsername());
                // uncomment below for testing only
//                    LOG.info( () -> "Account password: " + smtpConfig.getAccountPassword());

                LOG.info(() -> "From address: " + smtpConfig.getFromAddress());
                LOG.info(() -> "To address: " + smtpConfig.getToAddress());

                smtpProps = new Properties();
                smtpProps.put("mail.smtp.auth", "true");
                smtpProps.put("mail.smtp.starttls.enable", "true");
                smtpProps.put("mail.smtp.host", smtpConfig.getHost());
                smtpProps.put("mail.smtp.port", smtpConfig.getTlsPort());

            } else {
                LOG.warn("Email Alerts are disabled. Are you sure you want to configure this?");
            }
        }
    }
}