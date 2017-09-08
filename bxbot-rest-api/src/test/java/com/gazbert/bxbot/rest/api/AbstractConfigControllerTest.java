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

/**
 * Base class for Controller test classes.
 *
 * @author gazbert
 */
public abstract class AbstractConfigControllerTest {

    /**
     * This must match security.user.name in the src/test/resources/application.properties file.
     */
    protected static final String VALID_USER_LOGINID = "unit-test-user";

    /**
     * This must match a security.user.password in the src/test/resources/application.properties file.
     */
    protected static final String VALID_USER_PASSWORD = "unit-test-password";

    /**
     * Used for bad credentials tests.
     */
    protected static final String INVALID_USER_PASSWORD = "not-valid-password";

    /**
     * We'll always be sending/receiving JSON content in REST API.
     */
    protected static final MediaType CONTENT_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

    /**
     * Used to convert Java objects into JSON - roll on Java 9... ;-)
     */
    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    protected WebApplicationContext ctx;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    protected MockMvc mockMvc;


    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {
        mappingJackson2HttpMessageConverter =
                Arrays.stream(converters)
                        .filter(converter -> converter instanceof MappingJackson2HttpMessageConverter)
                        .findAny()
                        .orElse(null);

        Assert.assertNotNull("The JSON message converter must not be null",
                mappingJackson2HttpMessageConverter);
    }

    // ------------------------------------------------------------------------------------------------
    // Shared utils
    // ------------------------------------------------------------------------------------------------

    protected String buildAuthorizationHeaderValue(String username, String password) throws Exception {
        return "Basic " + new String(Base64Utils.encode(
                (username + ":" + password).getBytes("UTF-8")), Charset.forName("UTF-8"));
    }

    /*
     * Converts an object into its JSON string representation.
     */
    protected String jsonify(Object objectToJsonify) throws IOException {
        final MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter.write(objectToJsonify, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
