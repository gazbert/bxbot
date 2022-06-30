/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
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

package com.gazbert.bxbot.repository.yaml;

import static com.gazbert.bxbot.datastore.yaml.FileLocations.MARKETS_CONFIG_YAML_FILENAME;

import com.gazbert.bxbot.datastore.yaml.ConfigurationManager;
import com.gazbert.bxbot.datastore.yaml.market.MarketsType;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.repository.MarketConfigRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * A Market config repo that uses a YAML backed datastore.
 * 使用 YAML 支持的数据存储的市场配置存储库。
 *
 * @author gazbert
 */
@Repository("marketConfigYamlRepository")
@Transactional
public class MarketConfigYamlRepository implements MarketConfigRepository {

  private static final Logger LOG = LogManager.getLogger();
  private static final String EXISTING_MARKET_CONFIG = " Existing MarketConfig: 现有市场配置：";

  @Override
  public List<MarketConfig> findAll() {
    LOG.info(() -> "Fetching all Market configs... 正在获取所有市场配置...");
    return ConfigurationManager.loadConfig(MarketsType.class, MARKETS_CONFIG_YAML_FILENAME)
        .getMarkets();
  }

  @Override
  public MarketConfig findById(String id) {
    LOG.info(() -> "Fetching Market config for id: 获取 id 的市场配置：" + id);

    final MarketsType marketsType =
        ConfigurationManager.loadConfig(MarketsType.class, MARKETS_CONFIG_YAML_FILENAME);

    return adaptInternalToExternalConfig(
        marketsType.getMarkets().stream()
            .filter(item -> item.getId().equals(id))
            .distinct()
            .collect(Collectors.toList()));
  }

  @Override
  public MarketConfig save(MarketConfig config) {
    final MarketsType marketsType =
        ConfigurationManager.loadConfig(MarketsType.class, MARKETS_CONFIG_YAML_FILENAME);

    final List<MarketConfig> marketConfigs =
        marketsType.getMarkets().stream()
            .filter(item -> item.getId().equals(config.getId()))
            .distinct()
            .collect(Collectors.toList());

    if (config.getId() == null || config.getId().isEmpty()) {
      LOG.info(() -> "About to create MarketConfig: 关于创建 MarketConfig：" + config);

      if (marketConfigs.isEmpty()) {
        final MarketConfig newMarketConfig = new MarketConfig(config);
        newMarketConfig.setId(generateUuid());

        marketsType.getMarkets().add(newMarketConfig);
        ConfigurationManager.saveConfig(
            MarketsType.class, marketsType, MARKETS_CONFIG_YAML_FILENAME);

        final MarketsType updatedInternalMarketsConfig =
            ConfigurationManager.loadConfig(MarketsType.class, MARKETS_CONFIG_YAML_FILENAME);

        return adaptInternalToExternalConfig(
            updatedInternalMarketsConfig.getMarkets().stream()
                .filter(item -> item.getId().equals(newMarketConfig.getId()))
                .distinct()
                .collect(Collectors.toList()));
      } else {
        throw new IllegalStateException(
            "Trying to create new MarketConfig but null/empty id already exists.  尝试创建新的 MarketConfig 但 null/空 id 已经存在。 \""
                + "MarketConfig: 市场配置："
                + config
                + EXISTING_MARKET_CONFIG
                + marketsType.getMarkets());
      }
    } else {
      LOG.info(() -> "About to update MarketConfig: 即将更新市场配置：" + config);

      if (!marketConfigs.isEmpty()) {

        marketsType.getMarkets().remove(marketConfigs.get(0)); // will only be 1 unique strat 将只有 1 个独特的层
        marketsType.getMarkets().add(config);
        ConfigurationManager.saveConfig(
            MarketsType.class, marketsType, MARKETS_CONFIG_YAML_FILENAME);

        final MarketsType updatedMarketsType =
            ConfigurationManager.loadConfig(MarketsType.class, MARKETS_CONFIG_YAML_FILENAME);

        return adaptInternalToExternalConfig(
            updatedMarketsType.getMarkets().stream()
                .filter(item -> item.getId().equals(config.getId()))
                .distinct()
                .collect(Collectors.toList()));
      } else {
        LOG.warn(
            () ->
                "Trying to update MarketConfig but id does not exist MarketConfig: 尝试更新 MarketConfig 但 id 不存在 MarketConfig："
                    + config
                    + EXISTING_MARKET_CONFIG
                    + marketsType.getMarkets());
        return null;
      }
    }
  }

  @Override
  public MarketConfig delete(String id) {
    LOG.info(() -> "Deleting Market config for id: 删除 id 的市场配置：" + id);

    final MarketsType marketsType =
        ConfigurationManager.loadConfig(MarketsType.class, MARKETS_CONFIG_YAML_FILENAME);

    final List<MarketConfig> marketConfigs =
        marketsType.getMarkets().stream()
            .filter(item -> item.getId().equals(id))
            .distinct()
            .collect(Collectors.toList());

    if (!marketConfigs.isEmpty()) {
      final MarketConfig marketToRemove = marketConfigs.get(0); // will only be 1 unique strat 将只有 1 个独特的层
      marketsType.getMarkets().remove(marketToRemove);
      ConfigurationManager.saveConfig(MarketsType.class, marketsType, MARKETS_CONFIG_YAML_FILENAME);
      return adaptInternalToExternalConfig(Collections.singletonList(marketToRemove));
    } else {
      LOG.warn(
          () ->
              "Trying to delete MarketConfig but id does not exist. MarketConfig id: 尝试删除 MarketConfig 但 id 不存在。市场配置 ID："
                  + id
                  + EXISTING_MARKET_CONFIG
                  + marketsType.getMarkets());
      return null;
    }
  }

  // --------------------------------------------------------------------------
  // Adapter methods  // 适配器方法
  // --------------------------------------------------------------------------

  private static MarketConfig adaptInternalToExternalConfig(
      List<MarketConfig> internalMarketConfigItems) {
    if (!internalMarketConfigItems.isEmpty()) {
      // Should only ever be 1 unique Market id   // 只能是 1 个唯一的 Market id
      return internalMarketConfigItems.get(0);
    }
    return null;
  }

  // --------------------------------------------------------------------------
  // Util methods // 实用方法
  // --------------------------------------------------------------------------

  private String generateUuid() {
    return UUID.randomUUID().toString();
  }
}
