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

/*
 * SMTP configuration used for sending email alerts.
 */
public final class SmtpConfig {
    /*
     * The SMTP host for sending the email.
     */
    private String smtpHost;

    /*
     * The SMTP TLS port for sending the email.
     */
    private int smtpTlsPort;

    /*
     * The username/email address of the account being used to send the email.
     */
    private String accountUsername;

    /*
     * The password for the account being used to send the email.
     */
    private String accountPassword;

    /*
     * The From address to use when sending the email.
     */
    private String fromAddress;

    /*
     * The To address to use when sending the email.
     */
    private String toAddress;


    public SmtpConfig(String smtpHost, int smtpTlsPort, String accountUsername, String accountPassword,
                      String fromAddress, String toAddress) {
        this.smtpHost = smtpHost;
        this.smtpTlsPort = smtpTlsPort;
        this.accountUsername = accountUsername;
        this.accountPassword = accountPassword;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpTlsPort() {
        return smtpTlsPort;
    }

    public String getAccountUsername() {
        return accountUsername;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }
}
