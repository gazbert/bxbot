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

package com.gazbert.bxbot.rest.api;

import com.gazbert.bxbot.rest.security.User;
import com.gazbert.bxbot.domain.emailalerts.EmailAlertsConfig;
import com.gazbert.bxbot.services.EmailAlertsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * <p>
 * Controller for directing Email Alerts config requests.
 * <p>
 * Email Alerts config can only be fetched and updated - there is only 1 Email Alerts configuration per bot.
 *
 * @author gazbert
 * @since 1.0
 */
@RestController
@RequestMapping("/api/config")
public class EmailAlertsConfigController {

    private final EmailAlertsConfigService emailAlertsConfigService;

    @Autowired
    public EmailAlertsConfigController(EmailAlertsConfigService emailAlertsConfigService) {
        Assert.notNull(emailAlertsConfigService, "emailAlertsConfigService dependency cannot be null!");
        this.emailAlertsConfigService = emailAlertsConfigService;
    }

    /**
     * Returns Email Alerts configuration for the bot.
     *
     * @return the Email Alerts configuration.
     */
    @RequestMapping(value = "/emailalerts", method = RequestMethod.GET)
    public EmailAlertsConfig getEmailAlerts(@AuthenticationPrincipal User user) {

        final EmailAlertsConfig emailAlertsConfig = emailAlertsConfigService.getConfig();

        // Strip out the Account password for now - too risky to expose.
        emailAlertsConfig.getSmtpConfig().setAccountPassword(null);
        return emailAlertsConfig;
    }

    /**
     * Updates Email Alerts configuration for the bot.
     *
     * @return 204 'No Content' HTTP status code if Email Alerts config was updated, some other HTTP status code otherwise.
     */
    @RequestMapping(value = "/emailalerts", method = RequestMethod.PUT)
    ResponseEntity<?> updateEmailAlerts(@AuthenticationPrincipal User user, @RequestBody EmailAlertsConfig config) {

        emailAlertsConfigService.updateConfig(config);
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(ServletUriComponentsBuilder.fromCurrentRequest().path("/").buildAndExpand().toUri());
        return new ResponseEntity<>(null, httpHeaders, HttpStatus.NO_CONTENT);
    }
}

