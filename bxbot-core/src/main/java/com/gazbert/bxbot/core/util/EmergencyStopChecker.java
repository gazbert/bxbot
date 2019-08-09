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
 * Util class that checks if Emergency Stop Limit has been breached.
 *
 * @author gazbert
 */
public class EmergencyStopChecker {

  private static final Logger LOG = LogManager.getLogger();

  private static final String CRITICAL_EMAIL_ALERT_SUBJECT = "CRITICAL Alert message from BX-bot";
  private static final String DECIMAL_FORMAT_PATTERN = "#.########";

  private EmergencyStopChecker() {
  }

  /**
   * Checks if the Emergency Stop Currency (e.g. USD, BTC) wallet balance on exchange has gone
   * <strong>below</strong> configured limit.
   *
   * <p>If the balance cannot be obtained or has dropped below the configured limit, we send an
   * Email Alert and notify the main control loop to immediately shutdown the bot.
   *
   * <p>This check is here to help protect runaway losses due to:
   *
   * <ul>
   *   <li>'buggy' Trading Strategies.
   *   <li>Unforeseen bugs in the Trading Engine and Exchange Adapter.
   *   <li>The exchange sending corrupt order book data and the Trading Strategy being misled...
   *       this has happened.
   * </ul>
   *
   * @param exchangeAdapter the adapter used to connect to the exchange.
   * @param engineConfig the Trading Engine config.
   * @param emailAlerter the Email Alerter.
   * @return true if the emergency stop limit has been breached, false otherwise.
   * @throws TradingApiException if a serious error has occurred connecting to exchange.
   * @throws ExchangeNetworkException if a temporary network exception has occurred.
   */
  public static boolean isEmergencyStopLimitBreached(
      ExchangeAdapter exchangeAdapter, EngineConfig engineConfig, EmailAlerter emailAlerter)
      throws TradingApiException, ExchangeNetworkException {

    boolean isEmergencyStopLimitBreached = true;

    LOG.info(() -> "Performing Emergency Stop check...");

    BalanceInfo balanceInfo;
    try {
      balanceInfo = exchangeAdapter.getBalanceInfo();
    } catch (TradingApiException e) {
      final String errorMsg =
          "Failed to get Balance info from exchange to perform Emergency Stop check - letting"
              + " Trade Engine error policy decide what to do next...";
      LOG.error(() -> errorMsg, e);
      // re-throw to main loop - might only be connection issue and it will retry...
      throw e;
    }

    final Map<String, BigDecimal> balancesAvailable = balanceInfo.getBalancesAvailable();
    final BigDecimal currentBalance =
        balancesAvailable.get(engineConfig.getEmergencyStopCurrency());
    if (currentBalance == null) {
      final String errorMsg =
          "Emergency stop check: Failed to get current Emergency Stop Currency balance as '"
              + engineConfig.getEmergencyStopCurrency()
              + "' key into Balances map "
              + "returned null. Balances returned: "
              + balancesAvailable;
      LOG.error(() -> errorMsg);
      throw new IllegalStateException(errorMsg);
    } else {

      LOG.info(
          () ->
              "Emergency Stop Currency balance available on exchange is ["
                  + new DecimalFormat(DECIMAL_FORMAT_PATTERN).format(currentBalance)
                  + "] "
                  + engineConfig.getEmergencyStopCurrency());

      LOG.info(
          () ->
              "Balance that will stop ALL trading across ALL markets is ["
                  + new DecimalFormat(DECIMAL_FORMAT_PATTERN)
                      .format(engineConfig.getEmergencyStopBalance())
                  + "] "
                  + engineConfig.getEmergencyStopCurrency());

      if (currentBalance.compareTo(engineConfig.getEmergencyStopBalance()) < 0) {
        final String balanceBlownErrorMsg =
            "EMERGENCY STOP triggered! - Current Emergency Stop Currency ["
                + engineConfig.getEmergencyStopCurrency()
                + "] wallet "
                + "balance ["
                + new DecimalFormat(DECIMAL_FORMAT_PATTERN).format(currentBalance)
                + "] on exchange "
                + "is lower than configured Emergency Stop balance ["
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
        LOG.info(() -> "Emergency Stop check PASSED!");
      }
    }
    return isEmergencyStopLimitBreached;
  }
}
