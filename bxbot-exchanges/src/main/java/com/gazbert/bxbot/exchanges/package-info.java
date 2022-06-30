/**
 *
 *
 * <h2>Exchange Adapters</h2>
 *
 * <p>Trading Strategies will use Exchange Adapters to execute trades on the exchange via the {@link com.gazbert.bxbot.trading.api.TradingApi}.
 * * <p>交易策略将使用交易所适配器通过 {@link com.gazbert.bxbot.trading.api.TradingApi} 在交易所执行交易。
 *
 * <p>The Trading Engine will initialise Exchange Adapters via the {@link * com.gazbert.bxbot.exchange.api.ExchangeAdapter}.
 * * <p>交易引擎将通过 {@link * com.gazbert.bxbot.exchange.api.ExchangeAdapter} 初始化交换适配器。
 *
 * <p>You can write your own Exchange Adapters and keep them here. Alternatively, you can package
  them up in a separate jar and place it on BX-bot's runtime classpath. Your Exchange Adapter must:
 <p>您可以编写自己的 Exchange 适配器并将它们保留在此处。或者，您可以打包
 它们放在一个单独的 jar 中，并将其放在 BX-bot 的运行时类路径中。您的 Exchange 适配器必须：
 *
 * <ol>
 *   <li>implement the {@link com.gazbert.bxbot.exchange.api.ExchangeAdapter} and {@link
        com.gazbert.bxbot.trading.api.TradingApi} interfaces.
    <li>be placed on the Trading Engine's runtime classpath: keep it here, or in a separate jar file.
 <li>实现 {@link com.gazbert.bxbot.exchange.api.ExchangeAdapter} 和 {@link
com.gazbert.bxbot.trading.api.TradingApi} 接口。
 <li>放置在交易引擎的运行时类路径中：将其保存在此处，或保存在单独的 jar 文件中。

 *   <li>include a configuration entry in the exchanges.xml file.
 *   * <li>在exchanges.xml 文件中包含一个配置条目。
 * </ol>
 *
 * <p>See the project README "How do I write my own Exchange Adapter?" section.
 * <p>请参阅项目自述文件“我如何编写自己的 Exchange 适配器？”部分。
 *
 * @author gazbert
 * @since 1.0
 */
package com.gazbert.bxbot.exchanges;
