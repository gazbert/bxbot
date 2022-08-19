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

package com.gazbert.bxbot.trading.api;

/**
 * This exception is thrown by the Exchange Adapter when there is a network error when attempting to connect to the exchange to make an API call.
 * 当尝试连接到 Exchange 以进行 API 调用时出现网络错误时，Exchange 适配器会引发此异常。
 *
 * <p>The non-fatal error response codes and messages specified in the exchange.yaml config file determine whether this exception is thrown by the Exchange Adapter.
 * * <p>exchange.yaml 配置文件中指定的非致命错误响应代码和消息确定 Exchange 适配器是否抛出此异常。
 *
 * <p>If your Trading Strategy catches this exception, you could retry the API call, or exit from
  your Trading Strategy and let the Trading Engine execute your Trading Strategy at the next trade
  cycle. This allows the you to recover from temporary network issues.
 <p>如果您的交易策略捕捉到此异常，您可以重试 API 调用，或退出
 您的交易策略，让交易引擎在下一次交易中执行您的交易策略
 循环。这使您可以从临时网络问题中恢复。
 *
 * <p>If the Trading Engine receives these exceptions from directly calling an Exchange Adapter
  method, it will log the event and sleep until the next trade cycle.
 <p>如果交易引擎通过直接调用交换适配器接收到这些异常
 方法，它将记录事件并休眠，直到下一个交易周期。
 *
 * @author gazbert
 * @since 1.0
 */
public final class ExchangeNetworkException extends Exception {

  private static final long serialVersionUID = 1090595894948829893L;

  /**
   * Constructor builds exception with error message.
   * 构造函数生成带有错误消息的异常。
   *
   * @param msg the error message.
   *            错误信息。
   */
  public ExchangeNetworkException(String msg) {
    super(msg);
  }

  /**
   * Constructor builds exception with error message and original throwable.
   *  构造函数使用错误消息和原始 throwable 构建异常。
   *
   * @param msg the error message.
   *            错误信息。
   *
   * @param e the original exception.
   *          原来的例外。
   */
  public ExchangeNetworkException(String msg, Throwable e) {
    super(msg, e);
  }
}
