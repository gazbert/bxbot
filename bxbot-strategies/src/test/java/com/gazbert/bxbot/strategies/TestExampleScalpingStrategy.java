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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

/**
 * Tests the behaviour of the example Scalping Strategy. Some fairly decent coverage here, but we've
 * not tested all the exception handling conditions - you will want to do a much more thorough job
 * than this with your own strategies :-)
 *
 * @author gazbert
 */
public class TestExampleScalpingStrategy {

  private static final String MARKET_ID = "btc_usd";
  private static final String BASE_CURRENCY = "BTC";
  private static final String COUNTER_CURRENCY = "USD";

  private static final String CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT = "20"; // USD amount
  private static final String CONFIG_ITEM_MINIMUM_PERCENTAGE_GAIN = "2";

  private TradingApi tradingApi;
  private Market market;
  private StrategyConfig config;

  private MarketOrderBook marketOrderBook;
  private MarketOrder marketBuyOrder;
  private MarketOrder marketSellOrder;

  private List<MarketOrder> marketBuyOrders;
  private List<MarketOrder> marketSellOrders;

  /** Each test will be the same up to the point of fetching the order book. */
  @Before
  public void setUpBeforeEachTest() throws Exception {
    tradingApi = createMock(TradingApi.class);
    market = createMock(Market.class);
    config = createMock(StrategyConfig.class);

    // setup market order book
    marketOrderBook = createMock(MarketOrderBook.class);
    marketBuyOrder = createMock(MarketOrder.class);
    marketBuyOrders = new ArrayList<>();
    marketBuyOrders.add(marketBuyOrder);
    marketSellOrders = new ArrayList<>();
    marketSellOrder = createMock(MarketOrder.class);
    marketSellOrders.add(marketSellOrder);

    // expect config to be loaded
    expect(config.getConfigItem("counter-currency-buy-order-amount"))
        .andReturn(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT);
    expect(config.getConfigItem("minimum-percentage-gain"))
        .andReturn(CONFIG_ITEM_MINIMUM_PERCENTAGE_GAIN);

    // expect Market name to be logged zero or more times. Loose mock behaviour here; name is
    // cosmetic.
    expect(market.getName()).andReturn("BTC_USD").anyTimes();

    // expect market order book to be fetched
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getMarketOrders(MARKET_ID)).andReturn(marketOrderBook);
    expect(marketOrderBook.getBuyOrders()).andReturn(marketBuyOrders);
    expect(marketOrderBook.getSellOrders()).andReturn(marketSellOrders);
  }

  /*
   * Tests scenario when bot has just started and the strategy is invoked for the first time.
   *
   * - Given the bot has just started
   * - When the strategy is first invoked
   * - Then a new buy order is sent to the exchange
   */
  @Test
  public void testStrategySendsInitialBuyOrderWhenItIsFirstCalled() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to get amount of base currency to buy for given counter currency amount
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("1454.018");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send initial buy order to exchange
    final String orderId = "4239407233";
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("0.01375499");
    expect(market.getId()).andReturn(MARKET_ID);
    expect(market.getCounterCurrency()).andReturn(COUNTER_CURRENCY).anyTimes();
    expect(market.getBaseCurrency()).andReturn(BASE_CURRENCY).anyTimes();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.BUY, amountOfUnitsToBuy, bidSpotPrice))
        .andReturn(orderId);

    replay(tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder);
  }

  /*
   * Tests scenario when strategy has had its current buy order filled. We expect it to create a
   * new sell order.
   *
   * - Given the bot has had its current buy order filled
   * - When the strategy is invoked
   * - Then a new sell order is sent to the exchange
   */
  @Test
  public void testStrategySendsNewSellOrderToExchangeWhenCurrentBuyOrderFilled() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.BUY);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the buy order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled

    // expect to send new sell order to exchange
    final BigDecimal requiredProfitInPercent = new BigDecimal("0.02");
    final BigDecimal newAskPrice =
        lastOrderPrice
            .multiply(requiredProfitInPercent)
            .add(lastOrderPrice)
            .setScale(8, RoundingMode.HALF_UP);
    final String orderId = "4239407234";
    expect(market.getId()).andReturn(MARKET_ID).atLeastOnce();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.SELL, lastOrderAmount, newAskPrice))
        .andReturn(orderId);

    replay(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /*
   * Tests scenario when strategy's current buy order is still waiting to be filled. We expect
   * it to hold.
   *
   * - Given the bot has placed a buy order and it had not filled
   * - When the strategy is invoked
   * - Then the bot holds until the next trade cycle
   */
  @Test
  public void testStrategyHoldsWhenCurrentBuyOrderIsNotFilled() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.BUY);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the buy order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    final OpenOrder unfilledOrder = createMock(OpenOrder.class);
    final List<OpenOrder> openOrders = new ArrayList<>();
    openOrders.add(unfilledOrder); // still have open order
    expect(tradingApi.getYourOpenOrders(MARKET_ID)).andReturn(openOrders);

    // expect strategy to find existing open order and hold current position
    expect(openOrders.get(0).getId()).andReturn("45345346");

    replay(
        tradingApi,
        market,
        config,
        marketOrderBook,
        marketBuyOrder,
        marketSellOrder,
        orderState,
        unfilledOrder);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi,
        market,
        config,
        marketOrderBook,
        marketBuyOrder,
        marketSellOrder,
        orderState,
        unfilledOrder);
  }

  /*
   * Tests scenario when strategy has had its current sell order filled. We expect it to create a
   * new buy order.
   *
   * - Given the bot has had its current sell order filled
   * - When the strategy is invoked
   * - Then a new buy order is sent to the exchange
   */
  @Test
  public void testStrategySendsNewBuyOrderToExchangeWhenCurrentSellOrderFilled() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.SELL);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the sell order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled

    // expect to get amount of base currency to buy for given counter currency amount
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("0.015");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send new buy order to exchange
    final String orderId = "4239407233";
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("1333.33333333");
    expect(market.getId()).andReturn(MARKET_ID);
    expect(market.getCounterCurrency()).andReturn(COUNTER_CURRENCY).anyTimes();
    expect(market.getBaseCurrency()).andReturn(BASE_CURRENCY).anyTimes();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.BUY, amountOfUnitsToBuy, bidSpotPrice))
        .andReturn(orderId);

    replay(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /*
   * Tests scenario when strategy's current sell order is still waiting to be filled. We expect
   * it to hold.
   *
   * - Given the bot has placed a sell order and it had not filled
   * - When the strategy is invoked<
   * - Then the bot holds until the next trade cycle<
   */
  @Test
  public void testStrategyHoldsWhenCurrentSellOrderIsNotFilled() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.SELL);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the sell order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    final OpenOrder unfilledOrder = createMock(OpenOrder.class);
    final List<OpenOrder> openOrders = new ArrayList<>();
    openOrders.add(unfilledOrder); // still have open order
    expect(tradingApi.getYourOpenOrders(MARKET_ID)).andReturn(openOrders);

    // expect strategy to find existing open order and hold current position
    expect(openOrders.get(0).getId()).andReturn("45345346");

    replay(
        tradingApi,
        market,
        config,
        marketOrderBook,
        marketBuyOrder,
        marketSellOrder,
        orderState,
        unfilledOrder);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi,
        market,
        config,
        marketOrderBook,
        marketBuyOrder,
        marketSellOrder,
        orderState,
        unfilledOrder);
  }

  // ------------------------------------------------------------------------
  // Timeout exception handling tests
  // ------------------------------------------------------------------------

  /*
   * When attempting to send the initial buy order to the exchange, a timeout exception is received.
   * We expect the strategy to swallow it and exit until the next trade cycle.
   *
   * - Given the strategy has just sent initial buy order
   * - When a timeout exception is caught
   * - Then the strategy returns without error
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingInitialBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to get amount of base currency to buy for given counter currency amount
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("1454.018");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send initial buy order to exchange and receive timeout exception
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("0.01375499");
    expect(market.getId()).andReturn(MARKET_ID);
    expect(market.getCounterCurrency()).andReturn(COUNTER_CURRENCY).anyTimes();
    expect(market.getBaseCurrency()).andReturn(BASE_CURRENCY).anyTimes();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.BUY, amountOfUnitsToBuy, bidSpotPrice))
        .andThrow(new ExchangeNetworkException("Timeout waiting for exchange!"));

    replay(tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder);
  }

  /*
   * When attempting to send a buy order to the exchange, a timeout exception is received.
   * We expect the strategy to swallow it and exit until the next trade cycle.
   *
   * - Given the strategy has just sent a buy order
   * - When a timeout exception is caught
   * - Then the strategy returns without error
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.SELL);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the sell order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled

    // expect to get amount of base currency to buy for given counter currency amount
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("0.015");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send new buy order to exchange and receive timeout exception
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("1333.33333333");
    expect(market.getId()).andReturn(MARKET_ID);
    expect(market.getCounterCurrency()).andReturn(COUNTER_CURRENCY).anyTimes();
    expect(market.getBaseCurrency()).andReturn(BASE_CURRENCY).anyTimes();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.BUY, amountOfUnitsToBuy, bidSpotPrice))
        .andThrow(new ExchangeNetworkException("Timeout waiting for exchange!"));

    replay(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /*
   * When attempting to send a sell order to the exchange, a timeout exception is received.
   * We expect the strategy to swallow it and exit until the next trade cycle.
   *
   * - Given the strategy has just sent a sell order
   * - When a timeout exception is caught
   * - Then the strategy returns without error
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingSellOrder() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.BUY);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the buy order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled

    // expect to send new sell order to exchange and receive timeout exception
    final BigDecimal requiredProfitInPercent = new BigDecimal("0.02");
    final BigDecimal newAskPrice =
        lastOrderPrice
            .multiply(requiredProfitInPercent)
            .add(lastOrderPrice)
            .setScale(8, RoundingMode.HALF_UP);
    expect(market.getId()).andReturn(MARKET_ID).atLeastOnce();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.SELL, lastOrderAmount, newAskPrice))
        .andThrow(new ExchangeNetworkException("Timeout waiting for exchange!"));

    replay(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  // ------------------------------------------------------------------------
  // Trading API exception handling tests
  // ------------------------------------------------------------------------

  /*
   * When attempting to send the initial buy order to the exchange, a Trading API exception is
   * received. We expect the strategy to wrap it in a Strategy exception and throw it to the
   * Trading Engine.
   *
   * - Given the strategy has just sent initial buy order
   * - When a Trading API exception is caught
   * - Then the strategy throws a Strategy exception
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingInitialBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to get amount of base currency to buy for given counter currency amount
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("1454.018");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send initial buy order to exchange and receive timeout exception
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("0.01375499");
    expect(market.getId()).andReturn(MARKET_ID).atLeastOnce();
    expect(market.getCounterCurrency()).andReturn(COUNTER_CURRENCY).atLeastOnce();
    expect(market.getBaseCurrency()).andReturn(BASE_CURRENCY).atLeastOnce();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.BUY, amountOfUnitsToBuy, bidSpotPrice))
        .andThrow(new TradingApiException("Exchange returned a 500 status code!"));

    replay(tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder);
  }

  /*
   * When attempting to send a buy order to the exchange, a Trading API exception is received.
   * We expect the strategy to wrap it in a Strategy exception and throw it to the Trading Engine.
   *
   * - Given the strategy has just sent a buy order
   * - When a Trading API exception is caught
   * - Then the strategy throws a Strategy exception
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.SELL);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the sell order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled

    // expect to get amount of base currency to buy for given counter currency amount
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("0.015");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send new buy order to exchange and receive timeout exception
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("1333.33333333");
    expect(market.getId()).andReturn(MARKET_ID);
    expect(market.getCounterCurrency()).andReturn(COUNTER_CURRENCY).atLeastOnce();
    expect(market.getBaseCurrency()).andReturn(BASE_CURRENCY).atLeastOnce();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.BUY, amountOfUnitsToBuy, bidSpotPrice))
        .andThrow(new TradingApiException("Exchange returned a 500 status code!"));

    replay(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /*
   * When attempting to send a sell order to the exchange, a Trading API exception is received.
   * We expect the strategy to wrap it in a Strategy exception and throw it to the Trading Engine.
   *
   * - Given the strategy has just sent a sell order
   * - When a Trading API exception is caught
   * - Then the strategy throws a Strategy exception
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingSellOrder() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Class orderStateClass =
        Whitebox.getInnerClassType(ExampleScalpingStrategy.class, "OrderState");
    final Object orderState = createMock(orderStateClass);
    Whitebox.setInternalState(orderState, "id", "45345346");
    Whitebox.setInternalState(orderState, "type", OrderType.BUY);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);

    // expect to check if the buy order has filled
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled

    // expect to send new sell order to exchange and receive timeout exception
    final BigDecimal requiredProfitInPercent = new BigDecimal("0.02");
    final BigDecimal newAskPrice =
        lastOrderPrice
            .multiply(requiredProfitInPercent)
            .add(lastOrderPrice)
            .setScale(8, RoundingMode.HALF_UP);
    expect(market.getId()).andReturn(MARKET_ID).atLeastOnce();
    expect(tradingApi.createOrder(MARKET_ID, OrderType.SELL, lastOrderAmount, newAskPrice))
        .andThrow(new TradingApiException("Exchange returned a 500 status code!"));

    replay(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);

    final ExampleScalpingStrategy strategy = new ExampleScalpingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }
}
