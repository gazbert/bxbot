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

package com.gazbert.bxbot.core.strategies;

import com.gazbert.bxbot.core.api.trading.*;
import com.gazbert.bxbot.core.api.strategy.StrategyConfig;
import com.gazbert.bxbot.core.api.strategy.StrategyException;
import com.gazbert.bxbot.core.api.strategy.TradingStrategy;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

/**
 * <strong>
 * DISCLAIMER:
 * This algorithm is provided as-is; it might have bugs in it and you could lose money. Use it at our own risk!
 * </strong>
 *
 * <p>
 * This is a very simple <a href="http://www.investopedia.com/articles/trading/02/081902.asp">scalping strategy</a>
 * to show how to use the Trading API; you will want to code a much better algorithm.
 * It trades using <a href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a> at the
 * <a href="http://www.investopedia.com/terms/s/spotprice.asp">spot price</a>.
 * </p>
 *
 * <p>
 * It has been written specifically to trade altcoins on <a href="https://www.cryptsy.com/">Cryptsy</a>, but can be
 * adapted to trade on any trading. It assumes a long position in BTC and short positions in various altcoins. In other
 * words, it initiates trades to buy altcoins using BTC and then sells those altcoins at a profit to receive additional BTC.
 * The algorithm expects you to have deposited sufficient BTC into your trading wallet.
 * </p>
 *
 * <p>
 * Altcoin exchanges like <a href="https://www.cryptsy.com/">Cryptsy</a> are a good place to start trading Bitcoin,
 * because you can typically trade much smaller amounts (and risk losing it!) when testing your new algorithms. I usually
 * test my algorithms out on altcoin markets before running them on USD/BTC markets on exchanges like
 * <a href="https://btc-e.com">BTC-e</a> and <a href="https://www.bitstamp.net/">Bitstamp</a>.
 * </p>
 *
 * <p>
 * In a nutshell, the algorithm places an order at the current BID price, holds until the current ASK price (+ trading fees) is
 * higher than the price the order filled at, and then places a sell order at the current ASK price. Assuming the sell
 * order fills, we take the profit from the spread. The process then repeats.
 * </p>
 *
 * <p>
 * The algorithm does not factor in being outbid when placing buy orders, i.e. it does not cancel its current order
 * and place a new order at a higher price; it simply holds until the current BID price falls down again. Likewise, the
 * algorithm does not factor in being undercut when placing sell orders, i.e. it does not cancel its current order
 * and place a new order at a lower price; it simply holds until the current BID price falls down again.
 * </p>
 *
 * <p>
 * Chances are you will either get a stuck buy order if the market is going up, or a stuck sell order if the market is
 * going down. You could manually execute the trades on the exchanges and restart the bot to get going again... but a
 * much better solution would be to modify this algorithm to deal with it, i.e. cancelling your current buy order and
 * place a new order matching the best BID price, or cancelling your current sell order and placing a new order matching
 * the best ASK price. The {@link TradingApi} allows you to add this behaviour, but I've tried to keep things as simple
 * as possible in this example.
 * </p>
 *
 * <p>
 * Remember to include the correct exchanges fees (both buy and sell) in your calculations else you'll end up bleeding
 * money/coins to the trading.
 * </p>
 *
 * <p>
 * The algorithm only manages 1 order at a time to keep the example simple.
 * </p>
 *
 * <p>
 * You can pass configuration to your Strategy from the ./config/strategies.xml file - you access it from the
 * {@link #init(TradingApi, Market, StrategyConfig)} method via the StrategyConfig argument.
 * </p>
 *
 * <p>
 * The Trading Engine will only send 1 thread through your strategy code at a time, so you do not have to code for
 * concurrency.
 * </p>
 *
 * <p>
 * This <a href="http://www.investopedia.com/articles/active-trading/101014/basics-algorithmic-trading-concepts-and-examples.asp">
 * site</a> might give you a few ideas.
 * </p>
 *
 * <p>
 * Good luck!
 * <p/>
 *
 * @author gazbert
 */
public class ExampleScalpingStrategy implements TradingStrategy {

    private static final Logger LOG = Logger.getLogger(ExampleScalpingStrategy.class);

    /**
     * Reference to the main Trading API.
     */
    private TradingApi tradingApi;

    /**
     * The market this strategy is trading on.
     */
    private Market market;

    /**
     * The state of the order.
     */
    private OrderState lastOrder;

    /**
     * BTC buy order amount. This was loaded from the strategy entry in the ./config/strategies.xml config file.
     */
    private BigDecimal btcBuyOrderAmount;


    /**
     * Initialises the Trading Strategy.
     * Called once by the Trading Engine when the bot starts up; a bit like a servlet init() method.
     *
     * @param tradingApi the Trading API. Use this to make trades and stuff.
     * @param market     the market for this strategy. This is the market the strategy is currently running on - you wire
     *                   this up in the markets.xml and strategies.xml files.
     * @param config     optional configuration for the strategy. Contains any (optional) config you setup in the
     *                   strategies.xml file.
     */
    @Override
    public void init(TradingApi tradingApi, Market market, StrategyConfig config) {

        if (LOG.isInfoEnabled()) {
            LOG.info("Initialising Trading Strategy...");
        }

        this.tradingApi = tradingApi;
        this.market = market;
        getConfigForStrategy(config);

        if (LOG.isInfoEnabled()) {
            LOG.info("Trading Strategy initialised successfully!");
        }
    }

    /**
     * <p>
     * This is the main execution method of the Trading Strategy. It is where your algorithm lives.
     * </p>
     *
     * <p>
     * It is called by the Trading Engine during each trade cycle, e.g. every 60s. The trace cycle is configured in
     * the .config/engine.xml file.
     * </p>
     *
     * @throws StrategyException if something unexpected occurs. This tells the Trading Engine to shutdown the bot
     *                           immediately to help prevent unexpected losses.
     */
    @Override
    public void execute() throws StrategyException {

        if (LOG.isInfoEnabled()) {
            LOG.info(market.getName() + " Checking order status...");
        }

        try {
            // Grab the latest order book for the market.
            final MarketOrderBook orderBook = tradingApi.getMarketOrders(market.getId());

            // Cryptsy trading used to return nothing sometimes! So we need to be defensive here to protect from NPEs
            final List<MarketOrder> buyOrders = orderBook.getBuyOrders();
            if (buyOrders.size() == 0) {
                LOG.warn("Exchange returned empty Buy Orders. Ignoring this trade window. OrderBook: " + orderBook);
                return;
            }

            // Cryptsy trading used to return nothing sometimes! So we need to be defensive here to protect from NPEs
            final List<MarketOrder> sellOrders = orderBook.getSellOrders();
            if (sellOrders.size() == 0) {
                LOG.warn("Exchange returned empty Sell Orders. Ignoring this trade window. OrderBook: " + orderBook);
                return;
            }

            // Get the current BID and ASK spot prices.
            final BigDecimal currentBidPrice = buyOrders.get(0).getPrice();
            final BigDecimal currentAskPrice = sellOrders.get(0).getPrice();

            if (LOG.isInfoEnabled()) {
                LOG.info(market.getName() + " Current BID price=" + new DecimalFormat("#.########").format(currentBidPrice));
                LOG.info(market.getName() + " Current ASK price=" + new DecimalFormat("#.########").format(currentAskPrice));
            }

            /*
             * Is this the first time the Strategy has been called? If yes, we initialise the OrderState so we can keep
             * track of orders during later trace cycles.
             */
            if (lastOrder == null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " First time Strategy has been called - creating new OrderState object.");
                }
                lastOrder = new OrderState();
            }

            // Always handy to log what the last order was during each trace cycle.
            if (LOG.isInfoEnabled()) {
                LOG.info(market.getName() + " Last Order was: " + lastOrder);
            }

            /*
             * Execute the appropriate algorithm based on the last order type.
             */
            if (lastOrder.type == OrderType.BUY) {
                executeAlgoForWhenLastOrderWasBuy();

            } else if (lastOrder.type == OrderType.SELL) {
                executeAlgoForWhenLastOrderWasSell(currentBidPrice, currentAskPrice);

            } else if (lastOrder.type == null) {
                executeAlgoForWhenLastOrderWasNone(currentBidPrice);
            }

        } catch (ExchangeTimeoutException e) {

            // Your timeout handling code could got here.
            // We are just going to log it and swallow it, and wait for next trade cycle.
            LOG.error(market.getName() + " Failed to get market orders because Exchange threw timeout exception. " +
                    "Waiting until next trade cycle.", e);

        } catch (TradingApiException e) {

            // Your error handling code could go here...
            // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
            LOG.error(market.getName() + " Failed to get market orders because Exchange threw TradingApi exception. " +
                    " Telling Trading Engine to shutdown bot!", e);
            throw new StrategyException(e);
        }
    }

    /**
     * Algo for executing when last order we none. This is called when the Trading Strategy is invoked for the first time.
     * We start off with a buy order at current BID price.
     *
     * @param currentBidPrice the current market BID price.
     * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
     *                           Throwing this exception indicates we want the Trading Engine to shutdown the bot.
     */
    private void executeAlgoForWhenLastOrderWasNone(BigDecimal currentBidPrice) throws StrategyException {

        if (LOG.isInfoEnabled()) {
            LOG.info(market.getName() + " OrderType is NONE - placing new BUY order at ["
                    + new DecimalFormat("#.########").format(currentBidPrice) + "]");
        }

        try {

            // Calculate the amount of altcoin to buy for given amount of BTC.
            BigDecimal amountOfAltcoinToBuyForGivenBtc = getAmountOfAltcoinToBuyForGivenBtcAmount(btcBuyOrderAmount);

            if (LOG.isInfoEnabled()) {
                LOG.info(market.getName() + " Sending initial BUY order to trading --->");
            }

            // Send the order to the trading
            lastOrder.id = tradingApi.createOrder(market.getId(), OrderType.BUY, amountOfAltcoinToBuyForGivenBtc, currentBidPrice);

            if (LOG.isInfoEnabled()) {
                LOG.info(market.getName() + " Initial BUY Order sent successfully. ID: " + lastOrder.id);
            }

            // update last order details
            lastOrder.price = currentBidPrice;
            lastOrder.type = OrderType.BUY;
            lastOrder.amount = amountOfAltcoinToBuyForGivenBtc;

        } catch (ExchangeTimeoutException e) {

            // Your timeout handling code could got here, e.g. you might want to check if the order actually
            // made it to the trading? And if not, resend it...
            // We are just going to log it and swallow it, and wait for next trade cycle.
            LOG.error(market.getName() + " Initial order to BUY altcoin failed because Exchange threw timeout exception. " +
                    "Waiting until next trade cycle.", e);

        } catch (TradingApiException e) {

            // Your error handling code could go here...
            // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
            LOG.error(market.getName() + " Initial order to BUY altcoin failed because Exchange threw TradingApi exception. " +
                    " Telling Trading Engine to shutdown bot!", e);
            throw new StrategyException(e);
        }
    }

    /**
     * <p>
     * Algo for executing when last order we placed on the exchanges was a BUY.
     * </p>
     *
     * <p>
     * If last buy order filled, we try and sell at a profit.
     * </p>
     *
     * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
     *                           Throwing this exception indicates we want the Trading Engine to shutdown the bot.
     */
    private void executeAlgoForWhenLastOrderWasBuy() throws StrategyException {

        try {

            // Fetch our current open orders and see if the buy order is still outstanding on the trading
            final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
            boolean lastOrderFound = false;
            for (final OpenOrder myOrder : myOrders) {
                if (myOrder.getId().equals(lastOrder.id)) {
                    lastOrderFound = true;
                    break;
                }
            }

            // if the order is not there, it must have all filled.
            if (!lastOrderFound) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " ^^^ Yay!!! Last BUY Order Id [" + lastOrder.id + "] filled at [" + lastOrder.price + "]");
                }

                /*
                 * The last buy order was filled, so lets see if we can send a new sell order...
                 *
                 * IMPORTANT - new sell order ASK price must be > (last order price + trading fees) because:
                 *
                 * 1. if we put sell amount in at same amount as previous buy and trading barfs because we don't have
                 *    enough to cover transaction fee! Results in stuck SELL order.
                 * 2. we could be selling at a loss!
                 *
                 * For this example code, we are just going to add 1% on top of original bid price (last order)
                 * and also cover the exchanges fees. Your algo will have other ideas how much profit to make ;-)
                 */
                final BigDecimal percentProfitToMake = new BigDecimal("0.01");
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " Percentage profit to make on sell order is: " + percentProfitToMake);
                }

                final BigDecimal buyOrderPercentageFee = tradingApi.getPercentageOfBuyOrderTakenForExchangeFee(market.getId());
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " Exchange fee in percent for buy order is: " + buyOrderPercentageFee);
                }

                final BigDecimal sellOrderPercentageFee = tradingApi.getPercentageOfSellOrderTakenForExchangeFee(market.getId());
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " Exchange fee in percent for sell order is: " + sellOrderPercentageFee);
                }

                final BigDecimal totalPercentageIncrease = percentProfitToMake.add(buyOrderPercentageFee).add(sellOrderPercentageFee);
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " Total percentage increase for new sell order is: " + totalPercentageIncrease);
                }

                final BigDecimal amountToAdd = lastOrder.price.multiply(totalPercentageIncrease);
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " Amount to add last order price: " + amountToAdd);
                }

                /*
                 * Most exchanges (if not all) use 8 decimal places.
                 * It's usually bes t to ROUND UP the ASK price in your calculations to maximise gains.
                 */
                final BigDecimal newAskPrice = lastOrder.price.add(amountToAdd).setScale(8, RoundingMode.HALF_UP);
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " Placing new SELL order at ask price [" +
                            new DecimalFormat("#.########").format(newAskPrice) + "]");
                    LOG.info(market.getName() + " Sending new SELL order to trading --->");
                }


                // Build the new sell order
                lastOrder.id = tradingApi.createOrder(market.getId(), OrderType.SELL, lastOrder.amount, newAskPrice);

                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " New SELL Order sent successfully. ID: " + lastOrder.id);
                }

                // update last order state
                lastOrder.price = newAskPrice;
                lastOrder.type = OrderType.SELL;

            } else {

                /*
                 * BUY order not filled yet.
                 * Could be nobody has jumped on it yet... the order is only part filled... or market has gone up and
                 * we've been outbid and have a stuck buy order, in which case we have to wait for market to drop...
                 * unless you tweak this code to cancel the current order and raise your bid; remember to deal with any
                 * part-filled orders!
                 */
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " !!! Still have BUY Order " + lastOrder.id
                            + " waiting to fill at [" + lastOrder.price + "] - holding last BUY order...");
                }
            }

        } catch (ExchangeTimeoutException e) {

            // Your timeout handling code could got here, e.g. you might want to check if the order actually
            // made it to the trading? And if not, resend it...
            // We are just going to log it and swallow it, and wait for next trade cycle.
            LOG.error(market.getName() + " New Order to SELL altcoin failed because Exchange threw timeout exception. " +
                    "Waiting until next trade cycle. Last Order: " + lastOrder, e);

        } catch (TradingApiException e) {

            // Your error handling code could go here...
            // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
            LOG.error(market.getName() + " New order to SELL altcoin failed because Exchange threw TradingApi exception. " +
                    " Telling Trading Engine to shutdown bot! Last Order: " + lastOrder, e);
            throw new StrategyException(e);
        }
    }

    /**
     * <p>
     * Algo for executing when last order we placed on the exchanges was a SELL.
     * </p>
     *
     * <p>
     * If last sell order filled, we send a new buy order to the trading.
     * </p>
     *
     * @param currentBidPrice the current market BID price.
     * @param currentAskPrice the current market ASK price.
     * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
     *                           Throwing this exception indicates we want the Trading Engine to shutdown the bot.
     */
    private void executeAlgoForWhenLastOrderWasSell(BigDecimal currentBidPrice, BigDecimal currentAskPrice)
            throws StrategyException {

        try  {

            // Fetch our current open orders and see if the sell order is still outstanding on the trading
            final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
            boolean lastOrderFound = false;
            for (final OpenOrder myOrder : myOrders) {
                if (myOrder.getId().equals(lastOrder.id)) {
                    lastOrderFound = true;
                    break;
                }
            }

            // if the order is not there, it must have all filled.
            if (!lastOrderFound) {
                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " ^^^ Yay!!! Last SELL Order Id [" + lastOrder.id + "] filled at [" + lastOrder.price + "]");
                }

                // Get amount of altcoin we can buy for given BTC amount.
                final BigDecimal amountOfAltcoinToBuyForGivenBtc = getAmountOfAltcoinToBuyForGivenBtcAmount(btcBuyOrderAmount);

                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " Placing new BUY order at bid price [" +
                            new DecimalFormat("#.########").format(currentBidPrice) + "]");
                    LOG.info(market.getName() + " Sending new BUY order to trading --->");
                }

                // Send the buy order to the trading.
                lastOrder.id = tradingApi.createOrder(market.getId(), OrderType.BUY, amountOfAltcoinToBuyForGivenBtc, currentBidPrice);

                if (LOG.isInfoEnabled()) {
                    LOG.info(market.getName() + " New BUY Order sent successfully. ID: " + lastOrder.id);
                }

                // update last order details
                lastOrder.price = currentBidPrice;
                lastOrder.type = OrderType.BUY;
                lastOrder.amount = amountOfAltcoinToBuyForGivenBtc;

            } else {

                /*
                 * SELL order not filled yet.
                 * Could be nobody has jumped on it yet... it is only part filled... or market has gone down and we've
                 * been undercut and have a stuck sell order, in which case we have to wait for market to recover...
                 * unless you tweak this code to cancel the current order and lower your ask; remember to deal with any
                 * part-filled orders!
                 */
                if (currentAskPrice.compareTo(lastOrder.price) < 0) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(market.getName() + " <<< Current ask price [" + currentAskPrice
                                + "] is LOWER then last order price ["
                                + lastOrder.price + "] - holding last SELL order...");
                    }
                } else if (currentAskPrice.compareTo(lastOrder.price) > 0) {
                    // TODO throw illegal state exception
                    LOG.error(market.getName() + " >>> Current ask price [" + currentAskPrice
                            + "] is HIGHER than last order price ["
                            + lastOrder.price + "] - IMPOSSIBLE! BXBot must have sold?????");
                } else if (currentAskPrice.compareTo(lastOrder.price) == 0) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info(market.getName() + " === Current ask price [" + currentAskPrice
                                + "] is EQUAL to last order price ["
                                + lastOrder.price + "] - holding last SELL order...");
                    }
                }
            }
        } catch (ExchangeTimeoutException e) {

            // Your timeout handling code could got here, e.g. you might want to check if the order actually
            // made it to the trading? And if not, resend it...
            // We are just going to log it and swallow it, and wait for next trade cycle.
            LOG.error(market.getName() + " New Order to BUY altcoin failed because Exchange threw timeout exception. " +
                    "Waiting until next trade cycle. Last Order: " + lastOrder, e);

        } catch (TradingApiException e) {

            // Your error handling code could go here...
            // We are just going to re-throw as StrategyException for engine to deal with - it will shutdown the bot.
            LOG.error(market.getName() + " New order to BUY altcoin failed because Exchange threw TradingApi exception. " +
                    " Telling Trading Engine to shutdown bot! Last Order: " + lastOrder, e);
            throw new StrategyException(e);
        }
    }

    /**
     * Returns amount of altcoin to buy for a given amount of BTC based on last market trade price.
     *
     * @param amountOfBtcToTrade the amount of BTC we have to trade (buy) with.
     * @return the amount of altcoin we can buy for the given BTC amount.
     * @throws TradingApiException if an unexpected error occurred contacting the trading.
     * @throws ExchangeTimeoutException if a request to the trading has timed out.
     */
    private BigDecimal getAmountOfAltcoinToBuyForGivenBtcAmount(BigDecimal amountOfBtcToTrade) throws
            TradingApiException, ExchangeTimeoutException {

        if (LOG.isInfoEnabled()) {
            LOG.info(market.getName() + " Calculating amount of altcoin to buy for " +
                    new DecimalFormat("#.########").format(amountOfBtcToTrade) + " BTC");
        }

        // Fetch the last trade price
        final BigDecimal lastTradePriceInBtcForOneAltcoin = tradingApi.getLatestMarketPrice(market.getId());
        if (LOG.isInfoEnabled()) {
            LOG.info(market.getName() + " Last trade price for 1 altcoin was: "
                    + new DecimalFormat("#.########").format(lastTradePriceInBtcForOneAltcoin) + " BTC");
        }

        /*
         * Most exchanges (if not all) use 8 decimal places and typically round in favour of the trading.
         * It's usually safest to ROUND DOWN the order quantity in your calculations.
         */
        final BigDecimal amountOfAltcoinToBuyForGivenBtc = amountOfBtcToTrade.divide(
                lastTradePriceInBtcForOneAltcoin, 8, RoundingMode.HALF_DOWN);

        if (LOG.isInfoEnabled()) {
            LOG.info(market.getName() + " Amount of altcoin to BUY for ["
                    + new DecimalFormat("#.########").format(amountOfBtcToTrade)
                    + " BTC] based on last market trade price: " + amountOfAltcoinToBuyForGivenBtc);
        }
        return amountOfAltcoinToBuyForGivenBtc;
    }

    /**
     * Loads the config for the strategy. We expect the btc-buy-order-amount' config item to be present in the
     * ./config/strategies.xml config file.
     *
     * @param config the config for the Trading Strategy.
     */
    private void getConfigForStrategy(StrategyConfig config) {

        final String btcBuyOrderAmountFromConfigAsString = config.getConfigItem("btc-buy-order-amount");
        if (btcBuyOrderAmountFromConfigAsString == null) {
            // game over - kill it off now.
            throw new IllegalArgumentException("Mandatory btc-buy-order-amount value missing in strategy.xml config.");
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("<btc-buy-order-amount> from config is: " + btcBuyOrderAmountFromConfigAsString);
        }

        // will fail fast if value is not a number!
        btcBuyOrderAmount = new BigDecimal(btcBuyOrderAmountFromConfigAsString);
        if (LOG.isInfoEnabled()) {
            LOG.info("btcBuyOrderAmount: " + btcBuyOrderAmount);
        }
    }

    /**
     * <p>
     * Models the state of an Order we have placed on the trading.
     * </p>
     *
     * <p>
     * Typically, you would maintain order state in a database or use some other persistent datasource to recover from
     * restarts and for audit purposes. In this example, we are storing the state in memory to keep it simple.
     * </p>
     *
     * @author gazbert
     */
    private class OrderState {

        /**
         * Id - default to null.
         */
        private String id = null;

        /**
         * Type: buy/sell. We default to null which means no order has been placed yet, i.e. we've just started!
         */
        private OrderType type = null;

        /**
         *  Price to buy/sell at - default to zero.
         */
        private BigDecimal price = new BigDecimal(0);

        /**
         * Number of units to buy/sell - default to zero.
         */
        private BigDecimal amount = new BigDecimal(0);

        @Override
        public String toString() {
            return OrderState.class.getSimpleName()
                    + " ["
                    + "id=" + id
                    + ", type=" + type
                    + ", price=" + price
                    + ", amount=" + amount
                    + "]";
        }
    }
}
