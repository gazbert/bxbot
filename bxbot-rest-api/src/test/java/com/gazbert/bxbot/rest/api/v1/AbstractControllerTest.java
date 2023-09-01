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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.util.AssertionErrors.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for all Controller test classes.
 *
 * @author gazbert
 */
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public abstract class AbstractControllerTest {

  protected static final String API_ENDPOINT_BASE_URI = "/api/v1";

  // This must match a user's USERNAME in the user table in src/main/config/import.sql
  protected static final String VALID_USER_NAME = "user";

  // This must match a user's PASSWORD in the user table in src/main/resources/import.sql
  protected static final String VALID_USER_PASSWORD = "user";

  // This must match an admin's USERNAME in the user table in src/main/resources/import.sql
  protected static final String VALID_ADMIN_NAME = "admin";

  // This must match an admin's PASSWORD in the user table in src/main/resources/import.sql
  protected static final String VALID_ADMIN_PASSWORD = "admin";

  // Used to convert Java objects into JSON
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

    assertNotNull(
        "The JSON message converter must not be null", mappingJackson2HttpMessageConverter);
  }

  // --------------------------------------------------------------------------
  // Shared utils
  // --------------------------------------------------------------------------

  protected String buildAuthorizationHeaderValue(String username, String password) {
    return "Basic "
        + new String(
            Base64.getEncoder()
                .encode((username + ":" + password).getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
  }

  /*
   * Builds a JWT response.
   * Kudos to @royclarkson for his OAuth2 version:
   * https://github.com/royclarkson/spring-rest-service-oauth
   */
  protected String getJwt(String username, String password) throws Exception {

    final String content =
        mockMvc
            .perform(
                post("/api/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonify(new UsernameAndPassword(username, password))))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token", is(notNullValue())))
            .andReturn()
            .getResponse()
            .getContentAsString();

    final ObjectMapper objectMapper = new ObjectMapper();
    final JwtResponse jwtResponse = objectMapper.readValue(content, JwtResponse.class);
    return jwtResponse.getToken();
  }

  protected String jsonify(Object objectToJsonify) throws IOException {
    final MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
    mappingJackson2HttpMessageConverter.write(
        objectToJsonify, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
    return mockHttpOutputMessage.getBodyAsString();
  }

  // --------------------------------------------------------------------------
  // Private helpers
  // --------------------------------------------------------------------------

  private static class UsernameAndPassword {

    private String username;
    private String password;

    UsernameAndPassword(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  private static class JwtResponse {

    private String token;

    // empty constructor needed by Jackson
    public JwtResponse() {
      // noop
    }

    String getToken() {
      return token;
    }

    void setToken(String token) {
      this.token = token;
    }
  }
}
