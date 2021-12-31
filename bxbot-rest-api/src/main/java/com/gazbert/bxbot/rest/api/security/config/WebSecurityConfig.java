/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Stephan Zerhusen
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

package com.gazbert.bxbot.rest.api.security.config;

import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationEntryPoint;
import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * Encapsulates the Spring web security config for the app.
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen:
 * https://github.com/szerhusenBC/jwt-spring-security-demo
 *
 * @author gazbert
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final UserDetailsService userDetailsService;

  @Autowired
  public WebSecurityConfig(
      CorsFilter corsFilter,
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      UserDetailsService userDetailsService) {
    this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Must be done to work with Spring Boot 2.0.
   * https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Migration-Guide#authenticationmanager-bean
   *
   * @return the authentication manager bean.
   * @throws Exception id anything unexpected happens.
   */
  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  /**
   * Creates an Authentication Manager implementation for authenticating users using Bcrypt
   * passwords.
   *
   * @param authenticationManagerBuilder the Authentication Manager.
   * @throws Exception if anything breaks building the Authentication Manager.
   */
  @Autowired
  public void configureAuthentication(AuthenticationManagerBuilder authenticationManagerBuilder)
      throws Exception {
    authenticationManagerBuilder
        .userDetailsService(this.userDetailsService)
        .passwordEncoder(bcryptPasswordEncoder());
  }

  /*
   * Use bcrypt password encoding.
   * https://docs.spring.io/spring-security/site/docs/5.0.5.RELEASE/reference/htmlsingle/#pe-bcpe
   */
  @Bean
  public BCryptPasswordEncoder bcryptPasswordEncoder() {
    return new BCryptPasswordEncoder(12); // tuned to 1 sec; default is 10 rounds.
  }

  @Bean
  public JwtAuthenticationFilter authenticationTokenFilterBean() {
    return new JwtAuthenticationFilter();
  }

  @Override
  protected void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        // Default behaviour is to enable CSRF protection.
        // We need to override this behaviour for our stateless (no cookies used!) REST endpoints.
        // https://security.stackexchange.com/questions/166724/should-i-use-csrf-protection-on-rest-api-endpoints
        // https://stackoverflow.com/questions/27390407/post-request-to-spring-server-returns-403-forbidden
        .csrf().disable()

        .exceptionHandling()
        .authenticationEntryPoint(jwtAuthenticationEntryPoint)

        // No need to create session as JWT auth is stateless / per request
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        // Allow anyone to try and authenticate to get a token
        .and()
        .authorizeRequests()
        .antMatchers("/api/token")
        .permitAll()

        // Allow CORS pre-flighting for everything
        .antMatchers(HttpMethod.OPTIONS, "/**")
        .permitAll() // allow CORS pre-flighting

        // Allow anyone access to Swagger docs
        .antMatchers(
            HttpMethod.GET,
            "/api-docs",
            "/swagger-resources/**",
            "/swagger-ui.html**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/webjars/**",
            "/favicon.ico")
        .permitAll()

        // Lock down everything else
        .anyRequest()
        .authenticated();

    // Add our custom JWT security filter before Spring Security's Username/Password filter
    httpSecurity.addFilterBefore(
        authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);

    // Disable page caching in the browser
    httpSecurity.headers().cacheControl().disable();
  }
}
