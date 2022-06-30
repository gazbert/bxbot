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
  not tested all the exception handling conditions - you will want to do a much more thorough job
  than this with your own strategies :-)
 测试示例剥头皮策略的行为。这里有一些相当不错的报道，但我们已经
 没有测试所有的异常处理条件 - 你会想要做一个更彻底的工作
 比这用你自己的策略:-)
 *
 * @author gazbert
 */
public class TestExampleScalpingStrategy {

  private static final String MARKET_ID = "btc_usd";
  private static final String BASE_CURRENCY = "BTC";
  private static final String COUNTER_CURRENCY = "USD";

  private static final String CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT = "20"; // USD amount 美元金额
  private static final String CONFIG_ITEM_MINIMUM_PERCENTAGE_GAIN = "2";

  private TradingApi tradingApi;
  private Market market;
  private StrategyConfig config;

  private MarketOrderBook marketOrderBook;
  private MarketOrder marketBuyOrder;
  private MarketOrder marketSellOrder;

  private List<MarketOrder> marketBuyOrders;
  private List<MarketOrder> marketSellOrders;

  /** Each test will be the same up to the point of fetching the order book.
   * 在获取订单簿之前，每个测试都是相同的。 */
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
    // 期望配置被加载
    expect(config.getConfigItem("counter-currency-buy-order-amount"))
        .andReturn(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT);
    expect(config.getConfigItem("minimum-percentage-gain"))
        .andReturn(CONFIG_ITEM_MINIMUM_PERCENTAGE_GAIN);

    // expect Market name to be logged zero or more times. Loose mock behaviour here; name is cosmetic.
    // 期望市场名称被记录零次或多次。这里松散的模拟行为；名字是化妆品。
    expect(market.getName()).andReturn("BTC_USD").anyTimes();

    // expect market order book to be fetched
    // 期望获取市场订单簿
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getMarketOrders(MARKET_ID)).andReturn(marketOrderBook);
    expect(marketOrderBook.getBuyOrders()).andReturn(marketBuyOrders);
    expect(marketOrderBook.getSellOrders()).andReturn(marketSellOrders);
  }

  /**
   * Tests scenario when bot has just started and the strategy is invoked for the first time.
   ** 测试机器人刚启动并首次调用策略时的场景。
   * - Given the bot has just started
   * * - 鉴于机器人刚刚启动
   *
   * - When the strategy is first invoked
   * * - 首次调用策略时
   *
   * - Then a new buy order is sent to the exchange
   * * - 然后一个新的买单被发送到交易所
   */
  @Test
  public void testStrategySendsInitialBuyOrderWhenItIsFirstCalled() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to get amount of base currency to buy for given counter currency amount
    // 对于给定的对应货币数量，期望获得要购买的基础货币数量
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("1454.018");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send initial buy order to exchange
    // 期望发送初始买单到交易所
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

  /**
   * Tests scenario when strategy has had its current buy order filled. We expect it to create a new sell order.
   ** 测试策略已完成其当前买单时的场景。我们希望它创建一个新的卖单。
   *
   * - Given the bot has had its current buy order filled
   * * - 鉴于机器人已完成其当前的购买订单
   *
   * - When the strategy is invoked
   * * - 调用策略时
   *
   * - Then a new sell order is sent to the exchange
   * * - 然后一个新的卖单被发送到交易所
   */
  @Test
  public void testStrategySendsNewSellOrderToExchangeWhenCurrentBuyOrderFilled() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    // 模拟一个现有的买单状态
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
    // 期望检查买单是否已经成交
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled  空列表；订单已成交

    // expect to send new sell order to exchange
    // 期望发送新的卖单来交换
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
    // 注入现有的买单
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /**
   * Tests scenario when strategy's current buy order is still waiting to be filled. We expect
    it to hold.
   当策略的当前买单仍在等待执行时测试场景。我们期待
   它举行。
   *
   * - Given the bot has placed a buy order and it had not filled
   * * - 鉴于机器人已经下了一个买单并且它还没有成交
   *
   * - When the strategy is invoked
   * * - 调用策略时
   *
   * - Then the bot holds until the next trade cycle
   * * - 然后机器人保持到下一个交易周期
   */
  @Test
  public void testStrategyHoldsWhenCurrentBuyOrderIsNotFilled() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    // 模拟一个现有的买单状态
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
    // 期望检查买单是否已经成交
    expect(market.getId()).andReturn(MARKET_ID);
    final OpenOrder unfilledOrder = createMock(OpenOrder.class);
    final List<OpenOrder> openOrders = new ArrayList<>();
    openOrders.add(unfilledOrder); // still have open order 仍有未结订单
    expect(tradingApi.getYourOpenOrders(MARKET_ID)).andReturn(openOrders);

    // expect strategy to find existing open order and hold current position
    // 期望策略找到现有的未结订单并持有当前头寸
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
    // 注入现有的买单
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

  /**
   * Tests scenario when strategy has had its current sell order filled. We expect it to create a new buy order.
   * * 当策略已完成其当前卖单时测试场景。我们预计它会创建一个新的买单。
   *
   * - Given the bot has had its current sell order filled
   * * - 鉴于机器人已经完成了当前的卖单
   *
   * - When the strategy is invoked
   * * - 调用策略时
   *
   * - Then a new buy order is sent to the exchange
   * * - 然后一个新的买单被发送到交易所
   */
  @Test
  public void testStrategySendsNewBuyOrderToExchangeWhenCurrentSellOrderFilled() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    // 模拟现有的卖单状态
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
    // 期望检查卖单是否已成交
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled // 空列表；订单已成交

    // expect to get amount of base currency to buy for given counter currency amount
    // 对于给定的对应货币数量，期望获得要购买的基础货币数量
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("0.015");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send new buy order to exchange
    // 期望发送新的买单来交换
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
    // 注入现有的卖单
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    // 运行测试
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /**
   * Tests scenario when strategy's current sell order is still waiting to be filled. We expect
    it to hold.
   当策略的当前卖单仍在等待执行时测试场景。我们期待
   它举行。
   *
   * - Given the bot has placed a sell order and it had not filled
   * * - 鉴于机器人已下卖单但尚未成交
   *
   * - When the strategy is invoked<
   * * - 调用策略时<
   *
   * - Then the bot holds until the next trade cycle<
   * * - 然后机器人保持到下一个交易周期<
   */
  @Test
  public void testStrategyHoldsWhenCurrentSellOrderIsNotFilled() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    // 模拟现有的卖单状态
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
    // 期望检查卖单是否已成交
    expect(market.getId()).andReturn(MARKET_ID);
    final OpenOrder unfilledOrder = createMock(OpenOrder.class);
    final List<OpenOrder> openOrders = new ArrayList<>();
    openOrders.add(unfilledOrder); // still have open order  仍有未结订单
    expect(tradingApi.getYourOpenOrders(MARKET_ID)).andReturn(openOrders);

    // expect strategy to find existing open order and hold current position
    // 期望策略找到现有的未结订单并持有当前头寸
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
    // 注入现有的卖单
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    // 运行测试
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
  // 超时异常处理测试
  // ------------------------------------------------------------------------

  /**
   * When attempting to send the initial buy order to the exchange, a timeout exception is received.
   * * 尝试向交易所发送初始买单时，收到超时异常。
   *
   * We expect the strategy to swallow it and exit until the next trade cycle.
   * * 我们预计该策略会吞下它并退出，直到下一个交易周期。
   *
   * - Given the strategy has just sent initial buy order
   * * - 鉴于策略刚刚发送了初始买单
   *
   * - When a timeout exception is caught
   * * - 当捕获到超时异常时
   *
   * - Then the strategy returns without error
   * * - 然后策略返回没有错误
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingInitialBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to get amount of base currency to buy for given counter currency amount
    // 对于给定的对应货币数量，期望获得要购买的基础货币数量
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("1454.018");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send initial buy order to exchange and receive timeout exception
    // 期望发送初始买单到交易所并接收超时异常
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

  /**
   * When attempting to send a buy order to the exchange, a timeout exception is received.
   * * 尝试向交易所发送买单时，收到超时异常。
   *
   * We expect the strategy to swallow it and exit until the next trade cycle.
   * * 我们预计该策略会吞下它并退出，直到下一个交易周期。
   *
   * - Given the strategy has just sent a buy order
   * * - 鉴于策略刚刚发送了一个买单
   *
   * - When a timeout exception is caught
   * * - 当捕获到超时异常时
   *
   * - Then the strategy returns without error
   * * - 然后策略返回没有错误
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    // 模拟现有的卖单状态
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
    // 期望检查卖单是否已成交
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled 空列表；订单已成交

    // expect to get amount of base currency to buy for given counter currency amount
    // 对于给定的对应货币数量，期望获得要购买的基础货币数量
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("0.015");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send new buy order to exchange and receive timeout exception
    // 期望发送新的买单到交易所并收到超时异常
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
    // 注入现有的卖单
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    // 运行测试
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /**
   * When attempting to send a sell order to the exchange, a timeout exception is received.
   * * 尝试向交易所发送卖单时，收到超时异常。
   *
   * We expect the strategy to swallow it and exit until the next trade cycle.
   * * 我们预计该策略会吞下它并退出，直到下一个交易周期。
   *
   * - Given the strategy has just sent a sell order
   * * - 鉴于策略刚刚发出卖单
   *
   * - When a timeout exception is caught
   * * - 当捕获到超时异常时
   *
   * - Then the strategy returns without error
   * * - 然后策略返回没有错误
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingSellOrder() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    // 模拟一个现有的买单状态
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
    // 期望检查买单是否已经成交
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled 空列表；订单已成交

    // expect to send new sell order to exchange and receive timeout exception
    // 期望发送新的卖单到交易所并收到超时异常
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
    // 注入现有的买单
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    // 运行测试
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  // ------------------------------------------------------------------------
  // Trading API exception handling tests
  // 交易API异常处理测试
  // ------------------------------------------------------------------------

  /**
   * When attempting to send the initial buy order to the exchange, a Trading API exception is
    received. We expect the strategy to wrap it in a Strategy exception and throw it to the
    Trading Engine.
   尝试将初始购买订单发送到交易所时，交易 API 异常是
   已收到。我们希望该策略将其包装在一个 Strategy 异常中并将其抛出到
   交易引擎。
   *
   * - Given the strategy has just sent initial buy order
   * * - 鉴于策略刚刚发送了初始买单
   * - When a Trading API exception is caught
   * * - 当捕获到交易 API 异常时
   *
   * - Then the strategy throws a Strategy exception
   * * - 然后策略抛出一个策略异常
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingInitialBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to get amount of base currency to buy for given counter currency amount
    // 对于给定的对应货币数量，期望获得要购买的基础货币数量
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("1454.018");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send initial buy order to exchange and receive timeout exception
    // 期望发送初始买单到交易所并接收超时异常
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

  /**
   * When attempting to send a buy order to the exchange, a Trading API exception is received.
   * * 尝试向交易所发送买单时，收到交易 API 异常。
   *
   * We expect the strategy to wrap it in a Strategy exception and throw it to the Trading Engine.
   * * 我们希望策略将其包装在一个策略异常中并将其扔给交易引擎。
   *
   * - Given the strategy has just sent a buy order
   * * - 鉴于策略刚刚发送了一个买单
   *
   * - When a Trading API exception is caught
   * * - 当捕获到交易 API 异常时
   *
   * - Then the strategy throws a Strategy exception
   * * - 然后策略抛出一个策略异常
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    // 模拟现有的卖单状态
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
    // 期望检查卖单是否已成交
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled 空列表；订单已成交

    // expect to get amount of base currency to buy for given counter currency amount
    // 对于给定的对应货币数量，期望获得要购买的基础货币数量
    expect(market.getId()).andReturn(MARKET_ID);
    final BigDecimal lastTradePrice = new BigDecimal("0.015");
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    // expect to send new buy order to exchange and receive timeout exception
    // 期望发送新的买单到交易所并收到超时异常
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
    // 注入现有的卖单
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }

  /**
   * When attempting to send a sell order to the exchange, a Trading API exception is received.
   * * 尝试向交易所发送卖单时，收到交易 API 异常。
   *
   * We expect the strategy to wrap it in a Strategy exception and throw it to the Trading Engine.
   * * 我们希望策略将其包装在一个策略异常中并将其扔给交易引擎。
   *
   * - Given the strategy has just sent a sell order
   * * - 鉴于策略刚刚发出卖单
   *
   * - When a Trading API exception is caught
   * * - 当捕获到交易 API 异常时
   * - Then the strategy throws a Strategy exception
   * * - 然后策略抛出一个策略异常
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingSellOrder() throws Exception {
    // expect to get current bid and ask spot prices
    // 期望获得当前出价和询价现货价格
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    // 模拟一个现有的买单状态
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
    // 期望检查买单是否已经成交
    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.getYourOpenOrders(MARKET_ID))
        .andReturn(new ArrayList<>()); // empty list; order has filled 空列表；订单已成交

    // expect to send new sell order to exchange and receive timeout exception
    // 期望发送新的卖单到交易所并收到超时异常
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
    // 注入现有的买单
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    // run test
    // 运行测试
    strategy.init(tradingApi, market, config);
    strategy.execute();

    verify(
        tradingApi, market, config, marketOrderBook, marketBuyOrder, marketSellOrder, orderState);
  }
}
