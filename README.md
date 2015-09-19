# BX-bot

_BX-bot is in beta and undergoing a final round of live-testing on the exchanges. 
If you plan on using the current code, be careful! Release 1.0 is coming soon..._

## What is BX-bot?
BX-bot (_Bex_ bot) is a simple Java algorithmic trading bot for trading [Bitcoin](https://bitcoin.org) on 
cryptocurrency [exchanges](https://bitcoinwisdom.com/).

The project contains the basic infrastructure to trade on a [cryptocurrency](http://coinmarketcap.com/) exchange... 
except for the trading strategies; you'll need to write those yourself. A basic example 
[scalping strategy](http://www.investopedia.com/articles/trading/02/081902.asp) is included to get you started with the
Trading API. Take a look [here](http://www.investopedia.com/articles/active-trading/101014/basics-algorithmic-trading-concepts-and-examples.asp)
for more ideas.

Exchange Adapters for using and [BTC-e](https://btc-e.com), [Bitstamp](https://www.bitstamp.net), [Cryptsy](https://www.cryptsy.com), 
and [Bitfinex](https://www.bitfinex.com) are included. Feel free to improve these or contribute new adapters to the 
project; that would be shiny.

The Trading API provides support for [limit orders](http://www.investopedia.com/terms/l/limitorder.asp)
traded at the [spot price](http://www.investopedia.com/terms/s/spotprice.asp);
it does not support [futures](http://www.investopedia.com/university/beginners-guide-to-trading-futures/) or 
[margin](http://www.investopedia.com/university/margin/) trading... yet.
 
**Warning:** Trading Bitcoin carries significant financial risk; you could lose money. This software is provided 'as is'
and released under the [MIT license](http://opensource.org/licenses/MIT).

## Architecture
![bxbot-core-architecture.png](https://github.com/gazbert/BX-bot/blob/master/docs/bxbot-core-architecture.png)

- Trading Engine - the execution unit. It provides a framework for integrating and executing Exchange Adapters and Trading Strategies.
- Exchange Adapters - the data stream unit. They provide access to a given exchange.
- Trading Strategies - the decision or strategy unit. This is where the trading decisions happen.
- Trading API - Trading Strategies use this API to make trades. Exchange Adapters implement this to provide access
  to a given exchange.
- Strategy API - Trading Strategies implement this for the Trading Engine to execute them.
 
Trading Strategies and Exchange Adapters are injected by the Trading Engine on startup. The bot uses a crude XML based
dependency injection framework to achieve this; the long term goal is to convert it to a [Spring Boot](http://projects.spring.io/spring-boot/) app.

The bot was designed to fail hard and fast if any unexpected errors occur in the Exchange Adapters or Trading Strategies:
it will log the error, send an email alert (if configured), and then shutdown.

The first release of BX-bot is _single-threaded_ for simplicity; I am working on a concurrent version.

## Dependencies
BX-bot requires [Oracle JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html) for the
development and runtime environment.

You'll also need [Maven 3](https://maven.apache.org) installed in order to build the bot and pull in the dependencies;
BX-bot depends on [log4j](http://logging.apache.org/log4j/1.2/), [JavaMail](https://java.net/projects/javamail/pages/Home), 
and [Google Gson](https://code.google.com/p/google-gson/). See the pom.xml for details.

## Testing
[![Build Status](https://travis-ci.org/gazbert/BX-bot.svg?branch=master)](https://travis-ci.org/gazbert/BX-bot)

The bot has undergone basic unit testing on a _best-effort_ basis. The project has a continuous integration build 
running on [Travis CI](https://travis-ci.org/).

## Issue & Change Management
Issues and new features will be managed using the project [Issue Tracker](https://github.com/gazbert/BX-bot/issues) - 
please submit bugs here.
 
You are welcome to take on any new features or fix any bugs!

## User Guide

### Configuration
The bot provides a plugin framework for the:

1. Exchanges to use.
1. Markets to trade on.
1. Trading Strategies to execute.

It uses XML configuration files. These live in the `config` folder.

Config changes are only applied at startup; they are _not_ hot.

All configuration elements are mandatory unless specified otherwise.

Sample configurations for running on different exchanges can be found in the `config/samples` folder.

##### Exchange Adapters
You specify the Exchange Adapter you want BX-bot to use in the `exchange.xml` file. 

```xml
<exchange>
    <name>Cryptsy</name>
    <adapter>com.gazbert.bxbot.core.exchanges.CryptsyExchangeAdapter</adapter>
</exchange>
```

All elements are mandatory unless stated otherwise.

The `<name>` value is for descriptive use only. It is used in the log statements.

For the `<adapter>` value, you must specify the fully qualified name of the Exchange Adapter class for the Trading Engine
to inject on startup. The class _must_ be on the runtime classpath. See the _How do I write my own Exchange Adapter?_ 
section for more details.

BX-bot only supports 1 Exchange Adapter for each instance of the bot; you will need to create multiple (runtime) 
instances of the bot to run against different exchanges.

##### Markets
You specify which markets you want to trade on in the `markets.xml` file.

```xml
<markets>
    <market>
        <label>LTC/BTC</label>
        <id>3</id>
        <base-currency>LTC</base-currency>
        <counter-currency>BTC</counter-currency>
        <enabled>true</enabled>
        <trading-strategy>scalping-strategy</trading-strategy>
    </market>          
    <market>
        <label>DOGE/BTC</label>
        <id>132</id>
        <base-currency>DOGE</base-currency>
        <counter-currency>BTC</counter-currency>
        <enabled>false</enabled>
        <trading-strategy>scalping-strategy</trading-strategy>
    </market>         
    <market>
        <label>XRP/BTC</label>
        <id>454</id>
        <base-currency>XRP</base-currency>
        <counter-currency>BTC</counter-currency>
        <enabled>false</enabled>
        <trading-strategy>scalping-strategy</trading-strategy>
    </market>              
</markets>
```

All elements are mandatory unless stated otherwise.

The `<label>` value is for descriptive use only. It is used in the log statements.

The `<id>` value is the market id as defined on the exchange. E.g the Cryptsy LTC market id is 3: https://www.cryptsy.com/markets/view/3

The `<base-currency>` value is the currency short code for the base currency in the currency pair. When you buy or sell a
currency pair, you are performing that action on the base currency. The base currency is the commodity you are buying or
selling. E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency (BTC) is the
counter currency.

The `<counter-currency>` value is the currency short code for the counter currency in the currency pair. This is also known
as the quote currency. E.g. in a LTC/BTC market, the first currency (LTC) is the base currency and the second currency 
(BTC) is the counter currency.

The `<enabled>` value allows you to toggle trading on the market. Remember, config changes are only applied on startup.

The `<trading-strategy>` value _must_ match a strategy `<id>` defined in your `strategies.xml` config.
Currently, BX-bot only supports 1 `<trading-strategy>` per `<market>`.

##### Strategies #####
You specify the Trading Strategies you wish to use in the `strategies.xml` file.

```xml
<trading-strategies>
    <strategy>
        <id>scalping-strategy</id>
        <label>Basic Scalping Strat</label>
        <description>A simple trend following scalper that buys at current BID price and sells at current
         ASK price, taking profit from the spread. The exchange fees are factored in.</description>
        <class-name>com.gazbert.bxbot.core.strategies.ExampleScalpingStrategy</class-name>
        <configuration>
            <config-item>
                <name>btc-buy-order-amount</name>
                <value>0.5</value>
            </config-item>
            <config-item>
                <name>minimumPercentageGain</name>
                <value>1</value>
            </config-item>
        </configuration>
    </strategy>
    <strategy>
        <id>macd-strategy</id>
        <label>MACD Based Strat</label>
        <description>Strat uses MACD data to take long position in USD.</description>
        <class-name>com.gazbert.bxbot.core.strategies.YourMacdStrategy</class-name>
        <configuration>
            <config-item>
                <name>btc-buy-order-amount</name>
                <value>0.5</value>
            </config-item>
            <config-item>
                <name>shortEmaInterval</name>
                <value>12</value>
            </config-item>
            <config-item>
                <name>longEmaInterval</name>
                <value>26</value>
            </config-item>
        </configuration>
    </strategy>
</trading-strategies>
```

All elements are mandatory unless stated otherwise.

The `<id>` value must be unique. The markets.xml `<market><trading-strategy>` entries cross-reference this.

The `<label>` value is for descriptive use only. It is used in the log statements.

The `<description>` value is optional and not used anywhere yet; a new Web UI will in the future.

For the `<class-name>` value, you must specify the fully qualified name of your Trading Strategy class for the
Trading Engine to inject on startup. The class _must_ be on the runtime classpath.

The `<configuration>` section is optional. It allows you to set custom key/value pair String config - this is passed
to your Trading Strategy when the bot starts up; see the _How do I write my own Trading Strategy?_ section.

##### Engine
The `engine.xml` file is used to configure the Trading Engine.

```xml
<engine>
    <emergency-stop-currency>BTC</emergency-stop-currency>
    <emergency-stop-balance>1.0</emergency-stop-balance>
    <trade-cycle-interval>60</trade-cycle-interval>
</engine>
```

All elements are mandatory.

The `<emergency-stop-currency>` value must be set to prevent catastrophic loss on the exchange. 
This is normally the currency you intend to hold a long position in. It should be set to the currency short code for the
wallet, e.g. BTC, LTC, USD. This value can be case sensitive for some exchanges - check the Exchange Adapter documentation.

The `<emergency-stop-balance>` value must be set to prevent catastrophic loss on the exchange. 
The Trading Engine checks this value at the start of every trade cycle: if your `<emergency-stop-currency>` wallet balance on
the exchange drops below this value, the Trading Engine will log it, send an Email Alert (if configured) and then shutdown.

The `<trade-cycle-interval>` value is the interval in _seconds_ that the Trading Engine will wait/sleep before executing
each trade cycle. The minimum value is 1 second. Some exchanges allow you to hit them harder than others. However, while
their API documentation might say one thing, the reality is you might get socket timeouts and 50x responses if you hit it
too hard - you cannot perform HFT over the public internet! I have EMA/MACD strats running every 5mins and 'scalping' 
strats running every 60s on BTC-e. You'll need to experiment with the trade cycle interval for different exchanges.

##### Email Alerts
You specify the Email Alerts config in the `email-alerts.xml` file.

```xml
<email-alerts>
    <enabled>false</enabled>
    <smtp-config>
        <smtp-host>smtp.gmail.com</smtp-host>
        <smtp-tls-port>587</smtp-tls-port>
        <account-username>your.account.username@gmail.com</account-username>
        <account-password>your.account.password</account-password>
        <from-addr>from.addr@gmail.com</from-addr>
        <to-addr>to.addr@gmail.com</to-addr>
    </smtp-config>
</email-alerts>
```

This config is used to send email alerts when the bot is forced to shutdown due to an unexpected error occurring in the 
Trading Strategies or Exchange Adapters.

All elements are mandatory unless stated otherwise.

If `<enabled>` is set to 'true', the bot will load the optional `<smtp-config>` config. If `<enabled>` is set to 'false',
you can omit the `<smtp-config>` config.

If enabled, the bot will send email alerts to the `<to-addr>` if it needs to shutdown due to a critical error.

Sample SMTP config for using a Gmail account is shown above.

The email is sent using TLS.

#### Logging
Logging for the bot is provided by [log4j](http://logging.apache.org/log4j/1.2/). You can configure the logging levels 
in the `resources/log4j.properties` file. I recommend running at INFO level. DEBUG level logging will produce a *lot* of
output from the Exchange Adapters; very handy for debugging, but not so good for your disk space!

### How do I write my own Trading Strategy?
_"Battle not with monsters, lest ye become a monster, and if you gaze into the abyss, the abyss gazes also into you."_ - Friedrich Nietzsche

The best place to start is with the sample Trading Strategy provided - see the latest 
[BasicScalpingExampleStrategy](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/strategies/ExampleScalpingStrategy.java)
for an example. More information can be found 
[here](http://www.investopedia.com/articles/active-trading/101014/basics-algorithmic-trading-concepts-and-examples.asp).

Your strategy must implement the [TradingStrategy](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/strategy/TradingStrategy.java)
interface. This allows the Trading Engine to:

1. inject your strategy on startup of the bot.
1. pass configuration you set up in the `strategies.xml` to your strategy.
1. invoke your strategy during each trade cycle.

The Trading Engine will only send 1 thread through your Trading Strategy; you do not have to code for concurrency.

The project Javadoc will be useful too. It can be found in the `./target/apidocs` folder after running the Maven build -
see the _Build Guide_ section.

##### Making Trades
You use the [TradingApi](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java)
to make trades etc. The API is passed to your Trading Strategy implementation `init` method when the bot starts up. 
See the Javadoc for full details of the API.

##### Error Handling
Your Trading Strategy implementation should throw a [StrategyException](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/strategy/StrategyException.java)
whenever it 'breaks'. BX-bot's error handling policy is designed to fail hard and fast; it will log the error, send an
Email Alert (if configured), and shutdown.

Note that Exchange Adapters can (some more often than others!) throw an [ExchangeTimeoutException](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/ExchangeTimeoutException.java) 
if the adapter has network issues connecting with the exchange. Your strategy should always catch this exception and 
choose what to do next, e.g. retry the previous Trading API call, or 'swallow' the exception and wait until the Trading
Engine invokes the strategy again at the next trade cycle.

##### Configuration
You specify the Trading Strategies you wish to use in the `strategies.xml` file - see the main _Configuration_ section 
for full details.

The optional `<configuration>` section in the `strategies.xml` allows you to set key/value pair String config to pass to
your Trading Strategy implementation.

On startup, the Trading Engine will pass the config to your Trading Strategy's `init` method in the `StrategyConfig` arg. 

##### Dependencies
Your Trading Strategy implementation has a compile-time dependency on the [Strategy API](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/api/strategy)
and the [Trading API](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/api/trading).

The inbuilt [BasicScalpingExampleStrategy](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/strategies/ExampleScalpingStrategy.java)
also has a compile-time dependency on log4j.

##### Packaging & Deployment #####
To get going fast, you can code your Trading Strategy and place it in the [com.gazbert.bxbot.core.strategies](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/strategies)
package alongside the example strategy. When you build the project, your Trading Strategy will be included in the BX-bot jar. 
You can also create your own jar for your strats, e.g. `my-strats.jar`, and include it on BX-bot's runtime classpath -
see the _Installation Guide_ for how to do this.

### How do I write my own Exchange Adapter?
The best place to start is with the Cryptsy Exchange Adapter provided - see the latest [BitstampExchangeAdapter](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/exchanges/BitstampExchangeAdapter.java)
for an example.

Your adapter must implement the [TradingApi](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java)
interface. See the Javadoc for details of the API. This allows for:

1. the main Trading Engine to inject your adapter on startup of the bot.
1. the Trading Strategies to invoke your adapter's implementation of the [TradingApi](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java)
   during each trade cycle.

The Trading Engine will only send 1 thread through your Exchange Adapter; you do not have to code for concurrency.

The project Javadoc will be useful too. It can be found in the `./target/apidocs` folder after running the Maven build -
see the _Build Guide_ section.

##### Error Handling
Your Exchange Adapter implementation should throw a [TradingApiException](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApiException.java)
whenever it breaks; the Trading Strategies will catch this and decide how they want to proceed.

The first release of the bot is _single-threaded_ for simplicity. The downside to this is that if an API call to the 
exchange gets blocked on IO, BX-bot will get stuck until your Exchange Adapter frees the block. The Trading API provides
an [ExchangeTimeoutException](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApiException.java)
for your adapter to throw if it times-out connecting to the exchange. It is your responsibility to free up any blocked
connections - see the [BitstampExchangeAdapter](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/exchanges/BitstampExchangeAdapter.java)
for an example how to do this.

The Trading Engine will also call your adapter directly when performing the _Emergency Stop_ check to see if your 
`<emergency-stop-currency>` wallet balance on the exchange drops below the configured `<emergency-stop-value>` value. If this call to the
[TradingApi](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java)
`getBalanceInfo()` fails and is not due to a [ExchangeTimeoutException](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/ExchangeTimeoutException.java),
the Trading Engine will log the error, send an Email Alert (if configured), and shutdown. If the API call failed due to
a timeout exception, the Trading Engine will log the error and sleep until the next trade cycle.

##### Configuration
You provide your Exchange Adapter details in the `exchange.xml` file - see the main _Configuration_ section for full details.

##### Dependencies
Your Exchange Adapter implementation has a compile-time dependency on the [Trading API](https://github.com/gazbert/BX-bot/blob/master/src/main/java/com/gazbert/bxbot/core/api/trading/TradingApi.java).

The inbuilt Exchange Adapters also have compile-time dependencies on log4j and Gson; your code might not.

##### Packaging & Deployment
To get going fast, you can code your Exchange Adapter and place it in the [com.gazbert.bxbot.core.exchanges](https://github.com/gazbert/BX-bot/tree/master/src/main/java/com/gazbert/bxbot/core/exchanges)
package alongside the other inbuilt adapters. When you build the project, your Exchange Adapter will be included in the BX-bot jar. 
You can also create your own jar for your adapters, e.g. `my-adapters.jar`, and include it on BX-bot's runtime classpath -
see the _Installation Guide_ for how to do this.

## Build Guide
A Maven `pom.xml` is included for building the bot.

1. Clone the BX-bot repo locally.
1. Open the `./config` XML files and configure them as required.
1. If you plan on using one of the inbuilt Exchange Adapters, open the appropriate `./resources/<exchange>/<exchange>-config.properties.template`
   config file, and configure/rename it as per instructions in file.
1. If you plan on using Trading Strategies or Exchange Adapters that are packaged in separate jar files, you'll need to add
   the dependency in the `pom.xml` file - see the commented out examples.
1. Run `mvn assembly:assembly` to build the bot and produce the distribution artifacts `bxbot-core-1.0-SNAPSHOT-dist.tar.gz`
   and `bxbot-core-1.0-SNAPSHOT-dist.zip`. Take a look at the Javadoc in the `./target/apidocs` folder after running this.
1. Next, see the _Installation Instructions_.

## Installation Guide

1. Copy either the `bxbot-core-1.0-SNAPSHOT-dist.tar.gz` or `bxbot-core-1.0-SNAPSHOT-dist.zip` onto the machine you want
   to run the bot.
1. Unzip it into a folder, e.g. `bxbot-home`. Make sure the `bxbot.sh` script is executable: `chmod 755 bxbot.sh`.
1. Open the `bxbot.sh` script and follow the instructions in the file.

To start, stop, and query the bot's status, use `./bxbot.sh [start|stop|status]`
 
## Coming Soon
The following features are going to be developed: 

- [OKcoin](https://www.okcoin.com/) Exchange Adapter.
- [itBit](https://www.itbit.com) Exchange Adapter.
- Convert bot into [Spring Boot](http://projects.spring.io/spring-boot/) app. This will include a new REST API for administering the bot.
- Web UI (written in [AngularJS](https://angularjs.org/), [TypeScript](http://www.typescriptlang.org/), and 
  [Bootstrap](http://getbootstrap.com/)) for administering the bot; it will consume the new Spring Boot app REST API. 
- Android app for administering the bot; it will consume the new Spring Boot app REST API.
- iOS app for administering the bot; it will consume the new Spring Boot app REST API.
- Trade repository API for Trading Strategies to record and analyse their trades - SQL, [MongoDB](https://www.mongodb.org/),
  and [Neo4J](http://neo4j.com/) will be supported.

See the main project [Issue Tracker](https://github.com/gazbert/BX-bot/issues) for timescales and progress.


