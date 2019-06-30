/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
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

package com.gazbert.bxbot.rest.api.v1;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for all Controller test classes.
 *
 * @author gazbert
 */
public abstract class AbstractControllerTest {

  protected static final String API_ENDPOINT_BASE_URI = "/api/v1";
  protected static final String INVALID_USER_PASSWORD = "not-valid-password";

  // This must match security.user.name in the src/test/resources/application.properties file.
  protected static final String VALID_USER_LOGIN_ID = "unit-test-user";

  // This must match a security.user.password in the src/test/resources/application.properties file.
  protected static final String VALID_USER_PASSWORD = "unit-test-password";

  // We'll always be sending/receiving JSON content in REST API.
  protected static final MediaType CONTENT_TYPE =
      new MediaType(
          MediaType.APPLICATION_JSON.getType(),
          MediaType.APPLICATION_JSON.getSubtype(),
          Charset.forName("utf8"));

  // Used to convert Java objects into JSON - roll on Java 9... ;-)
  private HttpMessageConverter mappingJackson2HttpMessageConverter;

  @Autowired protected WebApplicationContext ctx;

  @Autowired protected FilterChainProxy springSecurityFilterChain;

  protected MockMvc mockMvc;

  @Autowired
  protected void setConverters(HttpMessageConverter<?>[] converters) {
    mappingJackson2HttpMessageConverter =
        Arrays.stream(converters)
            .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
            .findAny()
            .orElse(null);

    Assert.assertNotNull(
        "The JSON message converter must not be null", mappingJackson2HttpMessageConverter);
  }

  // --------------------------------------------------------------------------
  // Shared utils
  // --------------------------------------------------------------------------

  protected String buildAuthorizationHeaderValue(String username, String password) {
    return "Basic "
        + new String(
            Base64Utils.encode((username + ":" + password).getBytes(StandardCharsets.UTF_8)),
            Charset.forName("UTF-8"));
  }

  protected String jsonify(Object objectToJsonify) throws IOException {
    final MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
    mappingJackson2HttpMessageConverter.write(
        objectToJsonify, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
    return mockHttpOutputMessage.getBodyAsString();
  }
}
