# BX-bot

[![Build Status](https://travis-ci.org/gazbert/bxbot.svg?branch=master)](https://travis-ci.org/gazbert/bxbot)
[![Join the chat at https://gitter.im/BX-bot/Lobby](https://badges.gitter.im/BX-bot/Lobby.svg)](https://gitter.im/BX-bot/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)		 	 
 
## What is BX-bot?

<img src="./docs/bxbot-cropped.png" align="right" width="25%" />

BX-bot (_Bex_) is a simple [Bitcoin](https://bitcoin.org) trading bot written in Java for trading on cryptocurrency 
[exchanges](https://bitcoin.org/en/exchanges).

The project contains the basic infrastructure to trade on a [cryptocurrency](http://coinmarketcap.com/) exchange...
except for the trading strategies - you'll need to write those yourself! A simple 
[example](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java) of a 
[scalping](http://www.investopedia.com/articles/trading/02/081902.asp) strategy is included to get you started with the
Trading API - take a look [here](https://github.com/ta4j/ta4j) for more ideas.

Exchange Adapters for using [Bitstamp](https://www.bitstamp.net), [Bitfinex](https://www.bitfinex.com),
[OKCoin](https://www.okcoin.com/), [GDAX](https://www.gdax.com/), [itBit](https://www.itbit.com/),
[Kraken](https://www.kraken.com), and [Gemini](https://gemini.com/) are included.
Feel free to improve these or contribute new adapters to the project; that would be 
[shiny!](https://en.wikipedia.org/wiki/Firefly_(TV_series))

The Trading API provides support for [limit orders](http://www.investopedia.com/terms/l/limitorder.asp)
traded at the [spot price](http://www.investopedia.com/terms/s/spotprice.asp);
it does not support [futures](http://www.investopedia.com/university/beginners-guide-to-trading-futures/) or 
[margin](http://www.investopedia.com/university/margin/) trading.
 
**Warning:** Trading Bitcoin carries significant financial risk; you could lose money. This software is provided 'as is'
and released under the [MIT license](http://opensource.org/licenses/MIT).

## Architecture
![bxbot-core-architecture.png](./docs/bxbot-core-architecture.png)

- **Trading Engine** - the execution unit. It provides a framework for integrating Exchange Adapters and executing Trading Strategies.
- **Exchange Adapters** - the data stream unit. They provide access to a given exchange.
- **Trading Strategies** - the decision or strategy unit. This is where the trading decisions happen.
- **Trading API** - Trading Strategies use this API to make trades. Exchange Adapters implement this to provide access
  to a given exchange.
- **Strategy API** - Trading Strategies implement this so the Trading Engine can execute them.
 
Trading Strategies and Exchange Adapters are injected by the Trading Engine on startup. The bot uses a crude XML based
dependency injection framework to achieve this; the long term goal is to convert it into a fully configurable 
[Spring Boot](http://projects.spring.io/spring-boot/) app.

The bot was designed to fail hard and fast if any unexpected errors occur in the Exchange Adapters or Trading Strategies:
it will log the error, send an email alert (if configured), and then shut down.

## Installation Guide

### The Docker way
If you want to just play around with the 
[`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java) 
and evaluate the bot, Docker is the way to go.

1. Install [Docker](https://docs.docker.com/engine/installation/) on the machine you want to run the bot.
1. Fetch the BX-bot image from [Docker Hub](https://hub.docker.com/r/gazbert/bxbot/): `docker pull gazbert/bxbot:x.x.x` -
   replace `x.x.x` with the [Release](https://github.com/gazbert/bxbot/releases) version of the bot you want to run, e.g.
   `docker pull gazbert/bxbot:0.8.6`
1. Run the Docker container: `docker container run --name bxbot-x.x.x -it gazbert/bxbot:x.x.x bash`
1. Change into the bot's directory: `cd bxbot*`
1. Configure the bot as required - see the main _[Configuration](#configuration-2)_ section. The bot's default 
   configuration uses the 
   [`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java), 
   but you'll probably want to [code your own](#how-do-i-write-my-own-trading-strategy)! The 
   [`TestExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/TestExchangeAdapter.java) is 
   configured by default - it makes public API calls to [Bitstamp](https://www.bitstamp.net), but stubs out the private 
   API (order management) calls; it's good for testing your initial setup without actually sending orders to the exchange.
1. Usage: `./bxbot.sh [start|stop|status]`
1. You can detach from the container and leave the bot running using the `CTRL-p` `CTRL-q` key sequence.
1. To re-attach to the Docker container, run `docker container ls` to get the CONTAINER ID. 
   Then run: `docker container attach <CONTAINER ID>`
   
A Docker image for each release is available on [Docker Hub](https://hub.docker.com/r/gazbert/bxbot/tags/).
  
### The manual way
The [Releases](https://github.com/gazbert/bxbot/releases) page has the stable releases, or you can grab the latest code 
from the head of the master branch.

The bot runs on Linux, macOS, and Windows. The Windows [bxbot.bat](./bxbot.bat) script for starting/stopping the bot is
elementary and needs further development.

BX-bot requires a Java 8 JDK ([openjdk-8-jdk](http://openjdk.java.net/install/) or 
[Oracle JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html))
to be installed on the machine you are going to use to build the bot.     

You can use [Maven](https://maven.apache.org) or [Gradle](https://gradle.org/) to build the bot and distribution artifact.
The instructions below are for Linux and macOS, but equivalent Windows scripts are included. 

#### Maven
1. Download the latest [release](https://github.com/gazbert/bxbot/releases). Unzip the bot.
1. If you plan on using your own Trading Strategies/Exchange Adapters packaged in separate jar files, you'll need to add
   the dependency in the [bxbot-app/pom.xml](./bxbot-app/pom.xml) - see the commented out dependency examples inside it.
1. From the project root, run `./mvnw clean assembly:assembly` to produce the distribution 
   artifacts `bxbot-app-<version>-dist.tar.gz` and `bxbot-app-<version>-dist.zip` in the `./target` folder.
1. Copy either the `bxbot-app-<version>-dist.tar.gz` or the `bxbot-app-<version>-dist.zip` onto the machine you 
   want to run the bot and unzip it someplace.
1. Configure the bot as required - see the main _[Configuration](#configuration-2)_ section.
   The bot's default configuration uses the 
   [`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java), 
   but you'll probably want to [code your own](#how-do-i-write-my-own-trading-strategy)! The 
   [`TestExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/TestExchangeAdapter.java) is configured 
   by default - it makes public API calls to [Bitstamp](https://www.bitstamp.net), but stubs out the private API (order management) 
   calls; it's good for testing your initial setup without actually sending orders to the exchange.   
1. Usage: `./bxbot.sh [start|stop|status]`   
    
#### Gradle    
1. Download the latest [release](https://github.com/gazbert/bxbot/releases). Unzip the bot.
1. If you plan on using your own Trading Strategies/Exchange Adapters packaged in separate jar files, you'll need to add
   the dependency in the [bxbot-app/build.gradle](bxbot-app/build.gradle) - see the commented out dependency examples inside it.
1. From the project root, run `./gradlew clean build` to build the bot.   
1. Then run `./gradlew buildTarGzipDist` or `./gradlew buildZipDist` to build the distribution 
   artifact: either `bxbot-app-<version>.tar.gz` or `bxbot-app-<version>.zip` respectively. 
   It will be placed in the `./build/distributions` folder.
1. Copy the artifact onto the machine you want to run the bot and unzip it someplace.
1. Configure the bot as required - see the main _[Configuration](#configuration-2)_ section.
   The bot's default configuration uses the 
   [`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java), 
   but you'll probably want to [code your own](#how-do-i-write-my-own-trading-strategy)! The 
   [`TestExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/TestExchangeAdapter.java) is configured 
   by default - it makes public API calls to [Bitstamp](https://www.bitstamp.net), but stubs out the private API (order management) 
   calls; it's good for testing your initial setup without actually sending orders to the exchange.
1. Usage: `./bxbot.sh [start|stop|status]`

**NOTE:** You only need a Java 8 JRE ([openjdk-8-jre](http://openjdk.java.net/install/) or 
[Oracle JRE 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)) to be installed on
the machine you want to _run_ the bot.
   
## Build Guide
BX-bot requires a Java 8 JDK ([openjdk-8-jdk](http://openjdk.java.net/install/) or 
[Oracle JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)) for the development
environment.

You can use [Maven](https://maven.apache.org) or [Gradle](https://gradle.org/) to build the bot and pull down the dependencies;
BX-bot depends on [log4j](http://logging.apache.org/log4j), [JavaMail](https://java.net/projects/javamail/pages/Home),
[Google Gson](https://code.google.com/p/google-gson/), [Google Guava](https://github.com/google/guava), and 
[Spring Boot](http://projects.spring.io/spring-boot/).
See the Maven [`pom.xml`](./pom.xml) for details.

The instructions below are for Linux and macOS, but equivalent Windows scripts are included.

### Maven
1. Clone the repo locally (master branch).
1. From the project root, run `./mvnw clean install`.
   If you want to run the exchange integration tests, use `./mvnw clean install -Pint`. 
   To execute both unit and integration tests, use `./mvnw clean install -Pall`.
1. Take a look at the Javadoc in the `./target/apidocs` folders of the bxbot-trading-api, bxbot-strategy-api, 
   and bxbot-exchange-api modules after the build completes.
   
### Gradle
1. Clone the repo locally (master branch).
1. From the project root, run `./gradlew build`.
   If you want to run the exchange integration tests, use `./gradlew integrationTests`.
   To execute both unit and integration tests, use `./gradlew build integrationTests`.
1. To generate the Javadoc, run `./gradlew javadoc` and look in the `./build/docs/javadoc` folders of the bxbot-trading-api, 
   bxbot-strategy-api, and bxbot-exchange-api modules.
   
## Issue & Change Management
Issues and new features are managed using the project [Issue Tracker](https://github.com/gazbert/bxbot/issues) -
submit bugs here.
 
You are welcome to take on new features or fix bugs! See [here](CONTRIBUTING.md) for how to get involved. 

For help and general questions about BX-bot, check out the [Gitter](https://gitter.im/BX-bot/Lobby) channel.

## Testing
The bot has undergone basic unit testing on a _best-effort_ basis. 

There is a continuous integration build running on [Travis CI](https://travis-ci.org/gazbert/bxbot).

The latest stable build can always be found on the [Releases](https://github.com/gazbert/bxbot/releases) page. 
The SNAPSHOT builds on master are active development builds, but the tests should always pass and the bot should always 
be deployable.

## User Guide
### Configuration
The bot provides a simple plugin framework for:

* Exchanges to integrate with.
* Markets to trade on.
* Trading Strategies to execute.

It uses XML configuration files. These live in the [`config`](./config) folder. Any config changes require a restart of
the bot to take effect.

Sample configurations for running on different exchanges can be found in the 
[`config/samples`](./config/samples)folder.

##### Engine
The [`engine.xml`](./config/engine.xml) file is used to configure the Trading Engine.

```xml
<engine>
    <bot-id>my-bitstamp-bot_1</bot-id>
    <bot-name>Bitstamp Bot</bot-name>
    <emergency-stop-currency>BTC</emergency-stop-currency>
    <emergency-stop-balance>1.0</emergency-stop-balance>
    <trade-cycle-interval>20</trade-cycle-interval>
</engine>
```

All elements are mandatory.

* The `<bot-id>` value is a unique identifier for the bot. This is used by 
  [BX-bot UI Server](https://github.com/gazbert/bxbot-ui-server) (work in progress) to identify and route configuration 
  updates and commands to the bot. Value must be an alphanumeric string. Underscores and dashes are also permitted.

* The `<bot-name>` is a friendly name for the bot. The is used by [BX-bot UI](https://github.com/gazbert/bxbot-ui) 
  (work in progress) to display the bot's name. Value must be an alphanumeric string. Spaces are allowed.
      
* The `<emergency-stop-currency>` value must be set to prevent catastrophic loss on the exchange. 
  This is normally the currency you intend to hold a long position in. It should be set to the currency short code for the
  wallet, e.g. BTC, LTC, USD. This value can be case sensitive for some exchanges - check the Exchange Adapter documentation.

* The `<emergency-stop-balance>` value must be set to prevent catastrophic loss on the exchange. 
  The Trading Engine checks this value at the start of every trade cycle: if your `<emergency-stop-currency>` wallet balance on
  the exchange drops below this value, the Trading Engine will log it, send an Email Alert (if configured) and then shut down.
  If you set this value to 0, the bot will bypass the check - be careful.

* The `<trade-cycle-interval>` value is the interval in _seconds_ that the Trading Engine will wait/sleep before executing
  each trade cycle. The minimum value is 1 second. Some exchanges allow you to hit them harder than others. However, while
  their API documentation might say one thing, the reality is you might get socket timeouts and 5xx responses if you hit it
  too hard. You'll need to experiment with the trade cycle interval for different exchanges.

##### Exchange Adapters
You specify the Exchange Adapter you want BX-bot to use in the 
[`exchange.xml`](./config/exchange.xml) file. 

BX-bot only supports 1 Exchange Adapter per bot, but you could have multiple bots running on the same exchange.

```xml
<exchange>
    <name>Bitstamp</name>
    <adapter>com.gazbert.bxbot.exchanges.BitstampExchangeAdapter</adapter>
    <authentication-config>
        <config-item>
            <name>client-id</name>
            <value>your-client-id</value>
        </config-item>    
        <config-item>
            <name>key</name>
            <value>your-api-key</value>
        </config-item>
        <config-item>
            <name>secret</name>
            <value>your-secret-key</value>
        </config-item>
    </authentication-config>
    <network-config>
        <connection-timeout>30</connection-timeout>
        <non-fatal-error-codes>
            <code>502</code>
            <code>503</code>
            <code>520</code>
            <code>522</code>
            <code>525</code>            
        </non-fatal-error-codes>
        <non-fatal-error-messages>
            <message>Connection reset</message>
            <message>Connection refused</message>
            <message>Remote host closed connection during handshake</message>
            <message>Unexpected end of file from server</message>           
        </non-fatal-error-messages>
    </network-config>
    <optional-config>
        <config-item>
            <name>not-needed-on-bitstamp-1</name>
            <value>here for illustration purposes only</value>
        </config-item>
        <config-item>
            <name>not-needed-on-bitstamp-2</name>
            <value>here for illustration purposes only</value>
        </config-item>
    </optional-config>
</exchange>
```

All elements are mandatory unless stated otherwise.

* The `<name>` value is a friendly name for the Exchange. It is used in log statements and by
  [BX-bot UI](https://github.com/gazbert/bxbot-ui) (work in progress) to display the Exchange's name.
  Value must be an alphanumeric string. Spaces are allowed.

* For the `<adapter>` value, you must specify the fully qualified name of the Exchange Adapter class for the Trading Engine
  to inject on startup. The class must be on the runtime classpath. See the 
  _[How do I write my own Exchange Adapter?](#how-do-i-write-my-own-exchange-adapter)_ section for more details.

* The `<authentication-config>` section is optional. If present, at least 1 `<config-item>` must be set - these are repeating
  key/value pairs. This section is used by the inbuilt Exchange Adapters to configure their exchange trading API credentials - see
  the sample `exchange.xml` config files for details.

* The `<network-config>` section is optional. If present, the `<connection-timeout>`, `<non-fatal-error-codes>`, and
  `<non-fatal-error-messages>` sections must be set. This section is used by the inbuilt Exchange Adapters to set
  their network configuration as detailed below:

    * The `<connection-timeout>` is the timeout value that the exchange adapter will wait on socket connect/socket read when
      communicating with the exchange. Once this threshold has been breached, the exchange adapter will give up and throw an
      [`ExchangeNetworkException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/ExchangeNetworkException.java).
      The sample Exchange Adapters are single threaded: if a request gets blocked, it will block all subsequent requests from
      getting to the exchange. This timeout value prevents an indefinite block.

    * The `<non-fatal-error-codes>` section contains a list of HTTP status codes that will trigger the adapter to throw a
      non-fatal `ExchangeNetworkException`.
      This allows the bot to recover from temporary network issues. See the sample `exchange.xml` config files for status codes to use.

    * The `<non-fatal-error-messages>` section contains a list of `java.io` Exception message content that will trigger the 
      adapter to throw a non-fatal `ExchangeNetworkException`. This allows the bot to recover from temporary network issues.
      See the sample `exchange.xml` config files for messages to use.

* The `<optional-config>` section is optional. It is not needed for Bitstamp, but shown above for illustration purposes.
  If present, at least 1 `<config-item>` must be set - these are repeating key/value String pairs.
  This section is used by the inbuilt Exchange Adapters to set any additional config, e.g. buy/sell fees.

##### Markets
You specify which markets you want to trade on in the 
[`markets.xml`](./config/markets.xml) file.

```xml
<markets>      
    <market>
        <id>btcusd</id>    
        <name>BTC/USD</name>        
        <base-currency>BTC</base-currency>
        <counter-currency>USD</counter-currency>
        <enabled>true</enabled>
        <trading-strategy-id>scalping-strategy</trading-strategy-id>
    </market>
    <market>
        <id>ltcusd</id>
        <name>LTC/BTC</name>
        <base-currency>LTC</base-currency>
        <counter-currency>BTC</counter-currency>
        <enabled>false</enabled>
        <trading-strategy-id>scalping-strategy</trading-strategy-id>
    </market>        
</markets>
```

All elements are mandatory unless stated otherwise.

* The `<id>` value is the market id as defined on the exchange. E.g. the BTC/USD market id is `btcusd` on 
  [Bitstamp](https://www.bitstamp.net/api/) - see `currency_pair` values.

* The `<name>` value is a friendly name for the market. The is used in the logs and by
  [BX-bot UI](https://github.com/gazbert/bxbot-ui) (work in progress) to display the market's name.
  Value must be an alphanumeric string.

* The `<base-currency>` value is the currency short code for the base currency in the currency pair. When you buy or sell a
  currency pair, you are performing that action on the base currency. The base currency is the commodity you are buying or
  selling. E.g. in a BTC/USD market, the first currency (BTC) is the base currency and the second currency (USD) is the
  counter currency.

* The `<counter-currency>` value is the currency short code for the counter currency in the currency pair. This is also known
  as the _quote_ currency.

* The `<enabled>` value allows you to toggle trading on the market. Remember, config changes are only applied on startup.

* The `<trading-strategy-id>` value _must_ match a strategy `<id>` defined in your `strategies.xml` config.
  Currently, BX-bot only supports 1 `<strategy>` per `<market>`.

##### Strategies #####
You specify the Trading Strategies you wish to use in the 
[`strategies.xml`](./config/strategies.xml) file.

```xml
<trading-strategies>
    <strategy>
        <id>scalping-strategy</id>
        <name>Basic Scalping Strat</name>
        <description>
         A simple trend following scalper that buys at the current BID price, holds until current market 
         price has reached a configurable minimum percentage gain, and then sells at current ASK price, thereby 
         taking profit from the spread. Don't forget to factor in the exchange fees!
        </description>
        <!-- This strategy is injected using the bot's custom injection framework using its class-name -->
        <class-name>com.gazbert.bxbot.strategies.ExampleScalpingStrategy</class-name>
        <optional-config>
            <config-item>
                <name>counter-currency-buy-order-amount</name>
                <value>20</value>
            </config-item>
            <config-item>
                <name>minimum-percentage-gain</name>
                <value>2</value>
            </config-item>
        </optional-config>
    </strategy>
    <strategy>
        <id>macd-strategy</id>
        <name>MACD Based Strat</name>
        <description>Strat uses MACD data to take long position in USD.</description>
        <!-- This strategy is injected using a Spring bean-name -->
        <bean-name>yourMacdStrategyBean</bean-name>
        <optional-config>
            <config-item>
                <name>counter-currency-buy-order-amount</name>
                <value>20</value>
            </config-item>
            <config-item>
                <name>shortEmaInterval</name>
                <value>12</value>
            </config-item>
            <config-item>
                <name>longEmaInterval</name>
                <value>26</value>
            </config-item>
        </optional-config>
    </strategy>
</trading-strategies>
```

All elements are mandatory unless stated otherwise.

* The `<id>` value is a unique identifier for the strategy. The `markets.xml` `<trading-strategy-id>` entries cross-reference this.
  Value must be an alphanumeric string. Underscores and dashes are also permitted.

* The `<name>` value is a friendly name for the strategy. The is used in the logs and by
  [BX-bot UI](https://github.com/gazbert/bxbot-ui) (work in progress) to display the strategy's name.
  Value must be an alphanumeric string. Spaces are allowed.

* The `<description>` value is optional, and used by [BX-bot UI](https://github.com/gazbert/bxbot-ui) (work in progress)
  to display the strategy's description.

You configure the loading of your strategy using either a `<class-name>` _or_ a `<bean-name>`; you cannot specify both. 

* For the `<class-name>` value, you must specify the fully qualified name of your Strategy class for the Trading Engine
  to load and execute. This will use the bot's custom injection framework. The class must be on the runtime classpath.
  If you set this value to load your strategy, you cannot set the `<bean-name>` value.
  
* For the `<bean-name>` value, you must specify the Spring bean name of you Strategy component class for the Trading Engine
  to load and execute. You will also need to annotate your strategy class with `@Component("yourMacdStrategyBean")` - 
  see the [example strategy](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java).
  This results in Spring injecting the bean.
  If you set this value to load your strategy, you cannot set the `<class-name>` value.        

* The `<optional-config>` section is optional. It allows you to set key/value pair config items. This config is passed
  to your Trading Strategy when the bot starts up; see the 
 _[How do I write my own Trading Strategy?](#how-do-i-write-my-own-trading-strategy)_ section.

##### Email Alerts
You specify the Email Alerts config in the 
[`email-alerts.xml`](./config/email-alerts.xml) file.

This config is used to send email alerts when the bot is forced to shut down due to an unexpected error occurring in the 
Trading Strategies or Exchange Adapters. The email is sent to the SMTP host using TLS.

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

All elements are mandatory unless stated otherwise.

* If `<enabled>` is set to 'true', the bot will send email alerts to the `<to-addr>` if it needs to shut down due to a
  critical error. 

* The `<smtp-config>` config is optional and only required if `<enabled>` is set to 'true'. 
  Sample SMTP config for using a Gmail account is shown above - all elements within `<smtp-config>` are mandatory. 

### How do I write my own Trading Strategy?
_"Battle not with monsters, lest ye become a monster, and if you gaze into the abyss, the abyss gazes also into you."_ - Friedrich Nietzsche

The best place to start is with the
[`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java) -
more ideas can be found in the excellent [ta4j](https://github.com/ta4j/ta4j) project.
There is also a Trading Strategy specific channel on [Gitter](https://gitter.im/BX-bot/trading-strategies).
  
Your strategy must implement the [`TradingStrategy`](./bxbot-strategy-api/src/main/java/com/gazbert/bxbot/strategy/api/TradingStrategy.java)
interface. This allows the Trading Engine to:

* Inject your strategy on startup.
* Pass any configuration (set in the `strategies.xml`) to your strategy.
* Invoke your strategy at each trade cycle.

You load your strategy using either `<class-name>` _or_ `<bean-name>` in the `strategies.xml` file - see the 
_[Strategies Configuration](#strategies)_ section for full details. The choice is yours, but `<bean-name>` is the way to
go if you want to use other Spring features in your strategy, e.g. a 
[Repository](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/stereotype/Repository.html) 
to store your trade data.   

The Trading Engine will only send 1 thread through your Trading Strategy; you do not have to code for concurrency.

##### Making Trades
You use the [`TradingApi`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApi.java)
to make trades etc. The API is passed to your Trading Strategy implementation `init` method when the bot starts up. 
See the Javadoc for full details of the API.

##### Error Handling
Your Trading Strategy implementation should throw a [`StrategyException`](./bxbot-strategy-api/src/main/java/com/gazbert/bxbot/strategy/api/StrategyException.java)
whenever it 'breaks'. BX-bot's error handling policy is designed to fail hard and fast; it will log the error, send an
Email Alert (if configured), and shut down.

Note that the inbuilt Exchange Adapters will (some more often than others!) throw an
[`ExchangeNetworkException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/ExchangeNetworkException.java)
if they encounter network issues connecting with the exchange. Your strategy should always catch this exception and
choose what to do next, e.g. retry the previous Trading API call, or 'swallow' the exception and wait until the Trading
Engine invokes the strategy again at the next trade cycle.

##### Configuration
You specify the Trading Strategies you wish to use in the `strategies.xml` file - see the _[Strategies Configuration](#strategies)_ section 
for full details.

The `<optional-config>` section in the `strategies.xml` allows you to set key/value pair config items to pass to your
Trading Strategy implementation. On startup, the Trading Engine will pass the config to your Trading Strategy's 
`init(TradingApi tradingApi, Market market, StrategyConfig config)` method. 

##### Dependencies
Your Trading Strategy implementation has a compile-time dependency on the [Strategy API](./bxbot-strategy-api)
and the [Trading API](./bxbot-trading-api).

The inbuilt [`ExampleScalpingStrategy`](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies/ExampleScalpingStrategy.java)
also has a compile-time dependency on log4j and Google Guava.

##### Packaging & Deployment #####
To get going fast, you can code your Trading Strategy and place it in the [bxbot-strategies](./bxbot-strategies/src/main/java/com/gazbert/bxbot/strategies)
module alongside the example strategy. When you build the project, your Trading Strategy will be included in the BX-bot jar. 
You can also create your own jar for your strats, e.g. `my-strats.jar`, and include it on BX-bot's runtime classpath -
see the _[Installation Guide](#the-manual-way)_ for how to do this.

### How do I write my own Exchange Adapter?
_"I was seldom able to see an opportunity until it had ceased to be one."_ - Mark Twain

The best place to start is with one of the inbuilt Exchange Adapters - see the latest 
[`BitstampExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/BitstampExchangeAdapter.java)
for example. There is also an Exchange Adapter specific channel on [Gitter](https://gitter.im/BX-bot/exchange-adapters).

Your adapter must implement the [`TradingApi`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApi.java)
and the [`ExchangeAdapter`](./bxbot-exchange-api/src/main/java/com/gazbert/bxbot/exchange/api/ExchangeAdapter.java)
interfaces. This allows the:
            
* Trading Engine to inject your adapter on startup.
* Trading Engine to pass any configuration (set in the `exchange.xml`) to your adapter.
* Trading Strategies to invoke your adapter's implementation of the `TradingApi` at each trade cycle.

[`AbstractExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/AbstractExchangeAdapter.java)
is a handy base class that all the inbuilt Exchange Adapters extend - it could be useful.

The Trading Engine will only send 1 thread through your Exchange Adapter; you do not have to code for concurrency.

##### Error Handling
Your Exchange Adapter implementation should throw a [`TradingApiException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApiException.java)
whenever it breaks; the Trading Strategies should catch this and decide how they want to proceed.

The Trading API provides an [`ExchangeNetworkException`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/ExchangeNetworkException.java)
for adapters to throw when they cannot connect to the exchange to make Trading API calls. This allows for
Trading Strategies to recover from temporary network failures. The `exchange.xml` config file has an optional `<network-config>`
section, which contains `<non-fatal-error-codes>` and `<non-fatal-error-messages>` elements - these can be used to tell the
adapter when to throw the exception.

The first release of the bot is _single-threaded_ for simplicity. The downside to this is that if an API call to the 
exchange gets blocked on IO, BX-bot will get stuck until your Exchange Adapter frees the block. The Trading API provides
an `ExchangeNetworkException` for your adapter to throw if it times-out connecting to the exchange. It is your responsibility to free up any blocked
connections - see the [`AbstractExchangeAdapter`](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges/AbstractExchangeAdapter.java)
for an example how to do this.

The Trading Engine will also call your adapter directly when performing the _Emergency Stop_ check to see if the 
`<emergency-stop-currency>` wallet balance on the exchange drops below the configured `<emergency-stop-value>` value.
If this call to the [`TradingApi`](./bxbot-trading-api/src/main/java/com/gazbert/bxbot/trading/api/TradingApi.java)
`getBalanceInfo()` fails and is not due to a `ExchangeNetworkException`, the Trading Engine will log the error, send an 
Email Alert (if configured), and shut down. If the API call failed due to an `ExchangeNetworkException`, the 
Trading Engine will log the error and sleep until the next trade cycle.

##### Configuration
You provide your Exchange Adapter details in the `exchange.xml` file - see the _[Exchange Adapters Configuration](#exchange-adapters)_ 
section for full details.

The `<optional-config>` section in the `exchange.xml` allows you to set key/value pair config items to pass to your
Exchange Adapter implementation. On startup, the Trading Engine will pass the config to your Exchange Adapter's 
`init(ExchangeConfig config)` method. 

##### Dependencies
Your Exchange Adapter implementation has a compile-time dependency on the [Trading API](./bxbot-trading-api).

The inbuilt Exchange Adapters also have compile-time dependencies on log4j, Google Gson, and Google Guava.

##### Packaging & Deployment
To get going fast, you can code your Exchange Adapter and place it in the 
[bxbot-exchanges](./bxbot-exchanges/src/main/java/com/gazbert/bxbot/exchanges) module alongside the other inbuilt adapters. 
When you build the project, your Exchange Adapter will be included in the BX-bot jar. You can also create your own jar 
for your adapters, e.g. `my-adapters.jar`, and include it on BX-bot's runtime classpath -
see the _[Installation Guide](#the-manual-way)_ for how to do this.

### Logging
Logging for the bot is provided by [log4j](http://logging.apache.org/log4j). The log file is written to `logs/bxbot.log` 
using a rolling policy. When a log file size reaches 100 MB or a new day is started, it is archived and a new log file 
is created. BX-bot will create up to 7 archives on the same day; these are stored in a directory based on the current 
year and month. Only the last 90 archives are kept. Each archive is compressed using gzip. The logging level is set at `info`. 
You can change this default logging configuration in the [`config/log4j2.xml`](./config/log4j2.xml) file.

I recommend running at `info` level, as `debug` level logging will produce a *lot* of
output from the Exchange Adapters; it's very handy for debugging, but not so good for your disk space!
 
## Coming Soon
The following features are in the pipeline:

- Java 9 support - the migration work is being done on the [bxbot-java9](https://github.com/gazbert/bxbot/tree/bxbot-java9) branch.
- A REST API for administering the bot. It's being developed on the [bxbot-restapi](https://github.com/gazbert/bxbot/tree/bxbot-restapi) branch.
- An [admin server](https://github.com/gazbert/bxbot-ui-server) for proxying commands and config updates to BX-bots in the cloud. 
  It will consume the bot's REST API.
- A [Web UI](https://github.com/gazbert/bxbot-ui) written in [Angular](https://angular.io/) for administering multiple
  bots in the cloud. It will integrate with the admin server. 
  
See the main project [Issue Tracker](https://github.com/gazbert/bxbot/issues) for timescales and progress.
