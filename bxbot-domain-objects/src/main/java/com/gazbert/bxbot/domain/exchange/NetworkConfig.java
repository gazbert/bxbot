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

package com.gazbert.bxbot.domain.exchange;

import com.google.common.base.MoreObjects;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Min;

/**
 * Domain object representing the Exchange Network config.
 *
 * @author gazbert
 */
@Schema
public class NetworkConfig {

  @Schema(
      description =
          "Optional connection timeout (secs) to wait for exchange response. Must be more than 1s."
              + " Defaults to 30s.")
  @Min(message = "Connection timeout must be more than 1 second", value = 1)
  private Integer connectionTimeout;

  @Schema(
      description =
          "Optional list of HTTP status codes that will trigger the adapter to throw a non-fatal"
              + " ExchangeNetworkException. This allows the bot to recover from temporary network"
              + " issues.")
  private List<Integer> nonFatalErrorCodes;

  @Schema(
      description =
          "An optional list of `java.io` Exception message content that will trigger the adapter"
              + " to throw a non-fatal ExchangeNetworkException. This allows the bot to recover "
              + "from temporary network issues.")
  private List<String> nonFatalErrorMessages;

  public NetworkConfig() {
    nonFatalErrorCodes = new ArrayList<>();
    nonFatalErrorMessages = new ArrayList<>();
  }

  public Integer getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public List<Integer> getNonFatalErrorCodes() {
    return nonFatalErrorCodes;
  }

  public void setNonFatalErrorCodes(List<Integer> nonFatalErrorCodes) {
    this.nonFatalErrorCodes = nonFatalErrorCodes;
  }

  public List<String> getNonFatalErrorMessages() {
    return nonFatalErrorMessages;
  }

  public void setNonFatalErrorMessages(List<String> nonFatalErrorMessages) {
    this.nonFatalErrorMessages = nonFatalErrorMessages;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("connectionTimeout", connectionTimeout)
        .add("nonFatalErrorCodes", nonFatalErrorCodes)
        .add("nonFatalErrorMessages", nonFatalErrorMessages)
        .toString();
  }
}
