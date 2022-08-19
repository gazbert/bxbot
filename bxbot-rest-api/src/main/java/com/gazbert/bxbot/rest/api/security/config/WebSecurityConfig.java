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
 * 封装应用程序的 Spring Web 安全配置。
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen:
 * https://github.com/szerhusenBC/jwt-spring-security-demo
 * <p>代码源自 Stephan Zerhusen 的优秀 JWT 和 Spring Boot 示例：
 *  * https://github.com/szerhusenBC/jwt-spring-security-demo
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
   * 必须完成才能使用 Spring Boot 2.0。
   *    * https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.0-Migration-Guide#authenticationmanager-bean
   *
   * @return the authentication manager bean.
   * * @return 身份验证管理器 bean。
   * @throws Exception if anything unexpected happens.
   * 如果发生任何意外情况，@throws 异常。
   */
  @Override
  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    return super.authenticationManagerBean();
  }

  /**
   * Creates an Authentication Manager implementation for authenticating users using Bcrypt passwords.
   * * 创建一个身份验证管理器实现，用于使用 Bcrypt 密码对用户进行身份验证。
   *
   * @param authenticationManagerBuilder the Authentication Manager.  * @param authenticationManagerBuilder 身份验证管理器。
   * @throws Exception if anything breaks building the Authentication Manager.  @throws 如果有任何东西破坏了身份验证管理器的构建，则抛出异常。
   */
  @Override
  public void configure(AuthenticationManagerBuilder authenticationManagerBuilder)
      throws Exception {
    authenticationManagerBuilder
        .userDetailsService(userDetailsService)
        .passwordEncoder(bcryptPasswordEncoder());
  }

  @Override
  protected void configure(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        // Default behaviour is to enable CSRF protection.  默认行为是启用 CSRF 保护。
        // We need to override this behaviour for our stateless (no cookies used!) REST endpoints.
            // 我们需要为我们的无状态（不使用 cookie！）REST 端点覆盖此行为。
        // https://security.stackexchange.com/questions/166724/should-i-use-csrf-protection-on-rest-api-endpoints
        // https://stackoverflow.com/questions/27390407/post-request-to-spring-server-returns-403-forbidden
        .csrf().disable()

        .exceptionHandling()
        .authenticationEntryPoint(jwtAuthenticationEntryPoint)

        // No need to create session as JWT auth is stateless / per request
            // 无需创建会话，因为 JWT auth 是无状态的/每个请求
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

        // Allow anyone to try and authenticate to get a token
            // 允许任何人尝试验证以获取令牌
        .and()
        .authorizeRequests()
        .antMatchers("/api/token")
        .permitAll()

        // Allow CORS pre-flighting for everything
            // 允许对所有内容进行 CORS 预飞行
        .antMatchers(HttpMethod.OPTIONS, "/**")
        .permitAll() // allow CORS pre-flighting
            // 允许 CORS 预飞行

        // Allow anyone access to Swagger docs
            // 允许任何人访问 Swagger 文档
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
            // 锁定其他所有内容
        .anyRequest()
        .authenticated();

    // Add our custom JWT security filter before Spring Security's Username/Password filter
    // 在 Spring Security 的用户名/密码过滤器之前添加我们自定义的 JWT 安全过滤器
    httpSecurity.addFilterBefore(
        authenticationTokenFilterBean(), UsernamePasswordAuthenticationFilter.class);

    // Disable page caching in the browser
    // 在浏览器中禁用页面缓存
    httpSecurity.headers().cacheControl().disable();
  }

  /*
   * Use bcrypt password encoding.  使用 bcrypt 密码编码。
   * https://docs.spring.io/spring-security/site/docs/5.0.5.RELEASE/reference/htmlsingle/#pe-bcpe
   */
  @Bean
  public BCryptPasswordEncoder bcryptPasswordEncoder() {
    return new BCryptPasswordEncoder(12); // tuned to 1 sec; default is 10 rounds.   调到 1 秒；默认为 10 轮。
  }

  @Bean
  public JwtAuthenticationFilter authenticationTokenFilterBean() {
    return new JwtAuthenticationFilter();
  }
}
