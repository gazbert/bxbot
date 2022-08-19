/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.core.util;

import com.gazbert.bxbot.core.mail.EmailAlertMessageBuilder;
import com.gazbert.bxbot.core.mail.EmailAlerter;
import com.gazbert.bxbot.domain.engine.EngineConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 类，该类检查是否已违反紧急停止限制
 * Util class that checks if Emergency Stop Limit has been breached.
 *
 * @author gazbert
 */
public class EmergencyStopChecker {

  private static final Logger LOG = LogManager.getLogger();

  private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot 来自 BX-bot 的 CRITICAL Alert 消息";
  private static final String DECIMAL_FORMAT_PATTERN = "#.########";

  private EmergencyStopChecker() {
  }

  /**
   * //检查紧急停止货币(如美元，比特币)钱包余额是否已经消失
   * Checks if the Emergency Stop Currency (e.g. USD, BTC) wallet balance on exchange has gone
//   * <strong>below</strong> configured limit.  //配置的限制
   *
   * //如果余额无法获得或低于配置限制，则发送一个
   * 发送电子邮件警告并通知主控制回路立即关闭机器人。
   * <p>If the balance cannot be obtained or has dropped below the configured limit, we send an
   * Email Alert and notify the main control loop to immediately shutdown the bot.
   *
   * //这张支票是为了防止失控的损失，因为:
   * <p>This check is here to help protect runaway losses due to:
   *
   * <ul>//buggy”的交易策略。
   *   <li>'buggy' Trading Strategies.
   *   //交易引擎和交易适配器中未预见到的错误
   *   <li>Unforeseen bugs in the Trading Engine and Exchange Adapter.
   *
   *   //交易所发送损坏的订单簿数据和交易策略被误导......
   *      这已经发生了
   *   <li>The exchange sending corrupt order book data and the Trading Strategy being misled...
     this has happened.
   * </ul>
   *
   * //用于连接到交换机的适配器
   * @param exchangeAdapter the adapter used to connect to the exchange.
   *
   *交易引擎配置
   * @param engineConfig the Trading Engine config.
   *
   *                     电子邮件警报器
   * @param emailAlerter the Email Alerter.
   *
   *                     如果超出紧急停止限制，则为 true，否则为 false。
   * @return true if the emergency stop limit has been breached, false otherwise.
   *
   *          TradingApiException 如果连接到交易所发生严重错误。
   * @throws TradingApiException if a serious error has occurred connecting to exchange.
   *
   * ExchangeNetworkException 如果发生临时网络异常
   * @throws ExchangeNetworkException if a temporary network exception has occurred.
   */
  public static boolean isEmergencyStopLimitBreached(
      ExchangeAdapter exchangeAdapter, EngineConfig engineConfig, EmailAlerter emailAlerter)
      throws TradingApiException, ExchangeNetworkException {

    boolean isEmergencyStopLimitBreached = true;

    LOG.info(() -> "Performing Emergency Stop check...执行紧急停止检查...");

    BalanceInfo balanceInfo;
    try {
      balanceInfo = exchangeAdapter.getBalanceInfo();
    } catch (TradingApiException e) {
      final String errorMsg =
          "Failed to get Balance info from exchange to perform Emergency Stop check - letting"
              + " Trade Engine error policy decide what to do next...(无法从交易所获取余额信息以执行紧急停止检查 - 让\n" +
                  "  交易引擎错误策略决定下一步做什么。)";
      LOG.error(() -> errorMsg, e);
      // re-throw to main loop - might only be connection issue and it will retry...  // 重新抛出到主循环 - 可能只是连接问题，它会重试...
      throw e;
    }

    final Map<String, BigDecimal> balancesAvailable = balanceInfo.getBalancesAvailable();
    final BigDecimal currentBalance =
        balancesAvailable.get(engineConfig.getEmergencyStopCurrency());
    if (currentBalance == null) {
      final String errorMsg =
          "Emergency stop check: Failed to get current Emergency Stop Currency balance as 紧急停止检查：无法获取当前紧急停止货币余额为: '"
              + engineConfig.getEmergencyStopCurrency()
              + "' key into Balances map 进入余额MAP"
              + "returned null. Balances returned: 返回空值。退回余额："
              + balancesAvailable;
      LOG.error(() -> errorMsg);
      throw new IllegalStateException(errorMsg);
    } else {

      LOG.info(
          () ->
              "Emergency Stop Currency balance available on exchange is [ 交易所可用的紧急停止货币余额为 ["
                  + new DecimalFormat(DECIMAL_FORMAT_PATTERN).format(currentBalance)
                  + "] "
                  + engineConfig.getEmergencyStopCurrency());

      LOG.info(
          () ->
              "Balance that will stop ALL trading across ALL markets is [ 将停止所有市场的所有交易的余额为 ["
                  + new DecimalFormat(DECIMAL_FORMAT_PATTERN)
                      .format(engineConfig.getEmergencyStopBalance())
                  + "] "
                  + engineConfig.getEmergencyStopCurrency());

      if (currentBalance.compareTo(engineConfig.getEmergencyStopBalance()) < 0) {
        final String balanceBlownErrorMsg =
            "EMERGENCY STOP triggered! - Current Emergency Stop Currency [ 紧急停止触发！ - 当前紧急停止货币 ["
                + engineConfig.getEmergencyStopCurrency()
                + "] wallet 钱包 "
                + "balance 余额 ["
                + new DecimalFormat(DECIMAL_FORMAT_PATTERN).format(currentBalance)
                + "] on exchange 在交换"
                + "is lower than configured Emergency Stop balance [ 低于配置的紧急停止余额 ["
                + new DecimalFormat(DECIMAL_FORMAT_PATTERN)
                    .format(engineConfig.getEmergencyStopBalance())
                + "] "
                + engineConfig.getEmergencyStopCurrency();

        LOG.fatal(() -> balanceBlownErrorMsg);

        emailAlerter.sendMessage(
            CRITICAL_EMAIL_ALERT_SUBJECT,
            EmailAlertMessageBuilder.buildCriticalMsgContent(
                balanceBlownErrorMsg,
                null,
                engineConfig.getBotId(),
                engineConfig.getBotName(),
                exchangeAdapter.getClass().getName()));
      } else {

        isEmergencyStopLimitBreached = false;
        LOG.info(() -> "Emergency Stop check PASSED! 紧急停止检查通过！");
      }
    }
    return isEmergencyStopLimitBreached;
  }
}
