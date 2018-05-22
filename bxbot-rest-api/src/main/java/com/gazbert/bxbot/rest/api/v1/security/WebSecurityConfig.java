/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Gareth Jon Lynch
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

package com.gazbert.bxbot.rest.api.v1.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

/**
 * Web security config for protecting the REST API.
 *
 * WARNING: This is not safe for Production yet!
 *
 * @author gazbert
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        // TODO - get from application.properties file and bcrypt em!
        auth.inMemoryAuthentication()
                .withUser("unit-test-user").password("unit-test-password")
                .authorities("ROLE_USER");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/nothingWillBeUnsecured").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();

        // Default behaviour is to enable CSRF protection.
        // Need to override this behaviour for our stateless (no cookies used!) REST endpoints.
        // https://security.stackexchange.com/questions/166724/should-i-use-csrf-protection-on-rest-api-endpoints
        // https://stackoverflow.com/questions/27390407/post-request-to-spring-server-returns-403-forbidden
        http.csrf().disable();
    }

    // https://docs.spring.io/spring-security/site/docs/5.0.5.RELEASE/reference/htmlsingle/#troubleshooting
    // Tmp patch for backwards compatibility pre Spring 5
    @SuppressWarnings("deprecation")
    @Bean
    public static NoOpPasswordEncoder passwordEncoder() {
        return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
    }

    // TODO - https://docs.spring.io/spring-security/site/docs/5.0.5.RELEASE/reference/htmlsingle/#pe-bcpe
//    @Bean
//    public static BCryptPasswordEncoder bCryptPasswordEncoder() {
//        return new BCryptPasswordEncoder(16);
//    }
}