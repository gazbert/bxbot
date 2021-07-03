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

package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * This is a very simple <a
 * href="http://www.investopedia.com/articles/trading/02/081902.asp">scalping strategy</a> to show
 * how to use the Trading API; you will want to code a much better algorithm! It trades using <a
 * href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a> at the <a
 * href="http://www.investopedia.com/terms/s/spotprice.asp">spot price</a>.
 *
 * <p><strong> DISCLAIMER: This algorithm is provided as-is; it might have bugs in it and you could
 * lose money. Use it at our own risk! </strong>
 *
 * <p>It was originally written to trade on <a href="https://btc-e.com">BTC-e</a>, but should work
 * for any exchange. The algorithm will start by buying the base currency (BTC in this example)
 * using the counter currency (USD in this example), and then sell the base currency (BTC) at a
 * higher price to take profit from the spread. The algorithm expects you to have deposited
 * sufficient counter currency (USD) into your exchange wallet in order to buy the base currency
 * (BTC).
 *
 * <p>When it starts up, it places an order at the current BID price and uses x amount of counter
 * currency (USD) to 'buy' the base currency (BTC). The value of x comes from the sample
 * {project-root}/config/strategies.yaml 'counter-currency-buy-order-amount' config-item, currently
 * set to 20 USD. Make sure that the value you use for x is large enough to be able to meet the
 * minimum BTC order size for the exchange you are trading on, e.g. the Bitfinex min order size is
 * 0.01 BTC as of 3 May 2017. The algorithm then waits for the buy order to fill...
 *
 * <p>Once the buy order fills, it then waits until the ASK price is at least y % higher than the
 * previous buy fill price. The value of y comes from the sample
 * {project-root}/config/strategies.yaml 'minimum-percentage-gain' config-item, currently set to 1%.
 * Once the % gain has been achieved, the algorithm will place a sell order at the current ASK
 * price. It then waits for the sell order to fill... and the cycle repeats.
 *
 * <p>The algorithm does not factor in being outbid when placing buy orders, i.e. it does not cancel
 * the current order and place a new order at a higher price; it simply holds until the current BID
 * price falls again. Likewise, the algorithm does not factor in being undercut when placing sell
 * orders; it does not cancel the current order and place a new order at a lower price.
 *
 * <p>Chances are you will either get a stuck buy order if the market is going up, or a stuck sell
 * order if the market goes down. You could manually execute the trades on the exchange and restart
 * the bot to get going again... but a much better solution would be to modify this code to deal
 * with it: cancel your current buy order and place a new order matching the current BID price, or
 * cancel your current sell order and place a new order matching the current ASK price. The {@link
 * TradingApi} allows you to add this behaviour.
 *
 * <p>Remember to include the correct exchange fees (both buy and sell) in your buy/sell
 * calculations when you write your own algorithm. Otherwise, you'll end up bleeding fiat/crypto to
 * the exchange...
 *
 * <p>This demo algorithm relies on the {project-root}/config/strategies.yaml
 * 'minimum-percentage-gain' config-item value being high enough to make a profit and cover the
 * exchange fees. You could tweak the algo to call the {@link
 * com.gazbert.bxbot.trading.api.TradingApi#getPercentageOfBuyOrderTakenForExchangeFee(String)} and
 * {@link
 * com.gazbert.bxbot.trading.api.TradingApi#getPercentageOfSellOrderTakenForExchangeFee(String)}
 * when calculating the order to send to the exchange... See the sample
 * {project-root}/config/samples/{exchange}/exchange.yaml files for info on the different exchange
 * fees.
 *
 * <p>You configure the loading of your strategy using either a className OR a beanName in the
 * {project-root}/config/strategies.yaml config file. This example strategy is configured using the
 * bean-name and by setting the @Component("exampleScalpingStrategy") annotation - this results in
 * Spring injecting the bean - see <a
 * href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Component.html">
 * Spring docs</a> for more details. Alternatively, you can load your strategy using className -
 * this will use the bot's custom injection framework. The choice is yours, but beanName is the way
 * to go if you want to use other Spring features in your strategy, e.g. a <a
 * href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Repository.html">
 * Repository</a> to store your trade data.
 *
 * <p>The algorithm relies on config from the sample {project-root}/config/strategies.yaml and
 * {project-root}/config/markets.yaml files. You can pass additional configItems to your Strategy
 * using the {project-root}/config/strategies.yaml file - you access it from the {@link
 * #init(TradingApi, Market, StrategyConfig)} method via the StrategyConfigImpl argument.
 *
 * <p>This simple demo algorithm only manages 1 order at a time to keep things simple.
 *
 * <p>The Trading Engine will only send 1 thread through your strategy code at a time - you do not
 * have to code for concurrency.
 *
 * <p>This <a
 * href="http://www.investopedia.com/articles/active-trading/101014/basics-algorithmic-trading-concepts-and-examples.asp">
 * site</a> might give you a few ideas - the {@link TradingApi} provides a basic Ticker that you
 * might want to use. Check out the excellent [ta4j](https://github.com/ta4j/ta4j) project too.
 *
 * <p>Good luck!
 *
 * @author gazbert
 */
@Component("exampleScalpingStrategy") // used to load the strategy using Spring bean injection
public class ExampleScalpingStrategy implements TradingStrategy {

  private static final Logger LOG = LogManager.getLogger();

  /** The decimal format for the logs. */
  private static final String DECIMAL_FORMAT = "#.########";

  /** Reference to the main Trading API. */
  private TradingApi tradingApi;

  /** The market this strategy is trading on. */
  private Market market;

  /** The state of the order. */
  private OrderState lastOrder;

  /**
   * The counter currency amount to use when placing the buy order. This was loaded from the
   * strategy entry in the {project-root}/config/strategies.yaml config file.
   */
  private BigDecimal counterCurrencyBuyOrderAmount;

  /**
   * The minimum % gain was to achieve before placing a SELL oder. This was loaded from the strategy
   * entry in the {project-root}/config/strategies.yaml config file.
   */
  private BigDecimal minimumPercentageGain;

  /** These can be set in the strategies.yaml file
   * used latestHigh is optional and default value for maxTradeCycles is 240
   */
  private BigDecimal latestHigh;
  private BigDecimal priceDrop;
  private int maxTradeCycles;


  //keep track of number of cycles since bot has started
  private int countTradeCycles = 0;

  /**
   * Initialises the Trading Strategy. Called once by the Trading Engine when the bot starts up;
   * it's a bit like a servlet init() method.
   * tradingApi the Trading API. Use this to make trades and stuff.
   * market the market for this strategy. This is the market the strategy is currently
   *     running on - you wire this up in the markets.yaml and strategies.yaml files.
   * config configuration for the strategy. Contains any (optional) config you set up in the
   *     strategies.yaml file.
   */


  @Override
  public void init(TradingApi tradingApi, Market market, StrategyConfig config) {
    LOG.info(() -> "Initialising Trading Strategy...");
    this.tradingApi = tradingApi;
    this.market = market;
    getConfigForStrategy(config);
    LOG.info(() -> "Trading Strategy initialised successfully!");
  }

  /**
   * This is the main execution method of the Trading Strategy. It is where your algorithm lives.
   *
   * <p>It is called by the Trading Engine during each trade cycle, e.g. every 60s. The trade cycle
   * is configured in the {project-root}/config/engine.yaml file.
   *
   * @throws StrategyException if something unexpected occurs. This tells the Trading Engine to
   *     shutdown the bot immediately to help prevent unexpected losses.
   */



  @Override
  public void execute() throws StrategyException {
    //Only buy if the current price has dropped 3% from latest high before buying (done)
    //check the price since last time and check if it's lower than latest know highest price
    //if 3% lower that latest highest, then buy, otherwise do nothing and wait for next cycle
    //if 4 hours have gone by and sell has not succeeded, reset strategy

    countTradeCycles++;

    LOG.info(() -> market.getName() + " Checking order status...");

    try {
      // Grab the latest order book for the market.
      final MarketOrderBook orderBook = tradingApi.getMarketOrders(market.getId());

      final List<MarketOrder> buyOrders = orderBook.getBuyOrders();
      if (buyOrders.isEmpty()) {
        LOG.warn(
            () ->
                "Exchange returned empty Buy Orders. Ignoring this trade window. OrderBook: "
                    + orderBook);
        return;
      }

      final List<MarketOrder> sellOrders = orderBook.getSellOrders();
      if (sellOrders.isEmpty()) {
        LOG.warn(
            () ->
                "Exchange returned empty Sell Orders. Ignoring this trade window. OrderBook: "
                    + orderBook);
        return;
      }

      // Get the current BID and ASK spot prices.
      final BigDecimal currentBidPrice = buyOrders.get(0).getPrice();
      final BigDecimal currentAskPrice = sellOrders.get(0).getPrice();

      LOG.info(
          () ->
              market.getName()
                  + " Current BID price="
                  + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice));
      LOG.info(
          () ->
              market.getName()
                  + " Current ASK price="
                  + new DecimalFormat(DECIMAL_FORMAT).format(currentAskPrice));

      // Is this the first time the Strategy has been called? If yes, we initialise the OrderState
      // so we can keep
      // track of orders during later trace cycles.
      if (lastOrder == null) {
        LOG.info(
            () ->
                market.getName()
                    + " First time Strategy has been called - creating new OrderState object.");
        lastOrder = new OrderState();
      }

      // Always handy to log what the last order was during each trace cycle.
      LOG.info(() -> market.getName() + " Last Order was: " + lastOrder);

      //see if we need to update the latestHigh
      if (latestHigh.compareTo(currentBidPrice) < 0) {
        latestHigh = currentBidPrice;
        LOG.info("Latesthigh update. Is now " + latestHigh);

      }


      // Execute the appropriate algorithm based on the last order type.


      if (lastOrder.type == OrderType.BUY) {
        executeAlgoForWhenLastOrderWasBuy();

      } else if (lastOrder.type == OrderType.SELL) {
        executeAlgoForWhenLastOrderWasSell(currentBidPrice, currentAskPrice);

      } else if (lastOrder.type == null) {
        executeAlgoForWhenLastOrderWasNone(currentBidPrice);
      }

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here.
      // We are just going to log it and swallow it, and wait for next trade cycle.
      LOG.error(
          () ->
              market.getName()
                  + " Failed to get market orders because Exchange threw network exception. "
                  + "Waiting until next trade cycle.",
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(
          market.getName()
              + " Failed to get market orders because Exchange threw TradingApi exception. "
              + "Telling Trading Engine to shutdown bot!",
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when the Trading Strategy is invoked for the first time. We start off with a
   * buy order at current BID price.
   * @param currentBidPrice the current market BID price.
   */

  private boolean readyToBuy(BigDecimal currentBidPrice) {

    boolean buy = false;
    LOG.info("3% below LatestHigh = " + latestHigh.multiply(priceDrop).compareTo(currentBidPrice));
    if (latestHigh.multiply(priceDrop).compareTo(currentBidPrice) > 0) {
      buy = true;
      //latestHigh set to current bid price
      latestHigh = currentBidPrice;
      LOG.info(" Ready to buy");
    } else {
      LOG.info(" not ready at currentPrice" + currentBidPrice + " latestHigh " + latestHigh);
    }
    return buy;
  }

  private void executeAlgoForWhenLastOrderWasNone(BigDecimal currentBidPrice)
      throws StrategyException {

    if (readyToBuy(currentBidPrice)) {
      LOG.info(
          () ->
              market.getName()
                  + " OrderType is NONE - placing new BUY order at ["
                  + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice)
                  + "]");

      try {
        // Calculate the amount of base currency (BTC) to buy for given amount of counter currency
        // (USD).
        BigDecimal ccBuy = counterCurrencyBuyOrderAmount;
        final BigDecimal amountOfBaseCurrencyToBuy =
                getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(ccBuy);

        // Send the order to the exchange
        LOG.info(() -> market.getName()
                + " Sending initial BUY order to exchange --->");

        lastOrder.id =
            tradingApi.createOrder(
                market.getId(), OrderType.BUY, amountOfBaseCurrencyToBuy, currentBidPrice);

        LOG.info(
            () -> market.getName()
                    + " Initial BUY Order sent successfully. ID: " + lastOrder.id);

        // update last order details
        lastOrder.price = currentBidPrice;
        lastOrder.type = OrderType.BUY;
        lastOrder.amount = amountOfBaseCurrencyToBuy;

      } catch (ExchangeNetworkException e) {
        // Your timeout handling code could go here, e.g. you might want to check if the order
        // actually made it to the exchange? And if not, resend it...
        // We are just going to log it and swallow it, and wait for next trade cycle.
        LOG.error(
            () ->
                market.getName()
                    + " Initial order to BUY base currency failed because Exchange threw network "
                    + "exception. Waiting until next trade cycle.",
            e);

      } catch (TradingApiException e) {
        // Your error handling code could go here...
        // We are just going to re-throw as StrategyException for engine to deal with - it will
        // shutdown the bot.
        LOG.error(
            () ->
                market.getName()
                    + " Initial order to BUY base currency failed bc Exchange threw TradingApi "
                    + "exception. Telling Trading Engine to shutdown bot!",
            e);
        throw new StrategyException(e);
      }
    } else {
      LOG.info(" not ready currentPrice" + currentBidPrice + " latestHigh " + latestHigh);
    }
  }

  /**
   * Algo for executing when last order we placed on the exchanges was a BUY.
   *
   * <p>If last buy order filled, we try and sell at a profit.
   *
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeAlgoForWhenLastOrderWasBuy() throws StrategyException {
    try {
      // Fetch our current open orders and see if the buy order is still outstanding/open on the
      // exchange
      final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
      boolean lastOrderFound = false;
      for (final OpenOrder myOrder : myOrders) {
        if (myOrder.getId().equals(lastOrder.id)) {
          lastOrderFound = true;
          break;
        }
      }

      // If the order is not there, it must have all filled.
      // also check if we're ready to buy again
      if (!lastOrderFound) {
        LOG.info(
            () ->
                market.getName()
                    + " ^^^ Yay!!! Last BUY Order Id ["
                    + lastOrder.id
                    + "] filled at ["
                    + lastOrder.price
                    + "]");

        /*
         * The last buy order was filled, so lets see if we can send a new sell order.
         *
         * IMPORTANT - new sell order ASK price must be > (last order price + exchange fees)
         *             because:
         *
         * 1. If we put sell amount in as same amount as previous buy, the exchange barfs because
         *    we don't have enough units to cover the transaction fee.
         * 2. We could end up selling at a loss.
         *
         * For this example strategy, we're just going to add 2% (taken from the
         * 'minimum-percentage-gain' config item in the {project-root}/config/strategies.yaml
         * config file) on top of previous bid price to make a little profit and cover the exchange
         * fees.
         *
         * Your algo will have other ideas on how much profit to make and when to apply the
         * exchange fees - you could try calling the
         * TradingApi#getPercentageOfBuyOrderTakenForExchangeFee() and
         * TradingApi#getPercentageOfSellOrderTakenForExchangeFee() when calculating the order to
         * send to the exchange...
         */
        LOG.info(
            () ->
                market.getName()
                    + " Percentage profit (in decimal) to make for the sell order is: "
                    + minimumPercentageGain);

        final BigDecimal amountToAdd = lastOrder.price.multiply(minimumPercentageGain);
        LOG.info(
            () -> market.getName() + " Amount to add to last buy order fill price: " + amountToAdd);

        // Most exchanges (if not all) use 8 decimal places.
        // It's usually best to round up the ASK price in your calculations to maximise gains.
        final BigDecimal newAskPrice =
            lastOrder.price.add(amountToAdd).setScale(8, RoundingMode.HALF_UP);
        LOG.info(
            () ->
                market.getName()
                    + " Placing new SELL order at ask price ["
                    + new DecimalFormat(DECIMAL_FORMAT).format(newAskPrice)
                    + "]");

        LOG.info(() -> market.getName() + " Sending new SELL order to exchange --->");

        // Build the new sell order
        lastOrder.id =
            tradingApi.createOrder(market.getId(), OrderType.SELL, lastOrder.amount, newAskPrice);
        LOG.info(() -> market.getName() + " New SELL Order sent successfully. ID: " + lastOrder.id);

        // update last order state
        lastOrder.price = newAskPrice;
        lastOrder.type = OrderType.SELL;
      } else {

        /*
         * BUY order has not filled yet.
         * Could be nobody has jumped on it yet... or the order is only part filled... or market
         * has gone up and we've been outbid and have a stuck buy order. In which case, we have to
         * wait for the market to fall for the order to fill... or you could tweak this code to
         * cancel the current order and raise your bid - remember to deal with any part-filled
         * orders!
         */
        LOG.info(
            () ->
                market.getName()
                    + " !!! Still have BUY Order "
                    + lastOrder.id
                    + " waiting to fill at ["
                    + lastOrder.price
                    + "] - holding last BUY order...");
      }

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order
      // actually
      // made it to the exchange? And if not, resend it...
      // We are just going to log it and swallow it, and wait for next trade cycle.
      LOG.error(
          () ->
              market.getName()
                  + " New Order to SELL base currency failed because Exchange threw network "
                  + "exception. Waiting until next trade cycle. Last Order: "
                  + lastOrder,
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(
          () ->
              market.getName()
                  + " New order to SELL base currency failed because Exchange threw TradingApi "
                  + "exception. Telling Trading Engine to shutdown bot! Last Order: "
                  + lastOrder,
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when last order we placed on the exchange was a SELL.
   *
   * <p>If last sell order filled, we send a new buy order to the exchange.
   *
   * @param currentBidPrice the current market BID price.
   * @param currentAskPrice the current market ASK price.
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeAlgoForWhenLastOrderWasSell(
      BigDecimal currentBidPrice, BigDecimal currentAskPrice) throws StrategyException {


    try {
      final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
      boolean lastOrderFound = false;
      for (final OpenOrder myOrder : myOrders) {
        if (myOrder.getId().equals(lastOrder.id)) {
          lastOrderFound = true;
          break;
        }
      }

      // If the order is not there, it must have all filled and we place a new buy
      // Also if we've been waiting >240 minutes since last sell order was placed,
      // just place new buy
      if (countTradeCycles > maxTradeCycles && lastOrderFound) {
        LOG.info(
            () ->
              "Waited for 240 minutes");
      }
      if (!lastOrderFound) {
        LOG.info(
            () ->
              market.getName()
                      + " ^^^ Yay!!! Last SELL Order Id ["
                      + lastOrder.id
                      + "] filled at ["
                      + lastOrder.price
                      + "]");
      }

      if (!lastOrderFound || countTradeCycles > maxTradeCycles) {
        //now check if we're ready to place a new buy order
        if (readyToBuy(currentAskPrice)) {
          // Get amount of base currency (BTC) we can buy for given counter currency (USD) amount.
          final BigDecimal amountOfBaseCurrencyToBuy =
                  getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
                          counterCurrencyBuyOrderAmount);

          LOG.info(
              () ->
              market.getName()
                      + " Placing new BUY order at bid price ["
                      + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice)
                      + "]");

          LOG.info(() -> market.getName() + " Sending new BUY order to exchange --->");

          // Send the buy order to the exchange.
          lastOrder.id =
                  tradingApi.createOrder(
                        market.getId(), OrderType.BUY, amountOfBaseCurrencyToBuy, currentBidPrice);
          LOG.info(
              () -> market.getName() + " New BUY Order sent successfully. ID: " + lastOrder.id);

          // update last order details
          lastOrder.price = currentBidPrice;
          lastOrder.type = OrderType.BUY;
          lastOrder.amount = amountOfBaseCurrencyToBuy;

          //reset countTradeCycles to start counting again
          countTradeCycles = 0;
        }
      } else {

        /*
         * SELL order not filled yet.
         * Could be nobody has jumped on it yet... or the order is only part filled... or market
         * has gone down and we've been undercut and have a stuck sell order. In which case, we
         * have to wait for market to recover for the order to fill... or you could tweak this
         * code to cancel the current order and lower your ask - remember to deal with any
         * part-filled orders!
         */
        if (currentAskPrice.compareTo(lastOrder.price) < 0) {
          LOG.info(
              () ->
                  market.getName()
                      + " <<< Current ask price ["
                      + currentAskPrice
                      + "] is LOWER then last order price ["
                      + lastOrder.price
                      + "] - holding last SELL order...");

        } else if (currentAskPrice.compareTo(lastOrder.price) > 0) {
          LOG.error(
              () ->
                  market.getName()
                      + " >>> Current ask price ["
                      + currentAskPrice
                      + "] is HIGHER than last order price ["
                      + lastOrder.price
                      + "] - IMPOSSIBLE! BX-bot must have sold?????");

        } else if (currentAskPrice.compareTo(lastOrder.price) == 0) {
          LOG.info(
              () ->
                  market.getName()
                      + " === Current ask price ["
                      + currentAskPrice
                      + "] is EQUAL to last order price ["
                      + lastOrder.price
                      + "] - holding last SELL order...");
        }
      }
    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order
      // actually made it to the exchange? And if not, resend it...
      // We are just going to log it and swallow it, and wait for next trade cycle.
      LOG.error(
          () ->
              market.getName()
                  + " New Order to BUY base currency failed because Exchange threw network "
                  + "exception. Waiting until next trade cycle. Last Order: "
                  + lastOrder,
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(
          () ->
              market.getName()
                  + " New order to BUY base currency failed because Exchange threw TradingApi "
                  + "exception. Telling Trading Engine to shutdown bot! Last Order: "
                  + lastOrder,
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Returns amount of base currency (BTC) to buy for a given amount of counter currency (USD) based
   * on last market trade price.
   *
   * @param amountOfCounterCurrencyToTrade the amount of counter currency (USD) we have to trade
   *     (buy) with.
   * @return the amount of base currency (BTC) we can buy for the given counter currency (USD)
   *     amount.
   * @throws TradingApiException if an unexpected error occurred contacting the exchange.
   * @throws ExchangeNetworkException if a request to the exchange has timed out.
   */
  private BigDecimal getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
      BigDecimal amountOfCounterCurrencyToTrade)
      throws TradingApiException, ExchangeNetworkException {

    LOG.info(
        () ->
            market.getName()
                + " Calculating amount of base currency (BTC) to buy for amount of counter "
                + "currency "
                + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                + " "
                + market.getCounterCurrency());

    // Fetch the last trade price
    final BigDecimal lastTradePriceInUsdForOneBtc = tradingApi.getLatestMarketPrice(market.getId());
    LOG.info(
        () ->
            market.getName()
                + " Last trade price for 1 "
                + market.getBaseCurrency()
                + " was: "
                + new DecimalFormat(DECIMAL_FORMAT).format(lastTradePriceInUsdForOneBtc)
                + " "
                + market.getCounterCurrency());

    /*
     * Most exchanges (if not all) use 8 decimal places and typically round in favour of the
     * exchange. It's usually safest to round down the order quantity in your calculations.
     */
    final BigDecimal amountOfBaseCurrencyToBuy =
        amountOfCounterCurrencyToTrade.divide(
            lastTradePriceInUsdForOneBtc, 8, RoundingMode.HALF_DOWN);

    LOG.info(
        () ->
            market.getName()
                + " Amount of base currency ("
                + market.getBaseCurrency()
                + ") to BUY for "
                + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                + " "
                + market.getCounterCurrency()
                + " based on last market trade price: "
                + amountOfBaseCurrencyToBuy);

    return amountOfBaseCurrencyToBuy;
  }

  /**
   * Loads the config for the strategy. We expect the 'counter-currency-buy-order-amount' and
   * 'minimum-percentage-gain' config items to be present in the
   * {project-root}/config/strategies.yaml config file.
   *
   * @param config the config for the Trading Strategy.
   */
  private void getConfigForStrategy(StrategyConfig config) {

    // Get counter currency buy amount...
    final String counterCurrencyBuyOrderAmountFromConfigAsString =
        config.getConfigItem("counter-currency-buy-order-amount");

    if (counterCurrencyBuyOrderAmountFromConfigAsString == null) {
      // game over
      throw new IllegalArgumentException(
          "Mandatory counter-currency-buy-order-amount value missing in strategy.xml config.");
    }
    LOG.info(
        () ->
            "<counter-currency-buy-order-amount> from config is: "
                + counterCurrencyBuyOrderAmountFromConfigAsString);

    // Will fail fast if value is not a number
    counterCurrencyBuyOrderAmount = new BigDecimal(counterCurrencyBuyOrderAmountFromConfigAsString);
    LOG.info(() -> "counterCurrencyBuyOrderAmount: " + counterCurrencyBuyOrderAmount);

    // Get min % gain...
    final String minimumPercentageGainFromConfigAsString =
        config.getConfigItem("minimum-percentage-gain");
    if (minimumPercentageGainFromConfigAsString == null) {
      // game over
      throw new IllegalArgumentException(
          "Mandatory minimum-percentage-gain value missing in strategy.xml config.");
    }
    LOG.info(
        () ->
            "<minimum-percentage-gain> from config is: " + minimumPercentageGainFromConfigAsString);

    // Will fail fast if value is not a number
    final BigDecimal minimumPercentageGainFromConfig =
        new BigDecimal(minimumPercentageGainFromConfigAsString);
    minimumPercentageGain =
        minimumPercentageGainFromConfig.divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);

    LOG.info(() -> "minimumPercentageGain in decimal is: " + minimumPercentageGain);


    //getpricedrop
    final String priceDropFromConfigAsString =
            config.getConfigItem("price-drop");
    if (priceDropFromConfigAsString == null) {
      // game over
      throw new IllegalArgumentException(
              "Mandatory <price-drop> value missing in strategy.xml config.");
    }
    LOG.info("<price-drop> from config is: " + priceDropFromConfigAsString);

    // Will fail fast if value is not a number
    final BigDecimal priceDropFromConfig =
            new BigDecimal(priceDropFromConfigAsString);
    priceDrop =
            priceDropFromConfig.divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);

    LOG.info("<price-drop> in decimal is: " + priceDrop);

    //getlatesthigh
    final String latestHighFromConfigAsString =
            config.getConfigItem("latest-high");
    if (latestHighFromConfigAsString == null) {
      // set to zero
      latestHigh = BigDecimal.valueOf(0);
      LOG.info("No config setting for <latest-high> setting to " + latestHigh);
    } else {
      LOG.info("<latest-high> from config is: " + latestHighFromConfigAsString);

      // Will fail fast if value is not a number
      latestHigh =
              new BigDecimal(latestHighFromConfigAsString);

      LOG.info(() -> "<latest-high> in decimal is: " + minimumPercentageGain);
    }
    // getTradecycles to wait before restarting strategy
    final String maxTradeCyclesFromConfigAsString =
            config.getConfigItem("max-trade-cycles");
    if (maxTradeCyclesFromConfigAsString == null) {
      // default to 240 cycles (minutes)
      maxTradeCycles = 240;
      LOG.info("<max-trade-cycles> default setting: " + maxTradeCycles);
    } else {


      // Set the integer value to config setting
      maxTradeCycles = Integer.parseInt(maxTradeCyclesFromConfigAsString);
      LOG.info("<max-trade-cycles> from config is: " + maxTradeCycles);
    }

  }

  /**
   * Models the state of an Order placed on the exchange.
   *
   * <p>Typically, you would maintain order state in a database or use some other persistence method
   * to recover from restarts and for audit purposes. In this example, we are storing the state in
   * memory to keep it simple.
   */
  private static class OrderState {

    /** Id - default to null. */
    private String id = null;

    /**
     * Type: buy/sell. We default to null which means no order has been placed yet, i.e. we've just
     * started!
     */
    private OrderType type = null;

    /** Price to buy/sell at - default to zero. */
    private BigDecimal price = BigDecimal.ZERO;

    /** Number of units to buy/sell - default to zero. */
    private BigDecimal amount = BigDecimal.ZERO;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("id", id)
          .add("type", type)
          .add("price", price)
          .add("amount", amount)
          .toString();
    }
  }
}
