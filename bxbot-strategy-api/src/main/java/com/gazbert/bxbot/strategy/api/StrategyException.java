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

package com.gazbert.bxbot.strategy.api;

/**
 * Trading Strategy implementations should throw this exception if they want the Trading Engine to shutdown the bot immediately.
 * 如果交易策略实现希望交易引擎立即关闭机器人，则应抛出此异常。
 *
 * @author gazbert
 * @since 1.0
 */
public final class StrategyException extends Exception {

  private static final long serialVersionUID = -5066890753686004758L;

  /**
   * Constructor builds exception with error message.
   * 构造函数生成带有错误消息的异常。
   *
   * @param msg the error message.
   *            错误信息。
   */
  public StrategyException(String msg) {
    super(msg);
  }

  /**
   * Constructor builds exception from original throwable.
   * 构造函数从原始 throwable 构建异常。
   *
   * @param e the original exception.
   *          原来的例外。
   */
  public StrategyException(Throwable e) {
    super(e);
  }

  /**
   * Constructor builds exception with error message and original throwable.
   *  构造函数使用错误消息和原始 throwable 构建异常。
   *
   * @param msg the error message.
   *            错误信息。
   * @param e the original exception.
   *          原来的例外。
   */
  public StrategyException(String msg, Throwable e) {
    super(msg, e);
  }
}
