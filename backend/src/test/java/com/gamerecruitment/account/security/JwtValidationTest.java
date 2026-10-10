package com.gamerecruitment.account.security;

import static org.junit.jupiter.api.Assertions.*;

import com.gamerecruitment.account.adapter.security.JwtTokenService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class JwtValidationTest {
  private static final String KEY = "dGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXktdGVzdC1rZXk=";

  @Test
  void signatureAndExactExpiry() {
    var now = Instant.parse("2026-10-10T01:00:00Z");
    var service = new JwtTokenService(KEY, Clock.fixed(now, ZoneOffset.UTC));
    var id = UUID.randomUUID();
    var token = service.issue(id);
    assertEquals(id, service.validate(token.value()).orElseThrow().accountId());
    assertTrue(service.validate(token.value() + "a").isEmpty());
    assertTrue(
        new JwtTokenService(KEY, Clock.fixed(now.plusSeconds(3599), ZoneOffset.UTC))
            .validate(token.value())
            .isPresent());
    assertTrue(
        new JwtTokenService(KEY, Clock.fixed(now.plusSeconds(3600), ZoneOffset.UTC))
            .validate(token.value())
            .isEmpty());
    assertTrue(
        new JwtTokenService(KEY, Clock.fixed(now.minusSeconds(1), ZoneOffset.UTC))
            .validate(token.value())
            .isEmpty());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new JwtTokenService(
                Base64.getEncoder().encodeToString(new byte[16]), Clock.systemUTC()));
  }

  @Test
  void requiredClaimsAndTypesCannotBeForgedEvenWithValidSignature() throws Exception {
    var now = Instant.parse("2026-10-10T01:00:00Z");
    var service = new JwtTokenService(KEY, Clock.fixed(now, ZoneOffset.UTC));
    var encoder =
        new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(
            new com.nimbusds.jose.jwk.source.ImmutableSecret<>(Base64.getDecoder().decode(KEY)));
    var baseline = new HashMap<String, Object>();
    baseline.put("iss", "game-recruitment-board");
    baseline.put("aud", List.of("game-recruitment-api"));
    baseline.put("sub", UUID.randomUUID().toString());
    baseline.put("jti", UUID.randomUUID().toString());
    baseline.put("iat", now);
    baseline.put("exp", now.plusSeconds(3600));
    var changes = new ArrayList<Map<String, Object>>();
    for (String key : baseline.keySet()) {
      var copy = new HashMap<>(baseline);
      copy.remove(key);
      changes.add(copy);
    }
    for (var change :
        List.of(
            Map.<String, Object>of("iss", "other"),
            Map.<String, Object>of("aud", List.of("other")),
            Map.<String, Object>of("sub", "bad"),
            Map.<String, Object>of("jti", "bad"),
            Map.<String, Object>of("exp", now.plusSeconds(3601)),
            Map.<String, Object>of("nbf", now.plusSeconds(1)),
            Map.<String, Object>of("iat", "not-a-time"))) {
      var copy = new HashMap<>(baseline);
      copy.putAll(change);
      changes.add(copy);
    }
    for (var claims : changes) {
      var raw = new com.nimbusds.jwt.JWTClaimsSet.Builder();
      claims.forEach((k, v) -> raw.claim(k, v instanceof Instant t ? Date.from(t) : v));
      var signed =
          new com.nimbusds.jwt.SignedJWT(
              new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), raw.build());
      signed.sign(new com.nimbusds.jose.crypto.MACSigner(Base64.getDecoder().decode(KEY)));
      String token = signed.serialize();
      assertTrue(service.validate(token).isEmpty());
    }
  }
}
