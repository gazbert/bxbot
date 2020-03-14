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
 *
 * <p>Properties are loaded from the config/application.properties file.
 *
 * <p>Code originated from the excellent JWT and Spring Boot example by Stephan Zerhusen:
 * https://github.com/szerhusenBC/jwt-spring-security-demo
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
   * the signing key and making sure it has not expired. We don't have to call the database for an
   * additional User lookup/check for every request.
   *
   * @param token the JWT in String format.
   * @return the token claims if the JWT was valid.
   * @throws JwtAuthenticationException if the JWT was invalid.
   */
  public Claims validateTokenAndGetClaims(String token) {
    try {
      final Claims claims = getClaimsFromToken(token);
      final Date created = getIssuedAtDateFromTokenClaims(claims);
      final Date lastPasswordResetDate = getLastPasswordResetDateFromTokenClaims(claims);
      if (!isCreatedAfterLastPasswordReset(created, lastPasswordResetDate)) {
        final String errorMsg =
            "Invalid token! Created date claim is before last password reset date."
                + " Created date: "
                + created
                + " Password reset date: "
                + lastPasswordResetDate;
        LOG.error(errorMsg);
        throw new JwtAuthenticationException(errorMsg);
      }
      return claims;
    } catch (Exception e) {
      final String errorMsg = "Invalid token! Details: " + e.getMessage();
      LOG.error(errorMsg, e);
      throw new JwtAuthenticationException(errorMsg, e);
    }
  }

  /**
   * Creates a JWT in String format.
   *
   * @param userDetails the JWT User details.
   * @return the JWT as a String.
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
   *
   * <p>The creation time of the current JWT must be AFTER than the last password reset date.
   * Earlier tokens are deemed to be invalid and potentially compromised.
   *
   * @param claims the JWT claims.
   * @param lastPasswordReset the last password reset date.
   * @return true if the token can be refreshed, false otherwise.
   */
  public boolean canTokenBeRefreshed(Claims claims, Date lastPasswordReset) {
    final Date created = getIssuedAtDateFromTokenClaims(claims);
    boolean canBeRefreshed = isCreatedAfterLastPasswordReset(created, lastPasswordReset);
    if (!canBeRefreshed) {
      LOG.warn(
          "Token cannot be refreshed for user: "
              + claims.get(CLAIM_KEY_USERNAME)
              + " - token creation date is BEFORE last password reset date");
    }
    return canBeRefreshed;
  }

  /**
   * Refreshes a JWT.
   *
   * @param token the token to refresh in String format.
   * @return a new (refreshed) JWT token in String format.
   * @throws JwtAuthenticationException if the token cannot be refreshed.
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
   *
   * @param claims the JWT claims.
   * @return the username.
   */
  public String getUsernameFromTokenClaims(Claims claims) {
    try {
      final String username = claims.getSubject();
      if (username == null) {
        final String errorMsg = "Failed to extract username claim from token!";
        LOG.error(errorMsg);
        throw new JwtAuthenticationException(errorMsg);
      }
      return username;
    } catch (Exception e) {
      final String errorMsg = "Failed to extract username claim from token!";
      LOG.error(errorMsg);
      throw new JwtAuthenticationException(errorMsg, e);
    }
  }

  /**
   * Extracts the user's Roles from the JWT claims.
   *
   * @param claims the JWT claims.
   * @return the user's Roles.
   * @throws JwtAuthenticationException if the user's roles cannot be extracted.
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
      final String errorMsg = "Failed to extract roles claim from token!";
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
      final String errorMsg = "Failed to extract lastPasswordResetDate claim from token!";
      LOG.error(errorMsg, e);
      throw new JwtAuthenticationException(errorMsg, e);
    }
    return lastPasswordResetDate;
  }

  // ------------------------------------------------------------------------
  // Private utils
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
      return true; // password not changed yet, so this is valid.
    } else {
      return (created.after(lastPasswordReset)); // valid only if after last password change
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
