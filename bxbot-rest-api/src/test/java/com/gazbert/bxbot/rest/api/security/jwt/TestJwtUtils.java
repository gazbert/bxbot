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

package com.gazbert.bxbot.rest.api.security.jwt;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationException;
import com.gazbert.bxbot.rest.api.security.model.Role;
import com.gazbert.bxbot.rest.api.security.model.RoleName;
import com.gazbert.bxbot.rest.api.security.model.User;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.assertj.core.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests the JWT utils.
 * * 测试 JWT 工具。
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen:
  https://github.com/szerhusenBC/jwt-spring-security-demo
 *<p>代码源自 Stephan Zerhusen 的优秀 JWT 和 Spring Boot 示例：
 *   https://github.com/szerhusenBC/jwt-spring-security-demo
 * @author gazbert
 */
@ExtendWith(SpringExtension.class)
class TestJwtUtils {

  private static final long GRADLE_FRIENDLY_TIME_TOLERANCE_IN_MILLIS = 10000L;

  private static final String SECRET_KEY = "mkultra";
  private static final long EXPIRATION_PERIOD = 3600L;
  private static final long ALLOWED_CLOCK_SKEW_IN_SECS = 5 * 60 + 1000; // 5 mins
  private static final String ISSUER = "Rey";
  private static final String AUDIENCE = "R2-D2";
  private static final Date ISSUED_AT_DATE = new Date();
  private static final Date EXPIRATION_DATE =
      new Date(ISSUED_AT_DATE.getTime() + (EXPIRATION_PERIOD * 1000));
  private static final Long USER_ROLE_ID = Long.valueOf("21344565442342");
  private static final Long USER_ID = Long.valueOf("2323267789789");
  private static final String USERNAME = "hansolo";
  private static final String PASSWORD = "password";
  private static final String FIRSTNAME = "Han";
  private static final String LASTNAME = "Solo";
  private static final String EMAIL = "han@falcon";
  private static final boolean USER_ENABLED = true;
  private static final Date LAST_PASSWORD_RESET_DATE_YESTERDAY = DateUtil.yesterday();
  private static final List<String> ROLES = Arrays.asList("ROLE_ADMIN", "ROLE_USER");

  @InjectMocks private JwtUtils jwtUtils;
  @MockBean private Claims claims;

  /** Setup for all tests.
   * 设置所有测试。 */
  @BeforeEach
  void init() {
    MockitoAnnotations.initMocks(this);
    ReflectionTestUtils.setField(jwtUtils, "expirationInSecs", EXPIRATION_PERIOD);
    ReflectionTestUtils.setField(jwtUtils, "secret", SECRET_KEY);
    ReflectionTestUtils.setField(jwtUtils, "allowedClockSkewInSecs", ALLOWED_CLOCK_SKEW_IN_SECS);
    ReflectionTestUtils.setField(jwtUtils, "issuer", ISSUER);
    ReflectionTestUtils.setField(jwtUtils, "audience", AUDIENCE);
  }

  // ------------------------------------------------------------------------
  // JWT Claims tests
  // JWT 声明测试
  // ------------------------------------------------------------------------

  @Test
  void testUsernameCanBeExtractedFromTokenClaims() {
    when(claims.getSubject()).thenReturn(USERNAME);
    assertThat(jwtUtils.getUsernameFromTokenClaims(claims)).isEqualTo(USERNAME);
    verify(claims, times(1)).getSubject();
  }

  @Test
  void testExceptionThrownIfUsernameCannotBeExtractedFromTokenClaims() {
    when(claims.getSubject()).thenReturn(null);
    assertThrows(
        JwtAuthenticationException.class, () -> jwtUtils.getUsernameFromTokenClaims(claims));
    verify(claims, times(1)).getSubject();
  }

  @Test
  void testIssuedAtDateCanBeExtractedFromTokenClaims() {
    when(claims.getIssuedAt()).thenReturn(ISSUED_AT_DATE);
    assertThat(jwtUtils.getIssuedAtDateFromTokenClaims(claims))
        .isCloseTo(ISSUED_AT_DATE, GRADLE_FRIENDLY_TIME_TOLERANCE_IN_MILLIS);
    verify(claims, times(1)).getIssuedAt();
  }

  @Test
  void testExpirationDateCanBeExtractedFromTokenClaims() {
    when(claims.getExpiration()).thenReturn(EXPIRATION_DATE);
    assertThat(jwtUtils.getExpirationDateFromTokenClaims(claims))
        .isCloseTo(EXPIRATION_DATE, GRADLE_FRIENDLY_TIME_TOLERANCE_IN_MILLIS);
    verify(claims, times(1)).getExpiration();
  }

  @Test
  void testRolesCanBeExtractedFromTokenClaims() {
    when(claims.get(JwtUtils.CLAIM_KEY_ROLES)).thenReturn(ROLES);
    final List<GrantedAuthority> roles = jwtUtils.getRolesFromTokenClaims(claims);
    assertThat(roles.size()).isEqualTo(2);
    assertThat(roles.get(0).getAuthority()).isEqualTo(RoleName.ROLE_ADMIN.name());
    assertThat(roles.get(1).getAuthority()).isEqualTo(RoleName.ROLE_USER.name());
    verify(claims, times(1)).get(JwtUtils.CLAIM_KEY_ROLES);
  }

  @Test
  void testExceptionThrownIfRolesCannotBeExtractedFromTokenClaims() {
    when(claims.get(JwtUtils.CLAIM_KEY_ROLES)).thenReturn(null);
    assertThrows(JwtAuthenticationException.class, () -> jwtUtils.getRolesFromTokenClaims(claims));
    verify(claims, times(1)).get(JwtUtils.CLAIM_KEY_ROLES);
  }

  @Test
  void testLastPasswordResetDateCanBeExtractedFromTokenClaims() {
    when(claims.get(JwtUtils.CLAIM_KEY_LAST_PASSWORD_CHANGE_DATE))
        .thenReturn(LAST_PASSWORD_RESET_DATE_YESTERDAY.getTime());
    assertThat(jwtUtils.getLastPasswordResetDateFromTokenClaims(claims))
        .isCloseTo(LAST_PASSWORD_RESET_DATE_YESTERDAY, GRADLE_FRIENDLY_TIME_TOLERANCE_IN_MILLIS);
    verify(claims, times(1)).get(JwtUtils.CLAIM_KEY_LAST_PASSWORD_CHANGE_DATE);
  }

  @Test
  void testExceptionThrownIfLastPasswordResetDateCannotBeExtractedFromTokenClaims() {
    when(claims.get(JwtUtils.CLAIM_KEY_LAST_PASSWORD_CHANGE_DATE)).thenReturn(null);
    assertThrows(
        JwtAuthenticationException.class,
        () -> jwtUtils.getLastPasswordResetDateFromTokenClaims(claims));

    verify(claims, times(1)).get(JwtUtils.CLAIM_KEY_LAST_PASSWORD_CHANGE_DATE);
  }

  // ------------------------------------------------------------------------
  // JWT Validation tests
  // JWT 验证测试
  // ------------------------------------------------------------------------

  @Test
  void whenValidateTokenCalledWithNonExpiredTokenThenExpectSuccess() {
    final String token = createTokenWithLastPasswordResetYesterday();
    assertThat(jwtUtils.validateTokenAndGetClaims(token)).isNotNull();
  }

  @Test
  void whenValidateTokenCalledWithExpiredTokenThenExpectFailure() {
    ReflectionTestUtils.setField(jwtUtils, "allowedClockSkewInSecs", 0L);
    ReflectionTestUtils.setField(jwtUtils, "expirationInSecs", 0L); // will expire fast!  // 将过渡！
    final String token = createTokenWithLastPasswordResetYesterday();
    assertThrows(JwtAuthenticationException.class, () -> jwtUtils.validateTokenAndGetClaims(token));
  }

  @Test
  void whenValidateTokenCalledWithCreatedDateEarlierThanLastPasswordResetDateThenExpectFailure() {
    final String token = createTokenWithInvalidCreationDate();
    assertThrows(JwtAuthenticationException.class, () -> jwtUtils.validateTokenAndGetClaims(token));
  }

  // ------------------------------------------------------------------------
  // JWT Refresh tests
  // JWT 刷新测试
  // ------------------------------------------------------------------------

  @Test
  void whenRefreshTokenCalledWithValidTokenThenExpectNewTokenToBeReturned() {
    final String token = createTokenWithLastPasswordResetYesterday();
    final Claims tokenClaims = jwtUtils.validateTokenAndGetClaims(token);

    final String refreshToken = jwtUtils.refreshToken(token);
    final Claims refreshTokenClaims = jwtUtils.validateTokenAndGetClaims(refreshToken);

    assertThat(refreshTokenClaims.getIssuedAt()).isAfterOrEqualsTo(tokenClaims.getIssuedAt());
  }

  @Test
  void whenCanTokenBeRefreshedCalledWithStaleTokenThenExpectReturnFalse() {
    final String token = createTokenWithLastPasswordResetYesterday();
    final Claims tokenClaims = jwtUtils.validateTokenAndGetClaims(token);
    final Date lastPasswordResetDate = new Date();

    // Invalid as password changed after token was created.
    // 无效，因为创建令牌后更改了密码。
    assertThat(jwtUtils.canTokenBeRefreshed(tokenClaims, lastPasswordResetDate)).isFalse();
  }

  @Test
  void whenCanTokenBeRefreshedCalledWhenPasswordNotChangedYetThenExpectReturnTrue() {
    final String token = createTokenWithLastPasswordResetYesterday();
    final Claims tokenClaims = jwtUtils.validateTokenAndGetClaims(token);

    // Valid as password has not been changed yet
    // 有效，因为密码尚未更改
    assertThat(jwtUtils.canTokenBeRefreshed(tokenClaims, null)).isTrue();
  }

  @Test
  void whenCanTokenBeRefreshedCalledWithValidTokenThenExpectReturnFalse() {
    final String token = createTokenWithLastPasswordResetYesterday();
    final Claims tokenClaims = jwtUtils.validateTokenAndGetClaims(token);
    await().atLeast(Duration.ofSeconds(1));

    // Valid as token created after password last changed
    // 与上次更改密码后创建的令牌一样有效
    final boolean canBeRefreshed = jwtUtils.canTokenBeRefreshed(tokenClaims, DateUtil.yesterday());
    assertThat(canBeRefreshed).isEqualTo(true);
  }

  // ------------------------------------------------------------------------
  // Util methods
  // 实用方法
  // ------------------------------------------------------------------------

  private String createTokenWithLastPasswordResetYesterday() {
    return jwtUtils.createToken(createJwtUser(LAST_PASSWORD_RESET_DATE_YESTERDAY));
  }

  private String createTokenWithInvalidCreationDate() {
    return jwtUtils.createToken(createJwtUser(DateUtil.tomorrow()));
  }

  private JwtUser createJwtUser(Date lastPasswordResetDate) {
    final User user = createUser(lastPasswordResetDate);
    return JwtUserFactory.create(user);
  }

  private User createUser(Date lastPasswordResetDate) {
    final User user = new User();
    user.setId(USER_ID);
    user.setUsername(USERNAME);
    user.setPassword(PASSWORD);
    user.setFirstname(FIRSTNAME);
    user.setLastname(LASTNAME);
    user.setEmail(EMAIL);
    user.setEnabled(USER_ENABLED);
    user.setLastPasswordResetDate(lastPasswordResetDate);

    final List<Role> roles = createRoles(user);
    user.setRoles(roles);

    return user;
  }

  private List<Role> createRoles(User user) {
    final List<User> users = Collections.singletonList(user);

    final Role role1 = new Role();
    role1.setId(USER_ROLE_ID);
    role1.setName(RoleName.ROLE_USER);
    role1.setUsers(users);

    final List<Role> roles = new ArrayList<>();
    roles.add(role1);
    return roles;
  }
}
