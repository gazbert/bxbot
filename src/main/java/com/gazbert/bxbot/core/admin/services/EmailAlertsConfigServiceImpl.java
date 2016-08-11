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

package com.gazbert.bxbot.core.admin.services;

import com.gazbert.bxbot.core.config.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.core.config.emailalerts.SmtpConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TODO Work in progress...
 *
 * @author gazbert
 * @since 11/08/2016
 */
@Service("emailAlertsConfigService")
@Transactional
public class EmailAlertsConfigServiceImpl implements EmailAlertsConfigService {

    @Override
    public EmailAlertsConfig getConfig() {
        return getCannedEmailAlertsConfig();
    }

    @Override
    public EmailAlertsConfig updateConfig(EmailAlertsConfig config) {
        return getCannedEmailAlertsConfig();
    }

    /*
     * TODO Hard code these for now - will come from Repository later...
     */
    private static EmailAlertsConfig getCannedEmailAlertsConfig() {
        final SmtpConfig smtpConfig = new SmtpConfig(
                "smtp.host.deathstar.com", 573, "boba", "b0unty", "boba.fett@Mandalore.com", "darth.vader@deathstar.com");
        final EmailAlertsConfig emailAlertsConfig = new EmailAlertsConfig();
        emailAlertsConfig.setEnabled(true);
        emailAlertsConfig.setSmtpConfig(smtpConfig);
        return emailAlertsConfig;
    }
}
