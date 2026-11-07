package com.gamerecruitment.account.adapter.security;

import com.gamerecruitment.account.application.port.TokenIssuer;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

public class JwtTokenService implements TokenIssuer {
  private final Clock clock;
  private final JwtEncoder encoder;
  private final NimbusJwtDecoder decoder;

  public JwtTokenService(String base64, Clock clock) {
    this.clock = clock;
    byte[] key = Base64.getDecoder().decode(base64);
    if (key.length < 32)
      throw new IllegalArgumentException("JWT key must contain at least 32 random bytes");
    encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    decoder =
        NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    decoder.setJwtValidator(
        jwt ->
            valid(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")));
  }

  public Issued issue(UUID accountId) {
    Instant issued = Instant.ofEpochSecond(clock.instant().getEpochSecond());
    var details = new Details(accountId, UUID.randomUUID(), issued, issued.plusSeconds(3600));
    var claims =
        JwtClaimsSet.builder()
            .issuer("game-recruitment-board")
            .audience(List.of("game-recruitment-api"))
            .subject(accountId.toString())
            .id(details.jti().toString())
            .issuedAt(issued)
            .expiresAt(details.expiresAt())
            .build();
    String value =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new Issued(value, details);
  }

  private boolean valid(Jwt jwt) {
    try {
      var issued = jwt.getIssuedAt();
      var expiry = jwt.getExpiresAt();
      var now = clock.instant();
      UUID.fromString(jwt.getSubject());
      UUID.fromString(jwt.getId());
      return issued != null
          && expiry != null
          && "game-recruitment-board".equals(jwt.getClaimAsString("iss"))
          && jwt.getAudience().equals(List.of("game-recruitment-api"))
          && !issued.isAfter(now)
          && expiry.equals(issued.plusSeconds(3600))
          && now.isBefore(expiry)
          && (jwt.getNotBefore() == null || !now.isBefore(jwt.getNotBefore()));
    } catch (RuntimeException e) {
      return false;
    }
  }

  public Optional<Details> validate(String value) {
    if (value == null || value.isBlank()) return Optional.empty();
    try {
      var jwt = decoder.decode(value);
      return Optional.of(
          new Details(
              UUID.fromString(jwt.getSubject()),
              UUID.fromString(jwt.getId()),
              jwt.getIssuedAt(),
              jwt.getExpiresAt()));
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
