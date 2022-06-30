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

package com.gazbert.bxbot.trading.api;

/**
 * This exception is thrown from Exchange Adapter implementations when there is a problem making an
  API call to the exchange.
 * 当生成一个问题时，Exchange 适配器实现会抛出此异常
 对交易所的 API 调用。
 *
 * <p>If your Trading Strategy receives this exception, this means something bad as happened; you
  would probably want to wrap this exception in a StrategyException and let the Trading Engine
  shutdown the bot immediately to prevent unexpected losses.
 <p>如果您的交易策略收到此异常，这意味着发生了不好的事情；你
 可能希望将此异常包装在 StrategyException 中并让交易引擎
 立即关闭机器人以防止意外损失。

 *
 * <p>If the Trading Engine receives one of these exceptions from directly calling an Exchange
  Adapter method, it shuts down the bot immediately.
 <p>如果交易引擎从直接调用交易所收到这些异常之一
 适配器方法，它立即关闭机器人。
 *
 * @author gazbert
 * @since 1.0
 */
public class TradingApiException extends Exception {

  private static final long serialVersionUID = -8279304672615688060L;

  /**
   * Constructor builds exception with error message.
   * 构造函数生成带有错误消息的异常。
   *
   * @param msg the error message.
   *            错误信息。
   */
  public TradingApiException(String msg) {
    super(msg);
  }

  /**
   * Constructor builds exception with error message and original throwable.
   * 构造函数使用错误消息和原始 throwable 构建异常。
   *
   * @param msg the error message.
   *            错误信息。
   *
   * @param e the original exception.
   *          原来的例外。
   */
  public TradingApiException(String msg, Throwable e) {
    super(msg, e);
  }
}
