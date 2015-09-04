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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * Test the Email Alert SMTP Config behaves as expected.
 */
public class TestSmtpConfig {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_TLS_PORT = 587;
    private static final String ACCOUNT_USERNAME = "your.account.username@gmail.com";
    private static final String ACCOUNT_PASSWORD = "your-password";
    private static final String FROM_ADDRESS = "from.address@gmail.com";
    private static final String TO_ADDRESS = "to.address@gmail.com";


    @Test
    public void testSmtpConfigIsInitialisedAsExpected() {

        final SmtpConfig smtpConfig = new SmtpConfig(SMTP_HOST, SMTP_TLS_PORT, ACCOUNT_USERNAME,
                ACCOUNT_PASSWORD, FROM_ADDRESS, TO_ADDRESS);

        assertEquals(SMTP_HOST, smtpConfig.getSmtpHost());
        assertEquals(SMTP_TLS_PORT, smtpConfig.getSmtpTlsPort());
        assertEquals(ACCOUNT_USERNAME, smtpConfig.getAccountUsername());
        assertEquals(ACCOUNT_PASSWORD, smtpConfig.getAccountPassword());
        assertEquals(FROM_ADDRESS, smtpConfig.getFromAddress());
        assertEquals(TO_ADDRESS, smtpConfig.getToAddress());
    }
}
