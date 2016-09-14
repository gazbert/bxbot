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

package com.gazbert.bxbot.rest.api;

import com.gazbert.bxbot.rest.security.OAuth2ServerConfiguration;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Base class for controller test classes.
 *
 * @author gazbert
 */
abstract class AbstractConfigControllerTest {

    /**
     * We'll always be sending/receiving JSON content in REST API.
     */
    static final MediaType CONTENT_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    /**
     * This must match the {@link OAuth2ServerConfiguration#OAUTH_CLIENT_ID} value.
     * TODO Mock this somehow...
     */
    private static final String OAUTH_CLIENT_ID = "bxbot-ui";

    /**
     * This must match the {@link OAuth2ServerConfiguration#OAUTH_CLIENT_SECRET} value.
     * TODO Mock this somehow...
     */
    private static final String OAUTH_CLIENT_SECRET = "S3cr3t";

    /**
     * Used to convert Java objects into JSON - roll on Java 9... ;-)
     */
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    protected WebApplicationContext ctx;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    MockMvc mockMvc;


    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {
        mappingJackson2HttpMessageConverter =
                Arrays.stream(converters)
                        .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
                        .findAny()
                        .get();

        Assert.assertNotNull("The JSON message converter must not be null",
                mappingJackson2HttpMessageConverter);
    }

    // ------------------------------------------------------------------------------------------------
    // Shared utils
    // ------------------------------------------------------------------------------------------------

    /*
     * Builds an OAuth2 access token.
     * Kudos to royclarkson - https://github.com/royclarkson/spring-rest-service-oauth
     */
    String getAccessToken(String username, String password) throws Exception {

        final String authorization = "Basic " + new String(Base64Utils.encode((OAUTH_CLIENT_ID + ":" + OAUTH_CLIENT_SECRET).getBytes()));
        final String contentType = MediaType.APPLICATION_JSON + ";charset=UTF-8";

        final String content = mockMvc.perform(post("/oauth/token").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", username)
                .param("password", password)
                .param("grant_type", "password")
                .param("scope", "read write")
                .param("client_id", OAUTH_CLIENT_ID)
                .param("client_secret", OAUTH_CLIENT_SECRET))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.access_token", is(notNullValue())))
                .andExpect(jsonPath("$.token_type", is(equalTo("bearer"))))
                .andExpect(jsonPath("$.refresh_token", is(notNullValue())))
                .andExpect(jsonPath("$.expires_in", is(greaterThan(4000))))
                .andExpect(jsonPath("$.scope", is(equalTo("read write"))))
                .andReturn().getResponse().getContentAsString();

        return content.substring(17, 53);
    }

    /*
     * Converts an object into its JSON string representation.
     */
    String jsonify(Object objectToJsonify) throws IOException {
        final MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter.write(objectToJsonify, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
