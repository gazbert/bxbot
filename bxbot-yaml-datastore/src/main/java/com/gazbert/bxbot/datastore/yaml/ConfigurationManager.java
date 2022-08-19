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

package com.gazbert.bxbot.datastore.yaml;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * The generic configuration manager loads config from a given YAML config file.
 * 通用配置管理器从给定的 YAML 配置文件加载配置。
 *
 * @author gazbert
 */
public final class ConfigurationManager {

  private static final Logger LOG = LogManager.getLogger();
  private static final String YAML_HEADER = "---" + System.getProperty("line.separator");

  private ConfigurationManager() {
  }

  /**
   * Loads the config from the YAML file.
   * 从 YAML 文件加载配置。
   *
   * @param configClass the config class.
   *                    配置类。
   *
   * @param yamlConfigFile the YAML config filename.
   *                       YAML 配置文件名。
   *
   * @param <T> the type of config.
   *           配置类型。
   *
   * @return the loaded config.
   * |@return 加载的配置。
   */
  public static synchronized <T> T loadConfig(final Class<T> configClass, String yamlConfigFile) {

    LOG.info(
        () -> "Loading configuration for [ 为 [ 加载配置" + configClass + "] from: " + yamlConfigFile + " ...");

    try (final FileInputStream fileInputStream = new FileInputStream(yamlConfigFile)) {
      final Yaml yaml = new Yaml(new Constructor(configClass));
      final T requestedConfig = yaml.load(fileInputStream);

      LOG.info(() -> "Loaded and set configuration for [ 为 [ 加载并设置配置" + configClass + "] successfully! 成功地!");
      return requestedConfig;

    } catch (IOException e) {
      final String errorMsg = "Failed to find or read [ 未能找到或读取 [" + yamlConfigFile + "] config 配置";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);

    } catch (Exception e) {
      final String errorMsg =
          "Failed to load [ 加载失败 [" + yamlConfigFile + "] file. Details: ] 文件.细节：" + e.getMessage();
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  /**
   * Saves the config to the YAML file.
   * 将配置保存到 YAML 文件。
   *
   * @param configClass the config Class.
   *                    配置类。
   *
   * @param config the config object to save.
   *               要保存的配置对象。
   *
   * @param yamlConfigFile the YAML config filename.
   *                       YAML 配置文件名。
   *
   * @param <T> the type of config.
   *           配置类型。
   */
  public static synchronized <T> void saveConfig(
      Class<T> configClass, T config, String yamlConfigFile) {

    LOG.info(() -> "Saving configuration for [ 为 [ 保存配置" + configClass + "] to: " + yamlConfigFile + " ...");

    try (final FileOutputStream fileOutputStream = new FileOutputStream(yamlConfigFile);
        final PrintWriter writer =
            new PrintWriter(fileOutputStream, true, StandardCharsets.UTF_8)) {

      // Skip null fields and order the YAML fields
      final Representer representer = new SkipNullFieldRepresenter();
      representer.setPropertyUtils(new ReversedPropertyUtils());

      final Yaml yaml = new Yaml(representer);
      final StringBuilder sb = new StringBuilder(YAML_HEADER);
      sb.append(yaml.dumpAs(config, Tag.MAP, DumperOptions.FlowStyle.BLOCK));

      LOG.debug(() -> "YAML file content:YAML 文件内容：\n" + sb);
      writer.print(sb);

    } catch (IOException e) {
      final String errorMsg = "Failed to find or read [ 未能找到或读取 [" + yamlConfigFile + "] config 配置";
      LOG.error(errorMsg, e);
      throw new IllegalStateException(errorMsg, e);

    } catch (Exception e) {
      final String errorMsg =
          "Failed to save config to [ 未能将配置保存到 [" + yamlConfigFile + "] file. Details:  ] 文件。细节：" + e.getMessage();
      LOG.error(errorMsg, e);
      throw new IllegalArgumentException(errorMsg, e);
    }
  }

  /** Stops null fields from getting written out to YAML.
   * 停止将空字段写入 YAML。 */
  private static class SkipNullFieldRepresenter extends Representer {
    @Override
    protected NodeTuple representJavaBeanProperty(
        Object javaBean, Property property, Object propertyValue, Tag customTag) {
      if (propertyValue == null) {
        return null;
      } else {
        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
      }
    }
  }

  /** Orders properties before dumping out YAML.
   * 在转储 YAML 之前订购属性。 */
  private static class ReversedPropertyUtils extends PropertyUtils {
    @Override
    protected Set<Property> createPropertySet(Class<?> type, BeanAccess beanAccess) {
      final Set<Property> result = new TreeSet<>(Collections.reverseOrder());
      result.addAll(super.createPropertySet(type, beanAccess));
      return result;
    }
  }
}
