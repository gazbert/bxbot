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

import com.gazbert.bxbot.rest.api.security.authentication.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Util class for validating and accessing JSON Web Tokens.
 * 用于验证和访问 JSON Web 令牌的实用程序类。
 *
 * <p>Properties are loaded from the config/application.properties file.
 * * <p>属性从 config/application.properties 文件加载。
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen:
 * https://github.com/szerhusenBC/jwt-spring-security-demo
 * <p>代码源自 Stephan Zerhusen 的优秀 JWT 和 Spring Boot 示例：
 *  * https://github.com/szerhusenBC/jwt-spring-security-demo
 *
 * @author gazbert
 */
@Component
public class JwtUtils {

  private static final Logger LOG = LogManager.getLogger();
  private static final String CUSTOM_CLAIM_NAMESPACE = "https://gazbert.com/bxbot/";

  static final String CLAIM_KEY_LAST_PASSWORD_CHANGE_DATE =
      CUSTOM_CLAIM_NAMESPACE + "lastPasswordChangeDate";
  static final String CLAIM_KEY_ROLES = CUSTOM_CLAIM_NAMESPACE + "roles";

  private static final String CLAIM_KEY_USERNAME = "sub";
  private static final String CLAIM_KEY_ISSUER = "iss";
  private static final String CLAIM_KEY_ISSUED_AT = "iat";
  private static final String CLAIM_KEY_AUDIENCE = "aud";

  @NotNull
  @Value("${bxbot.restapi.jwt.secret}")
  private String secret;

  @NotNull
  @Value("${bxbot.restapi.jwt.expiration}")
  @Min(1)
  private long expirationInSecs;

  @NotNull
  @Value("${bxbot.restapi.jwt.allowed_clock_skew}")
  @Min(1)
  private long allowedClockSkewInSecs;

  @NotNull
  @Value("${bxbot.restapi.jwt.issuer}")
  private String issuer;

  @NotNull
  @Value("${bxbot.restapi.jwt.audience}")
  private String audience;

  /**
   * For simple validation, it is sufficient to check the token integrity by just decrypting it with
    the signing key and making sure it has not expired. We don't have to call the database for an
    additional User lookup/check for every request.
   对于简单的验证，只需使用解密它就足以检查令牌的完整性
   签名密钥并确保它没有过期。我们不必调用数据库
   额外的用户查找/检查每个请求。
   *
   * @param token the JWT in String format. 字符串格式的 JWT。
   * @return the token claims if the JWT was valid.  令牌声明 JWT 是否有效。
   * @throws JwtAuthenticationException if the JWT was invalid.  如果 JWT 无效，则出现 JwtAuthenticationException。
   */
  public Claims validateTokenAndGetClaims(String token) {
    try {
      final Claims claims = getClaimsFromToken(token);
      final Date created = getIssuedAtDateFromTokenClaims(claims);
      final Date lastPasswordResetDate = getLastPasswordResetDateFromTokenClaims(claims);
      if (!isCreatedAfterLastPasswordReset(created, lastPasswordResetDate)) {
        final String errorMsg =
            "Invalid token! Created date claim is before last password reset date. 令牌无效！创建日期声明在上次密码重置日期之前。"
                + " Created date: 创建日期："
                + created
                + " Password reset date: 密码重置日期："
                + lastPasswordResetDate;
        LOG.error(errorMsg);
        throw new JwtAuthenticationException(errorMsg);
      }
      return claims;
    } catch (Exception e) {
      final String errorMsg = "Invalid token! Details: 令牌无效！细节：" + e.getMessage();
      LOG.error(errorMsg, e);
      throw new JwtAuthenticationException(errorMsg, e);
    }
  }

  /**
   * Creates a JWT in String format.  以字符串格式创建 JWT。
   *
   * @param userDetails the JWT User details.* @param userDetails JWT 用户详细信息。
   * @return the JWT as a String. @return JWT 作为字符串。
   */
  public String createToken(JwtUser userDetails) {
    final Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_KEY_ISSUER, issuer);
    claims.put(CLAIM_KEY_ISSUED_AT, new Date());
    claims.put(CLAIM_KEY_AUDIENCE, audience);
    claims.put(CLAIM_KEY_USERNAME, userDetails.getUsername());
    claims.put(CLAIM_KEY_ROLES, mapRolesFromGrantedAuthorities(userDetails.getAuthorities()));
    claims.put(CLAIM_KEY_LAST_PASSWORD_CHANGE_DATE, userDetails.getLastPasswordResetDate());
    return buildToken(claims);
  }

  /**
   * Checks if a JWT can be refreshed.
   * 检查是否可以刷新 JWT。
   *
   * <p>The creation time of the current JWT must be AFTER than the last password reset date.
    Earlier tokens are deemed to be invalid and potentially compromised.
   <p>当前 JWT 的创建时间必须晚于上次密码重置日期。
   较早的令牌被认为是无效的并且可能受到损害。
   *
   * @param claims the JWT claims.  索赔。
   * @param lastPasswordReset the last password reset date. 上次密码重置日期。
   * @return true if the token can be refreshed, false otherwise. 如果令牌可以刷新，则为 true，否则为 false。
   */
  public boolean canTokenBeRefreshed(Claims claims, Date lastPasswordReset) {
    final Date created = getIssuedAtDateFromTokenClaims(claims);
    boolean canBeRefreshed = isCreatedAfterLastPasswordReset(created, lastPasswordReset);
    if (!canBeRefreshed) {
      LOG.warn(
          "Token cannot be refreshed for user: 无法为用户刷新令牌："
              + claims.get(CLAIM_KEY_USERNAME)
              + " - token creation date is BEFORE last password reset date. - 令牌创建日期是上次密码重置日期之前.");
    }
    return canBeRefreshed;
  }

  /**
   * Refreshes a JWT.
   * 刷新 JWT。
   *
   * @param token the token to refresh in String format.
   *              以字符串格式刷新的令牌。
   *
   * @return a new (refreshed) JWT token in String format.
   * * @return 一个新的（刷新的）字符串格式的 JWT 令牌。
   *
   * @throws JwtAuthenticationException if the token cannot be refreshed.
   * @throws JwtAuthenticationException 如果无法刷新令牌。
   */
  public String refreshToken(String token) {
    try {
      final Claims claims = getClaimsFromToken(token);
      claims.put(CLAIM_KEY_ISSUED_AT, new Date());
      return buildToken(claims);
    } catch (Exception e) {
      final String errorMsg = "Failed to refresh token!";
      LOG.error(errorMsg, e);
      throw new JwtAuthenticationException(errorMsg, e);
    }
  }

  /**
   * Extracts the username from the JWT claims.
   * 从 JWT 声明中提取用户名。
   *
   * @param claims the JWT claims. the JWT 声明.
   * @return the username. 用户名。
   */
  public String getUsernameFromTokenClaims(Claims claims) {
    try {
      final String username = claims.getSubject();
      if (username == null) {
        final String errorMsg = "Failed to extract username claim from token! 无法从令牌中提取用户名声明！";
        LOG.error(errorMsg);
        throw new JwtAuthenticationException(errorMsg);
      }
      return username;
    } catch (Exception e) {
      final String errorMsg = "Failed to extract username claim from token! 无法从令牌中提取用户名声明！";
      LOG.error(errorMsg);
      throw new JwtAuthenticationException(errorMsg, e);
    }
  }

  /**
   * Extracts the user's Roles from the JWT claims.
   * 从 JWT 声明中提取用户的角色。
   *
   * @param claims the JWT claims.
   *               JWT 声称。
   * @return the user's Roles. 用户的角色。
   * @throws JwtAuthenticationException if the user's roles cannot be extracted.
   * @throws JwtAuthenticationException 如果无法提取用户的角色。
   */
  public List<GrantedAuthority> getRolesFromTokenClaims(Claims claims) {
    final List<GrantedAuthority> roles = new ArrayList<>();
    try {
      @SuppressWarnings("unchecked")
      final List<String> rolesFromClaim = (List<String>) claims.get(CLAIM_KEY_ROLES);
      for (final String roleFromClaim : rolesFromClaim) {
        roles.add(new SimpleGrantedAuthority(roleFromClaim));
      }
      return roles;
    } catch (Exception e) {
      final String errorMsg = "Failed to extract roles claim from token! 无法从令牌中提取角色声明！";
      LOG.error(errorMsg, e);
      throw new JwtAuthenticationException(errorMsg, e);
    }
  }

  Date getIssuedAtDateFromTokenClaims(Claims claims) {
    return claims.getIssuedAt();
  }

  Date getExpirationDateFromTokenClaims(Claims claims) {
    return claims.getExpiration();
  }

  Date getLastPasswordResetDateFromTokenClaims(Claims claims) {
    Date lastPasswordResetDate;
    try {
      lastPasswordResetDate = new Date((Long) claims.get(CLAIM_KEY_LAST_PASSWORD_CHANGE_DATE));
    } catch (Exception e) {
      final String errorMsg = "Failed to extract lastPasswordResetDate claim from token! 无法从令牌中提取 lastPasswordResetDate 声明！";
      LOG.error(errorMsg, e);
      throw new JwtAuthenticationException(errorMsg, e);
    }
    return lastPasswordResetDate;
  }

  // ------------------------------------------------------------------------
  // Private utils // 私有工具
  // ------------------------------------------------------------------------

  private String buildToken(Map<String, Object> claims) {
    final Date issuedAtDate = (Date) claims.get(CLAIM_KEY_ISSUED_AT);
    final Date expirationDate = new Date(issuedAtDate.getTime() + (expirationInSecs * 1000));

    return Jwts.builder()
        .setClaims(claims)
        .setExpiration(expirationDate)
        .setIssuedAt(issuedAtDate)
        .signWith(SignatureAlgorithm.HS512, secret)
        .compact();
  }

  private Claims getClaimsFromToken(String token) {
    return Jwts.parser()
        .setAllowedClockSkewSeconds(allowedClockSkewInSecs)
        .setSigningKey(secret)
        .requireIssuer(issuer)
        .requireAudience(audience)
        .parseClaimsJws(token)
        .getBody();
  }

  private boolean isCreatedAfterLastPasswordReset(Date created, Date lastPasswordReset) {
    if (lastPasswordReset == null) {
      return true; // password not changed yet, so this is valid.  密码尚未更改，因此这是有效的。
    } else {
      return (created.after(lastPasswordReset)); // valid only if after last password change  // 只有在最后一次密码更改后才有效
    }
  }

  private List<String> mapRolesFromGrantedAuthorities(
      Collection<? extends GrantedAuthority> grantedAuthorities) {
    final List<String> roles = new ArrayList<>();
    for (final GrantedAuthority grantedAuthority : grantedAuthorities) {
      roles.add(grantedAuthority.getAuthority());
    }
    return roles;
  }
}
