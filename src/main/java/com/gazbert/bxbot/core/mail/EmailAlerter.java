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

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.emailalerts.generated.EmailAlertsType;
import com.gazbert.bxbot.core.config.emailalerts.generated.SmtpConfigType;
import com.gazbert.bxbot.core.util.LogUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/*
 * A simple mail sender using SMTP and TLS.
 * The configuration for sending the email is loaded from the .config/email-alerts.xml config file.
 * It sends plain/text email only.
 */
public final class EmailAlerter {

    private static final Logger LOG = Logger.getLogger(EmailAlerter.class);

    private static volatile EmailAlerter EMAIL_ALERTER_SINGLETON;

    /*
     * Location of the config files (relative to project root).
     */
    private static final String EMAIL_ALERTS_CONFIG_XML_FILENAME = "config/email-alerts.xml";
    private static final String EMAIL_ALERTS_CONFIG_XSD_FILENAME = "config/schemas/email-alerts.xsd";

    /*
     * SMTP config loaded from the bot .config/email-alerts.xml config file. This will be null if no config provided.
     */
    private SmtpConfig smtpConfig;

    /*
     * Properties for configuring the SMTP session.
     */
    private Properties props;

    /*
     * Flag to indicate if Email Alerts are enabled.
     * Defaults to false unless set in config.
     */
    private boolean sendEmailAlertsEnabled;


    private EmailAlerter() {

        initialiseEmailAlerterConfig();

        if (smtpConfig != null) {
            props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpConfig.getSmtpHost());
            props.put("mail.smtp.port", smtpConfig.getSmtpTlsPort());
        }
    }

    public static EmailAlerter getInstance() {

        if (EMAIL_ALERTER_SINGLETON == null) {
            synchronized (EmailAlerter.class) {
                if (EMAIL_ALERTER_SINGLETON == null) {
                    EMAIL_ALERTER_SINGLETON = new EmailAlerter();
                }
            }
        }
        return EMAIL_ALERTER_SINGLETON;
    }

    /*
     * Sends a plain text email message to configured destination.
     */
    public void sendMessage(String subject, String msgContent) {

        if (sendEmailAlertsEnabled) {

            final Session session = Session.getInstance(props, new Authenticator() {
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

                LogUtils.log(LOG, Level.INFO, () -> "About to send following Email Alert with message content: " + msgContent);
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

    /*
     * Initialises the Email Alerter config.
     * Fetches XML config and checks if we need to load the optional email alert SMTP config.
     * Sets if Email Alerts are enabled.
     */
    private void initialiseEmailAlerterConfig() {

        final EmailAlertsType emailAlertsType = loadEmailAlerterConfig();
        if (emailAlertsType != null) {

            final boolean emailAlertsEnabled = emailAlertsType.isEnabled();
            if (emailAlertsEnabled) {

                LogUtils.log(LOG, Level.INFO, () -> "Email Alert for emergency bot shutdown is enabled. Loading SMTP config...");

                final SmtpConfigType smtpConfigType = emailAlertsType.getSmtpConfig();
                if (smtpConfigType == null) {
                    final String errorMsg = "Failed to initialise Email Alerter. " +
                            "Alerts are enabled but no smtp-config has been supplied in email-alerts.xml config file.";
                    throw new IllegalStateException(errorMsg);
                }

                final String smtpHost = smtpConfigType.getSmtpHost();
                LogUtils.log(LOG, Level.INFO, () -> "SMTP host: " + smtpHost);

                final int smtpTlsPort = smtpConfigType.getSmtpTlsPort();
                LogUtils.log(LOG, Level.INFO, () -> "SMTP TLS Port: " + smtpTlsPort);

                final String accountUsername = smtpConfigType.getAccountUsername();
                LogUtils.log(LOG, Level.INFO, () -> "Account username: " + accountUsername);

                final String accountPassword = smtpConfigType.getAccountPassword();
                /* uncomment below for green zone testing only */
//                    LogUtils.log(LOG, Level.INFO, () -> "Account password: " + accountPassword);

                final String fromAddress = smtpConfigType.getFromAddr();
                LogUtils.log(LOG, Level.INFO, () -> "From address: " + fromAddress);

                final String toAddress = smtpConfigType.getToAddr();
                LogUtils.log(LOG, Level.INFO, () -> "To address: " + toAddress);

                smtpConfig = new SmtpConfig(smtpHost, smtpTlsPort, accountUsername, accountPassword, fromAddress, toAddress);
                sendEmailAlertsEnabled = true;
            } else {
                LOG.warn("Email Alerts are disabled. Are you sure you want to configure this?");
            }
        }
    }

    private static EmailAlertsType loadEmailAlerterConfig() {
        return ConfigurationManager.loadConfig(EmailAlertsType.class,
                EMAIL_ALERTS_CONFIG_XML_FILENAME, EMAIL_ALERTS_CONFIG_XSD_FILENAME);
    }
}