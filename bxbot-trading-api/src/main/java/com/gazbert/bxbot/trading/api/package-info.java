/**
 *
 *
 * <h2>Trading API</h2>
 *
 * <p>The Trading API provides the trading operations for Trading Strategies to use.
 *
 * <p>Every Exchange Adapter must implement the {@link com.gazbert.bxbot.trading.api.TradingApi}
 * interface.
 *
 * <p>The current version of the Trading API only supports <a
 * href="http://www.investopedia.com/terms/l/limitorder.asp">limit orders</a> traded at the <a
 * href="http://www.investopedia.com/terms/s/spotprice.asp">spot price</a>. It does not support
 * futures or margin trading.
 *
 * <p>The Trading Engine, Trading Strategies, and Exchange Adapters have a compile-time dependency
 * on this API.
 *
 * @author gazbert
 * @since 1.0
 */
package com.gazbert.bxbot.trading.api;
