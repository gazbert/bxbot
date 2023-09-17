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

import static org.springframework.security.config.Customizer.withDefaults;

import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationEntryPoint;
import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Encapsulates the Spring web security config for the app.
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by <a
 * href="https://github.com/szerhusenBC/jwt-spring-security-demo">Stephan Zerhusen</a>.
 *
 * <p>It has had significant rework for Spring Security 6 and prep for 7.
 *
 * @author gazbert
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

  private static final int BCRYPT_STRENGTH_12_ROUNDS = 12;

  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final UserDetailsService userDetailsService;

  /**
   * Creates the WebSecurityConfig.
   *
   * @param jwtAuthenticationEntryPoint the JWT authentication entry point.
   * @param userDetailsService the user details service.
   */
  @Autowired
  public WebSecurityConfig(
      JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
      UserDetailsService userDetailsService) {
    this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Creates the Security Filter Chain for the app.
   *
   * @param http the HTTP Security builder.
   * @return the Security Filter chain.
   * @throws Exception if anything unexpected happens.
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    // By default, Spring Security uses a Bean by the name of corsConfigurationSource defined in our
    // RestCorsConfig class.
    http.cors(withDefaults());

    // Restrict access to the REST API.
    http.authorizeHttpRequests(
        auth ->
            // Allow anyone to try and authenticate to get a JWT.
            auth.requestMatchers(HttpMethod.POST, "/api/token")
                .permitAll()

                // Allow anyone to access the Swagger docs.
                .requestMatchers(
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

                // Allow CORS pre-flighting.
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()

                // All other requests MUST be authenticated.
                .anyRequest()
                .authenticated());

    // Set our JWT authentication entry point.
    http.exceptionHandling(
        exceptionHandler -> exceptionHandler.authenticationEntryPoint(jwtAuthenticationEntryPoint));

    // Default behaviour is to enable CSRF protection.
    // We need to override this behaviour for our stateless (no cookies used!) REST endpoints.
    // https://security.stackexchange.com/questions/166724/should-i-use-csrf-protection-on-rest-api-endpoints
    // https://stackoverflow.com/questions/27390407/post-request-to-spring-server-returns-403-forbidden
    http.csrf(AbstractHttpConfigurer::disable);

    // No need to create session as JWT auth is stateless / per request.
    http.sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    // Add our custom JWT security filter before Spring Security's Username/Password filter.
    http.addFilterBefore(authenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    // Disable page caching in the browser.
    http.headers(headers -> headers.cacheControl(HeadersConfigurer.CacheControlConfig::disable));

    return http.build();
  }

  /**
   * Returns the default Authentication Manager.
   *
   * @return the authentication manager bean.
   * @throws Exception if anything unexpected happens.
   */
  @Bean
  AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  /**
   * Creates an Authentication Manager implementation for authenticating users using Bcrypt
   * passwords.
   *
   * @param authenticationManagerBuilder the Authentication Manager builder.
   * @throws Exception if anything breaks building the Authentication Manager.
   */
  @Autowired
  void configure(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
    authenticationManagerBuilder
        .userDetailsService(userDetailsService)
        .passwordEncoder(new BCryptPasswordEncoder());
  }

  /**
   * Use bcrypt password encoding. See: <a
   * href="https://docs.spring.io/spring-security/site/docs/5.0.5.RELEASE/reference/htmlsingle/#pe-bcpe">BCrypt
   * password encoder</a>.
   *
   * @return The BCrypt password encoder.
   */
  @Bean
  public BCryptPasswordEncoder bcryptPasswordEncoder() {
    return new BCryptPasswordEncoder(
        BCRYPT_STRENGTH_12_ROUNDS); // tuned to 1 sec; default is 10 rounds.
  }

  /**
   * Creates our legacy custom JWT auth filter. At some point this should be replaced with the JWT
   * filter that now comes bundled with Spring Security.
   *
   * @return the JWT Auth Filter.
   */
  @Bean
  public JwtAuthenticationFilter authenticationTokenFilter() {
    return new JwtAuthenticationFilter();
  }
}
