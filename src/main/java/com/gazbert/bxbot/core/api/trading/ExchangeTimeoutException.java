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

package com.gazbert.bxbot.core.api.trading;

/**
 * This exception is thrown by an Exchange Adapter when there is a timeout trying to connect to the exchange to make an
 * API call.
 * </p>
 * The timeout limit is implementation specific for each Exchange Adapter; see the documentation for the adapter
 * you are using.
 * </p>
 * If your Trading Strategy catches this exception, you could retry the API call, or exit from your Trading Strategy
 * and let the Trading Engine execute your Trading Strategy at the next trade cycle. This allows the you to recover from
 * temporary network issues.
 * </p>
 * If the Trading Engine receives of these exceptions from directly calling an Exchange Adapter method, it will log the
 * event and sleep until the next trade cycle.
 *
 * @author gazbert
 */
public final class ExchangeTimeoutException extends Exception {

    private static final long serialVersionUID = 1090595894945129893L;

    /**
     * Constructor builds exception with error message.
     *
     * @param msg the error message.
     */
    public ExchangeTimeoutException(String msg) {
        super(msg);
    }

    /**
     * Constructor builds exception with error message and original throwable.
     *
     * @param msg the error message.
     * @param e   the original exception.
     */
    public ExchangeTimeoutException(String msg, Throwable e) {
        super(msg, e);
    }
}
