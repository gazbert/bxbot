/**
 *
 *
 * <h2>Trading API</h2>
 *  * <h2>交易接口</h2>
 *
 * <p>The Trading API provides the trading operations for Trading Strategies to use.
 *  <p>交易 API 提供交易操作以供交易策略使用。
 *
 * <p>Every Exchange Adapter must implement the {@link com.gazbert.bxbot.trading.api.TradingApi} interface.
 *  * <p>每个 Exchange 适配器都必须实现 {@link com.gazbert.bxbot.trading.api.TradingApi} 接口。
 *
 * <p>The current version of the Trading API only supports <a href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a> traded at the
  <a href="http://www.investopedia.com/terms/s/spotprice.asp">spot price</a>. It does not support
  futures or margin trading.
 <p>当前版本的交易 API 仅支持 <a href="http://www.investopedia.com/terms/l/limitorder.asp">限价订单</a>
 <a href="http://www.investopedia.com/terms/s/spotprice.asp">现货价格</a>。它不支持
 期货或保证金交易。

 *
 * <p>The Trading Engine, Trading Strategies, and Exchange Adapters have a compile-time dependency on this API.
 * * <p>交易引擎、交易策略和交易适配器在编译时依赖于这个 API。
 *
 * @author gazbert
 * @since 1.0
 */
package com.gazbert.bxbot.trading.api;
