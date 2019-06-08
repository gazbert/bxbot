/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 nodueck
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

package com.gazbert.bxbot.exchanges;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.exchange.api.ExchangeConfig;
import com.gazbert.bxbot.exchange.api.OtherConfig;
import com.gazbert.bxbot.exchanges.trading.api.impl.BalanceInfoImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderBookImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.MarketOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.OpenOrderImpl;
import com.gazbert.bxbot.exchanges.trading.api.impl.TickerImpl;
import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.Ticker;
import com.gazbert.bxbot.trading.api.TradingApiException;
import com.google.common.base.MoreObjects;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cryptopia Exchange Adapter.
 *
 * @author nodueck
 */
@Deprecated // 18 May 2019 : The exchange has gone into liquidation. See notice on: https://www.cryptopia.co.nz/
public class CryptopiaExchangeAdapter extends AbstractExchangeAdapter implements ExchangeAdapter {

  private static final Logger LOG = LogManager.getLogger();

  private static final String PUBLIC_API_BASE_URL = "https://www.cryptopia.co.nz/api/";
  private static final String AUTHENTICATED_API_URL = PUBLIC_API_BASE_URL;

  private static final String UNEXPECTED_ERROR_MSG = "Unexpected error has occurred in Cryptopia Exchange Adapter. ";
  private static final String UNEXPECTED_IO_ERROR_MSG = "Failed to connect to Exchange due to unexpected IO error.";

  private static final String PUBLIC_KEY_PROPERTY_NAME = "public_key";
  private static final String PRIVATE_KEY_PROPERTY_NAME = "private_key";

  private static final BigDecimal DEFAULT_CRYPTOPIA_TRADING_FEE_PERCENT = new BigDecimal("0.2");
  private static final String USE_GLOBAL_TRADING_FEE_PROPERTY_NAME = "use_global_trading_fee";
  private static final String GLOBAL_TRADING_FEE_PROPERTY_NAME = "global_trading_fee";

  private String publicKey = "";
  private String privateKey = "";

  private MessageDigest md5;
  private Mac mac;
  private boolean initializedAuthentication = false;

  private Gson gson;

  private boolean useGlobalTradingFee = false;
  private BigDecimal globalTradingFee;

  /*
   * Dateformat used by Cryptopia
   * e.g. 2014-12-07T20:04:05.3947572 (numbers after dot are removed before parsing)
   */
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  @Override
  public String getImplName() {
    return "Cryptopia API";
  }

  @Override
  public void init(ExchangeConfig config) {
    LOG.info(() -> "About to initialise Cryptopia ExchangeConfig: " + config);
    setAuthenticationConfig(config);
    setNetworkConfig(config);
    setOtherConfig(config);

    initSecureMessageLayer();
    initGson();
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId) throws ExchangeNetworkException, TradingApiException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("GetMarketOrders/"
                                                                            + marketId.toUpperCase());
      LOG.debug(() -> "Market Orders response: " + response);

      Type responseApiType = new TypeToken<CryptopiaPublicApiResponse<CryptopiaOrderBook>>() {
      }.getType();
      final CryptopiaPublicApiResponse<CryptopiaOrderBook> cryptopiaResponse = gson.fromJson(response.getPayload(),
          responseApiType);
      final CryptopiaOrderBook cryptopiaMarket = cryptopiaResponse.Data;

      final List<MarketOrder> buyOrders = new ArrayList<>();
      for (CryptopiaOrder cryptopiaBuyOrder : cryptopiaMarket.Buy) {
        final MarketOrder buyOrder = new MarketOrderImpl(
            OrderType.BUY,
            cryptopiaBuyOrder.Price,
            cryptopiaBuyOrder.Volume,
            cryptopiaBuyOrder.Total);
        buyOrders.add(buyOrder);
      }

      final List<MarketOrder> sellOrders = new ArrayList<>();
      for (CryptopiaOrder cryptopiaSellOrder : cryptopiaMarket.Sell) {
        final MarketOrder sellOrder = new MarketOrderImpl(
            OrderType.SELL,
            cryptopiaSellOrder.Price,
            cryptopiaSellOrder.Volume,
            cryptopiaSellOrder.Total);
        sellOrders.add(sellOrder);
      }
      return new MarketOrderBookImpl(marketId, sellOrders, buyOrders);
    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId) throws ExchangeNetworkException, TradingApiException {
    try {
      final Map<String, Object> params = createRequestParamMap();
      params.put("Market", marketId);
      params.put("Count", Long.parseLong("100")); //show up to 100 open orders
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("GetOpenOrders", params);
      LOG.debug(() -> "Open Orders response: " + response);

      Type responseApiType = new TypeToken<CryptopiaPrivateApiResponse<CryptopiaOpenOrders>>() {
      }.getType();
      final CryptopiaPrivateApiResponse<CryptopiaOpenOrders> cryptopiaResponse = gson.fromJson(response.getPayload(),
          responseApiType);
      final CryptopiaOpenOrders cryptopiaOpenOrders = cryptopiaResponse.Data;

      final List<OpenOrder> ordersToReturn = new ArrayList<>();
      for (final CryptopiaOpenOrder cryptopiaOpenOrder : cryptopiaOpenOrders) {

        //e.g. Market: DOT/BTC, but we need dot_btc
        if (!marketId.equalsIgnoreCase(cryptopiaOpenOrder.Market.replace("/", "_"))) {
          continue;
        }

        OrderType orderType;
        switch (cryptopiaOpenOrder.Type) {
          case "Buy":
            orderType = OrderType.BUY;
            break;
          case "Sell":
            orderType = OrderType.SELL;
            break;
          default:
            throw new TradingApiException(
                "Unrecognised order type received in getYourOpenOrders(). Value: " + cryptopiaOpenOrder.Type);
        }

        final OpenOrder order = new OpenOrderImpl(
            Long.toString(cryptopiaOpenOrder.OrderId),
            dateFormat.parse(cryptopiaOpenOrder.TimeStamp.split("\\.")[0]),
            marketId,
            orderType,
            cryptopiaOpenOrder.Rate,
            cryptopiaOpenOrder.Amount,
            cryptopiaOpenOrder.Amount.add(cryptopiaOpenOrder.Remaining),
            cryptopiaOpenOrder.Total);

        ordersToReturn.add(order);
      }
      return ordersToReturn;

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public String createOrder(String marketId, OrderType orderType, BigDecimal quantity, BigDecimal price)
      throws ExchangeNetworkException, TradingApiException {

    try {
      final Map<String, Object> params = createRequestParamMap();

      // transform e.g. dot_btc to DOT/BTC
      final String market = marketId.replace("_", "/").toUpperCase();
      params.put("market", market);
      params.put("type", orderType.getStringValue());
      // note we need to limit amount and price to 8 decimal places else exchange will barf
      params.put("rate", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(price));
      params.put("amount", new DecimalFormat("#.########", getDecimalFormatSymbols()).format(quantity));

      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("SubmitTrade", params);
      LOG.debug(() -> "Create Order response: " + response);

      Type responseApiType = new TypeToken<CryptopiaPrivateApiResponse<CryptopiaNewOrderResponse>>() {
      }.getType();
      final CryptopiaPrivateApiResponse<CryptopiaNewOrderResponse> cryptopiaResponse = gson.fromJson(
          response.getPayload(), responseApiType);
      final CryptopiaNewOrderResponse createOrderResponse = cryptopiaResponse.Data;

      final long id = createOrderResponse.OrderId;
      if (id == 0) {
        final String errorMsg = "Failed to place order on exchange. Error response: " + response;
        LOG.error(errorMsg);
        throw new TradingApiException(errorMsg);
      } else {
        return Long.toString(createOrderResponse.OrderId);
      }

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public boolean cancelOrder(String orderId, String marketId) throws ExchangeNetworkException, TradingApiException {
    try {
      final Map<String, Object> params = createRequestParamMap();
      params.put("Type", "Trade"); // to cancel a single Trade by order id
      params.put("OrderId", Long.parseLong(orderId));
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("CancelTrade", params);
      LOG.debug(() -> "Cancel Order response: " + response);

      final Type responseApiType = new TypeToken<CryptopiaPrivateApiResponse<CryptopiaCancelOrderResponse>>() {
      }.getType();
      final CryptopiaPrivateApiResponse<CryptopiaCancelOrderResponse> cryptopiaResponse = gson.fromJson(
          response.getPayload(), responseApiType);
      return cryptopiaResponse.Success;
    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId) throws ExchangeNetworkException, TradingApiException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("GetMarket/" + marketId.toUpperCase());
      LOG.debug(() -> "Market response: " + response);

      final Type responseApiType = new TypeToken<CryptopiaPublicApiResponse<CryptopiaMarket>>() {
      }.getType();
      final CryptopiaPublicApiResponse<CryptopiaMarket> cryptopiaResponse = gson.fromJson(response.getPayload(),
          responseApiType);
      return cryptopiaResponse.Data.LastPrice;
    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BalanceInfo getBalanceInfo() throws ExchangeNetworkException, TradingApiException {
    try {
      final ExchangeHttpResponse response = sendAuthenticatedRequestToExchange("GetBalance", null);
      LOG.debug(() -> "Balance response: " + response);

      final Type responseApiType = new TypeToken<CryptopiaPrivateApiResponse<CryptopiaBalances>>() {
      }.getType();
      final CryptopiaPrivateApiResponse<CryptopiaBalances> cryptopiaResponse = gson.fromJson(response.getPayload(),
          responseApiType);
      final CryptopiaBalances cryptopiaBalances = cryptopiaResponse.Data;

      final Map<String, BigDecimal> availableBalances = new HashMap<>();
      final Map<String, BigDecimal> onholdBalances = new HashMap<>();
      for (CryptopiaBalance balance : cryptopiaBalances) {
        availableBalances.put(balance.Symbol, balance.Available);
        onholdBalances.put(balance.Symbol, balance.Unconfirmed);
      }
      return new BalanceInfoImpl(availableBalances, onholdBalances);
    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      if (useGlobalTradingFee) {
        return globalTradingFee;
      } else {
        final ExchangeHttpResponse response = sendPublicRequestToExchange("GetTradePairs");
        LOG.debug(() -> "Buy Fee response: " + response);
        final Type responseApiType = new TypeToken<CryptopiaPrivateApiResponse<CryptopiaTradePairs>>() {
        }.getType();
        final CryptopiaPrivateApiResponse<CryptopiaTradePairs> cryptopiaResponse = gson.fromJson(response.getPayload(),
            responseApiType);
        final CryptopiaTradePairs cryptopiaTradePairs = cryptopiaResponse.Data;


        for (CryptopiaTradePair tradePair : cryptopiaTradePairs) {
          if (tradePair.Symbol.equalsIgnoreCase(marketId.split("_")[0])) {
            return tradePair.TradeFee;
          }
        }

        LOG.error("getPercentageOfBuyOrderTakenForExchangeFee: marketId {} not found.", marketId);
        throw new TradingApiException(UNEXPECTED_ERROR_MSG);
      }
    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId)
      throws TradingApiException, ExchangeNetworkException {
    try {
      if (useGlobalTradingFee) {
        return globalTradingFee;
      } else {
        final ExchangeHttpResponse response = sendPublicRequestToExchange("GetTradePairs");
        LOG.debug(() -> "Buy Fee response: " + response);
        final Type responseApiType = new TypeToken<CryptopiaPrivateApiResponse<CryptopiaTradePairs>>() {
        }.getType();
        final CryptopiaPrivateApiResponse<CryptopiaTradePairs> cryptopiaResponse = gson.fromJson(response.getPayload(),
            responseApiType);
        final CryptopiaTradePairs cryptopiaTradePairs = cryptopiaResponse.Data;


        for (CryptopiaTradePair tradePair : cryptopiaTradePairs) {
          if (tradePair.Symbol.equalsIgnoreCase(marketId.split("_")[0])) {
            return tradePair.TradeFee;
          }
        }

        LOG.error("getPercentageOfBuyOrderTakenForExchangeFee: marketId {} not found.", marketId);
        throw new TradingApiException(UNEXPECTED_ERROR_MSG);
      }
    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  @Override
  public Ticker getTicker(String marketId) throws TradingApiException, ExchangeNetworkException {
    try {
      final ExchangeHttpResponse response = sendPublicRequestToExchange("GetMarket/" + marketId.toUpperCase());
      LOG.debug(() -> "Latest Market Price response: " + response);

      final Type responseApiType = new TypeToken<CryptopiaPublicApiResponse<CryptopiaMarket>>() {
      }.getType();
      final CryptopiaPublicApiResponse<CryptopiaMarket> cryptopiaResponse = gson.fromJson(response.getPayload(),
          responseApiType);
      final CryptopiaMarket cryptopiaMarket = cryptopiaResponse.Data;
      return new TickerImpl(
          cryptopiaMarket.LastPrice,
          cryptopiaMarket.BidPrice,
          cryptopiaMarket.AskPrice,
          cryptopiaMarket.Low,
          cryptopiaMarket.High,
          cryptopiaMarket.Open,
          cryptopiaMarket.Volume,
          null, // vwap not supplied by Cryptopia
          new Date().getTime());

    } catch (ExchangeNetworkException | TradingApiException e) {
      throw e;
    } catch (Exception e) {
      LOG.error(UNEXPECTED_ERROR_MSG, e);
      throw new TradingApiException(UNEXPECTED_ERROR_MSG, e);
    }
  }

  // ------------------------------------------------------------------------------------------------
  //  GSON classes for JSON responses.
  //  See https://www.cryptopia.co.nz/Forum/Thread/255 and https://www.cryptopia.co.nz/Forum/Thread/256
  // ------------------------------------------------------------------------------------------------

  /**
   * GSON class for mapping returned order from 'GetMarket' API call response.
   */
  private static class CryptopiaMarket {

    public long TradePairId;
    public String Label;
    public BigDecimal AskPrice;
    public BigDecimal BidPrice;
    public BigDecimal Low;
    public BigDecimal High;
    public BigDecimal Volume;
    public BigDecimal LastPrice;
    public BigDecimal BuyVolume;
    public BigDecimal SellVolume;
    public BigDecimal Change;
    public BigDecimal Open;
    public BigDecimal Close;
    public BigDecimal BaseVolume;
    public BigDecimal BaseBuyVolume;
    public BigDecimal BaseSellVolume;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("tradePairId", TradePairId)
                 .add("label", Label)
                 .add("askPrice", AskPrice)
                 .add("bidPrice", BidPrice)
                 .add("low", Low)
                 .add("high", High)
                 .add("volume", Volume)
                 .add("lastPrice", LastPrice)
                 .add("buyVolume", BuyVolume)
                 .add("sellVolume", SellVolume)
                 .add("change", Change)
                 .add("open", Open)
                 .add("close", Close)
                 .add("baseVolume", BaseVolume)
                 .add("baseBuyVolume", BaseBuyVolume)
                 .add("baseSellVolume", BaseSellVolume)
                 .toString();
    }
  }

  /**
   * GSON class for receiving your open orders in 'orders' API call response.
   */
  private static class CryptopiaOpenOrders extends ArrayList<CryptopiaOpenOrder> {
    private static final long serialVersionUID = 5516523641153401953L;
  }

  /**
   * GSON class for mapping returned order from 'orders' API call response.
   */
  private static class CryptopiaOpenOrder {

    public long OrderId;
    public long TradePairId;
    public String Market;
    public String Type;
    public BigDecimal Rate;
    public BigDecimal Amount;
    public BigDecimal Total;
    public BigDecimal Remaining;
    public String TimeStamp;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("orderId", OrderId)
                 .add("tradePairId", TradePairId)
                 .add("market", Market)
                 .add("type", Type)
                 .add("rate", Rate)
                 .add("amount", Amount)
                 .add("total", Total)
                 .add("remaining", Remaining)
                 .add("timeStamp", TimeStamp)
                 .toString();
    }
  }

  /**
   * GSON class for a market Order Book.
   */
  private static class CryptopiaOrderBook {
    CryptopiaOrder[] Buy;
    CryptopiaOrder[] Sell;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("buy", Buy)
                 .add("sell", Sell)
                 .toString();
    }
  }

  /**
   * GSON class for a Market Order.
   */
  private static class CryptopiaOrder {
    public Integer TradePairId;
    public String Label;
    public BigDecimal Price;
    public BigDecimal Volume;
    public BigDecimal Total;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("tradePairId", TradePairId)
                 .add("label", Label)
                 .add("price", Price)
                 .add("volume", Volume)
                 .add("total", Total)
                 .toString();
    }
  }

  /**
   * GSON class for receiving your balances in 'GetBalance' API call response.
   */
  private static class CryptopiaBalances extends ArrayList<CryptopiaBalance> {
    private static final long serialVersionUID = -5454933973151400131L;
  }

  /**
   * GSON class for Balance.
   */
  private static class CryptopiaBalance {
    public long CurrencyId;
    public String Symbol;
    public BigDecimal Total;
    public BigDecimal Available;
    public BigDecimal Unconfirmed;
    public BigDecimal HeldForTrades;
    public BigDecimal PendingWithdraw;
    public String Address;
    public String BaseAddress;
    public String Status;
    public String StatusMessage;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("currencyId", CurrencyId)
                 .add("symbol", Symbol)
                 .add("total", Total)
                 .add("available", Available)
                 .add("unconfirmed", Unconfirmed)
                 .add("heldForTrades", HeldForTrades)
                 .add("pendingWithdraw", PendingWithdraw)
                 .add("address", Address)
                 .add("baseAddress", BaseAddress)
                 .add("status", Status)
                 .add("statusMessage", StatusMessage)
                 .toString();
    }
  }

  /**
   * GSON class for receiving a response when putting a new order.
   */
  private static class CryptopiaNewOrderResponse {
    long OrderId;
    long[] FilledOrders;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("orderId", OrderId)
                 .add("filledOrders", FilledOrders)
                 .toString();
    }
  }

  /**
   * GSON class for receiving a response when cancelling an order.
   */
  private static class CryptopiaCancelOrderResponse extends ArrayList<Long> {
    private static final long serialVersionUID = 1285853976415626644L;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .toString();
    }
  }

  /**
   * GSON class for receiving your trade pairs in 'GetTradePair' API call response.
   */
  private static class CryptopiaTradePairs extends ArrayList<CryptopiaTradePair> {
    private static final long serialVersionUID = -6787427618355089266L;
  }

  private static class CryptopiaTradePair {
    public long Id;
    public String Label;
    public String Currency;
    public String Symbol;
    public String BaseCurrency;
    public String BaseSymbol;
    public String Status;
    public String StatusMessage;
    public BigDecimal TradeFee;
    public BigDecimal MinimumTrade;
    public BigDecimal MaximumTrade;
    public BigDecimal MinimumBaseTrade;
    public BigDecimal MaximumBaseTrade;
    public BigDecimal MinimumPrice;
    public BigDecimal MaximumPrice;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("id", Id)
                 .add("label", Label)
                 .add("currency", Currency)
                 .add("symbol", Symbol)
                 .add("baseCurrency", BaseCurrency)
                 .add("baseSymbol", BaseSymbol)
                 .add("status", Status)
                 .add("statusMessage", StatusMessage)
                 .add("tradeFee", TradeFee)
                 .add("minimumTrade", MinimumTrade)
                 .add("maximumTrade", MaximumTrade)
                 .add("minimumBaseTrade", MinimumBaseTrade)
                 .add("maximumBaseTrade", MaximumBaseTrade)
                 .add("minimumPrice", MinimumPrice)
                 .add("maximumPrice", MaximumPrice)
                 .toString();
    }
  }

  /**
   * GSON class which wraps all other objects as data attribute
   * Differs slightly for public and private api calls.
   */
  private static class CryptopiaPublicApiResponse<T> {
    public boolean Success;
    public String Message;
    public T Data;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("success", Success)
                 .add("message", Message)
                 .add("data", Data)
                 .toString();
    }
  }

  /**
   * GSON class which wraps all other objects as data attribute
   * Differs slightly for public and private api calls.
   */
  private static class CryptopiaPrivateApiResponse<T> {
    public boolean Success;
    public String Error;
    public T Data;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
                 .add("success", Success)
                 .add("error", Error)
                 .add("data", Data)
                 .toString();
    }
  }

  /*
   * Initialises the secure messaging layer.
   * Sets up the MAC to safeguard the data we send to the exchange.
   * Used to encrypt the hash of the entire message with the private key to ensure message integrity.
   * We fail hard n fast if any of this stuff blows.
   */
  private void initSecureMessageLayer() {
    try {
      final byte[] base64DecodedSecret = Base64.getDecoder().decode(privateKey);

      final SecretKeySpec keyspec = new SecretKeySpec(base64DecodedSecret, "HmacSHA256");
      mac = Mac.getInstance("HmacSHA256");
      mac.init(keyspec);

      md5 = MessageDigest.getInstance("MD5");

      initializedAuthentication = true;

    } catch (NoSuchAlgorithmException e) {
      final String errorMsg = "Failed to setup MAC or MessageDigest security. HINT: Is HmacSHA256/MD5 installed?";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);
    } catch (InvalidKeyException e) {
      final String errorMsg = "Failed to setup MAC/MessageDigest security. Secret key seems invalid!";
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  private ExchangeHttpResponse sendPublicRequestToExchange(String apiMethod) throws ExchangeNetworkException,
                                                                                        TradingApiException {
    try {
      final URL url = new URL(PUBLIC_API_BASE_URL + apiMethod);
      return makeNetworkRequest(url, "GET", null, createHeaderParamMap());

    } catch (MalformedURLException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  /*
   * Makes an authenticated API call to the Cryptopia exchange.
   *
   * According to https://www.cryptopia.co.nz/Forum/Thread/256
   *
   * Authentication:
   * Authenticated methods require the use of an api key and can only be accessed via the POST method.
   * Authorization is performed by sending the following variables into the request authentication header:
   *
   * Authentication Method:
   * SCHEME: 'amx'
   * PARAMETER: 'API_KEY + ':' + REQUEST_SIGNATURE + ':' + NONCE' signed by secret key according to HMAC-SHA256 method.
   *
   * Request Structure:
   * REQUEST_SIGNATURE: API_KEY + "POST" + URI + NONCE + HASHED_POST_PARAMS
   * API_KEY: Your Cryptopia api key
   * URI: the request uri. e.g. https://www.cryptopia.co.nz/Api/SubmitTrade
   * HASHED_POST_PARAMS: Base64 encoded MD5 hash of the post parameters
   * NONCE: unique indicator for each request.
   */
  private ExchangeHttpResponse sendAuthenticatedRequestToExchange(String apiMethod, Map<String, Object> params)
      throws ExchangeNetworkException, TradingApiException {

    if (!initializedAuthentication) {
      final String errorMsg = "MAC Message security layer has not been initialized.";
      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }

    try {
      if (params == null) {
        params = createRequestParamMap();
      }

      final URL url = new URL(AUTHENTICATED_API_URL + apiMethod);
      final String paramsInJson = gson.toJson(params);

      //Generate authorization header
      final String nonce = generateNonce();
      final String encodedUrl = URLEncoder.encode(url.toString(), StandardCharsets.UTF_8.toString()).toLowerCase();
      final String md5Checksum = encodeBase64(md5.digest(paramsInJson.getBytes(StandardCharsets.UTF_8)));
      final String requestSignature = publicKey + "POST" + encodedUrl + nonce + md5Checksum;
      final String encodedAndHashedSignature = encodeBase64(mac.doFinal(requestSignature.getBytes(StandardCharsets.UTF_8)));
      final String authHeader = "amx " + publicKey + ":" + encodedAndHashedSignature + ":" + nonce;

      // Request headers required by Exchange
      final Map<String, String> requestHeaders = createHeaderParamMap();
      requestHeaders.put("Authorization", authHeader);
      requestHeaders.put("Content-Type", "application/json");

      return makeNetworkRequest(url, "POST", paramsInJson, requestHeaders);

    } catch (MalformedURLException | UnsupportedEncodingException e) {
      final String errorMsg = UNEXPECTED_IO_ERROR_MSG;
      LOG.error(errorMsg, e);
      throw new TradingApiException(errorMsg, e);
    }
  }

  // ------------------------------------------------------------------------------------------------
  //  Config methods
  // ------------------------------------------------------------------------------------------------

  private void setAuthenticationConfig(ExchangeConfig exchangeConfig) {
    final AuthenticationConfig authenticationConfig = getAuthenticationConfig(exchangeConfig);
    publicKey = getAuthenticationConfigItem(authenticationConfig, PUBLIC_KEY_PROPERTY_NAME);
    privateKey = getAuthenticationConfigItem(authenticationConfig, PRIVATE_KEY_PROPERTY_NAME);
  }

  private void setOtherConfig(ExchangeConfig exchangeConfig) {
    final OtherConfig otherConfig = getOtherConfig(exchangeConfig);

    final String useGlobalTradingFeeConfigItem = getOtherConfigItem(otherConfig, USE_GLOBAL_TRADING_FEE_PROPERTY_NAME);
    useGlobalTradingFee = !StringUtils.isEmpty(useGlobalTradingFeeConfigItem) &&
                              Boolean.parseBoolean(useGlobalTradingFeeConfigItem);

    final String globalTradingFeeConfigItem = useGlobalTradingFee ? getOtherConfigItem(otherConfig,
        GLOBAL_TRADING_FEE_PROPERTY_NAME) : null;
    globalTradingFee = !StringUtils.isEmpty(globalTradingFeeConfigItem) ? new BigDecimal(globalTradingFeeConfigItem) :
                           DEFAULT_CRYPTOPIA_TRADING_FEE_PERCENT;
  }

  // ------------------------------------------------------------------------------------------------
  //  Util methods
  // ------------------------------------------------------------------------------------------------

  private void initGson() {
    gson = new GsonBuilder()
               .setLenient() //since all names in json starts upper case
               .create();
  }

  private String generateNonce() {
    return String.valueOf(System.currentTimeMillis());
  }

  private String encodeBase64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  /*
   * Hack for unit-testing map params passed to transport layer.
   */
  private Map<String, Object> createRequestParamMap() {
    return new HashMap<>();
  }

  /*
   * Hack for unit-testing header params passed to transport layer.
   */
  private Map<String, String> createHeaderParamMap() {
    return new HashMap<>();
  }

  /*
   * Hack for unit-testing transport layer.
   */
  private ExchangeHttpResponse makeNetworkRequest(
      URL url, String httpMethod, String postData, Map<String, String> requestHeaders) throws
      TradingApiException, ExchangeNetworkException {
    return super.sendNetworkRequest(url, httpMethod, postData, requestHeaders);
  }
}
