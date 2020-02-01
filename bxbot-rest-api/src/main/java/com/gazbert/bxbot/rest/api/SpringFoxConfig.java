/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 gazbert
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

package com.gazbert.bxbot.rest.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Encapsulates the SpringFox Swagger config for documenting the bot's REST API.
 *
 * @author gazbert
 */
@Configuration
@EnableSwagger2
public class SpringFoxConfig {

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title("BX-bot REST API")
        .description(
            "Here be the documentation for using the bot's REST API."
                + System.lineSeparator()
                + System.lineSeparator()
                + "_\"Had I the heaven's embroidered cloths,"
                + System.lineSeparator()
                + "Enwrought with golden and silver light,"
                + System.lineSeparator()
                + "The blue and the dim and the dark cloths"
                + System.lineSeparator()
                + "Of night and light and the half-light;"
                + System.lineSeparator()
                + "I would spread the cloths under your feet:"
                + System.lineSeparator()
                + "But I, being poor, have only my dreams;"
                + System.lineSeparator()
                + "I have spread my dreams under your feet;"
                + System.lineSeparator()
                + "Tread softly because you tread on my dreams.\"_"
                + System.lineSeparator()
                + System.lineSeparator()
                + "W.B. Yeats")
        .termsOfServiceUrl("https://github.com/gazbert/bxbot")
        .contact("https://github.com/gazbert")
        .license("MIT")
        .licenseUrl("https://github.com/gazbert/bxbot/blob/master/LICENSE")
        .version("1.0")
        .build();
  }

  /**
   * Builds the SpringFox Docket for describing the bot's REST API.
   *
   * @return the Swagger Docket.
   */
  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.basePackage("com.gazbert.bxbot.rest.api"))
        .paths(PathSelectors.ant("/api/**"))
        .build()
        .apiInfo(apiInfo());
  }
}
