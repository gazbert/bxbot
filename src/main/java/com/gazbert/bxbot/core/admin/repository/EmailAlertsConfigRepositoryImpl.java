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

package com.gazbert.bxbot.core.admin.repository;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.core.config.emailalerts.SmtpConfig;
import com.gazbert.bxbot.core.config.emailalerts.generated.EmailAlertsType;
import com.gazbert.bxbot.core.config.emailalerts.generated.SmtpConfigType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.gazbert.bxbot.core.config.emailalerts.EmailAlertsConfig.EMAIL_ALERTS_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.core.config.emailalerts.EmailAlertsConfig.EMAIL_ALERTS_CONFIG_XSD_FILENAME;

/**
 * Implementation of the Email Alerts configuration repository.
 *
 * @author gazbert
 * @since 11/08/2016
 */
@Repository("emailAlertsConfigRepository")
@Transactional
public class EmailAlertsConfigRepositoryImpl implements EmailAlertsConfigRepository {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public EmailAlertsConfig getConfig() {
        final EmailAlertsType internalEmailAlertsConfig = ConfigurationManager.loadConfig(EmailAlertsType.class,
                EMAIL_ALERTS_CONFIG_XML_FILENAME, EMAIL_ALERTS_CONFIG_XSD_FILENAME);
        return adaptInternalToExternalConfig(internalEmailAlertsConfig);
    }

    @Override
    public void updateConfig(EmailAlertsConfig config) {

        LOG.info(() -> "About to update: " + config);

        final EmailAlertsType internalEmailAlertsConfig = adaptExternalToInternalConfig(config);
        ConfigurationManager.saveConfig(EmailAlertsType.class, internalEmailAlertsConfig, EMAIL_ALERTS_CONFIG_XML_FILENAME);
    }

    // ------------------------------------------------------------------------------------------------
    // Adapter methods
    // ------------------------------------------------------------------------------------------------

    private static EmailAlertsConfig adaptInternalToExternalConfig(EmailAlertsType internalEmailAlertsConfig) {

        final SmtpConfig smtpConfig = new SmtpConfig();
        smtpConfig.setHost(internalEmailAlertsConfig.getSmtpConfig().getSmtpHost());
        smtpConfig.setTlsPort(internalEmailAlertsConfig.getSmtpConfig().getSmtpTlsPort());
        smtpConfig.setToAddress(internalEmailAlertsConfig.getSmtpConfig().getToAddr());
        smtpConfig.setFromAddress(internalEmailAlertsConfig.getSmtpConfig().getFromAddr());
        smtpConfig.setAccountUsername(internalEmailAlertsConfig.getSmtpConfig().getAccountUsername());
        // We don't expose email account password - potential security risk

        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(internalEmailAlertsConfig.isEnabled());
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }

    private static EmailAlertsType adaptExternalToInternalConfig(EmailAlertsConfig externalEmailAlertsConfig) {

        final SmtpConfigType smtpConfig = new SmtpConfigType();
        smtpConfig.setFromAddr(externalEmailAlertsConfig.getSmtpConfig().getFromAddress());
        smtpConfig.setToAddr(externalEmailAlertsConfig.getSmtpConfig().getToAddress());

        // We don't permit updating of: account username, password, host, port - potential security risk
        // We load the existing config and merge it in with the updated stuff...
        final EmailAlertsType existingEmailAlertsConfig = ConfigurationManager.loadConfig(EmailAlertsType.class,
                EMAIL_ALERTS_CONFIG_XML_FILENAME, EMAIL_ALERTS_CONFIG_XSD_FILENAME);
        smtpConfig.setSmtpHost(existingEmailAlertsConfig.getSmtpConfig().getSmtpHost());
        smtpConfig.setSmtpTlsPort(existingEmailAlertsConfig.getSmtpConfig().getSmtpTlsPort());
        smtpConfig.setAccountUsername(existingEmailAlertsConfig.getSmtpConfig().getAccountUsername());
        smtpConfig.setAccountPassword(existingEmailAlertsConfig.getSmtpConfig().getAccountPassword());

        final EmailAlertsType emailAlertsConfig = new EmailAlertsType();
        emailAlertsConfig.setEnabled(externalEmailAlertsConfig.isEnabled());
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }
}
