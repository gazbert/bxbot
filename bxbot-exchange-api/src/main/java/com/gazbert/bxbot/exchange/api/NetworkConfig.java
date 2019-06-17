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

package com.gazbert.bxbot.exchange.api;

import java.util.List;

/**
 * Encapsulates any (optional) Network configuration for an Exchange Adapter.
 *
 * @author gazbert
 * @since 1.0
 */
public interface NetworkConfig {

  /**
   * Fetches (optional) list of non-fatal error codes.
   *
   * @return a list of non-fatal error codes if present, an empty list otherwise.
   */
  List<Integer> getNonFatalErrorCodes();

  /**
   * Fetches (optional) list of non-fatal error messages.
   *
   * @return list of non-fatal error messages if present, an empty list otherwise.
   */
  List<String> getNonFatalErrorMessages();

  /**
   * Fetches (optional) connection timeout value.
   *
   * @return the connection timeout value if present, null otherwise.
   */
  Integer getConnectionTimeout();
}
