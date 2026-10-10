package com.gamerecruitment.account.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerecruitment.account.ApiTestSupport;
import com.gamerecruitment.account.adapter.persistence.RevokedTokenCleanup;
import com.gamerecruitment.account.adapter.security.*;
import com.gamerecruitment.account.application.port.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class LogoutRevocationTest extends ApiTestSupport {
  @Test
  void logoutRevokesOnlyTheCurrentLoginAndPersists() throws Exception {
    register("player_a");
    var a = login("player_a");
    var b = login("player_a");
    send("/api/auth/logout", null, csrf(), a).andExpect(status().isNoContent());
    mvc.perform(get("/api/auth/me").cookie(a)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/auth/me").cookie(b)).andExpect(status().isOk());
    send("/api/auth/logout", null, csrf(), a).andExpect(status().isNoContent());
    assertEquals(1, jdbc.queryForObject("select count(*) from revoked_tokens", Integer.class));
    var fresh =
        new CurrentAccountAuthenticator(
            context.getBean(TokenIssuer.class),
            new com.gamerecruitment.account.adapter.persistence.JdbcRevokedTokenStore(
                org.springframework.jdbc.core.simple.JdbcClient.create(
                    context.getBean(javax.sql.DataSource.class))),
            context.getBean(AccountRepository.class));
    assertTrue(fresh.optional(a.getValue()).isEmpty());
    assertTrue(fresh.optional(b.getValue()).isPresent());
  }

  @Test
  void exactExpiryAndCleanupDoNotReviveToken() throws Exception {
    register("player_a");
    var token = login("player_a");
    clock.at(Instant.parse("2026-10-10T01:59:59Z"));
    mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isOk());
    clock.at(Instant.parse("2026-10-10T02:00:00Z"));
    mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isUnauthorized());
    send("/api/auth/logout", null, csrf(), token).andExpect(status().isNoContent());
    context.getBean(RevokedTokenCleanup.class).clean();
    mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isUnauthorized());
  }

  @Test
  void cleanupDeletesOnlyExpiredRevocationsWithoutRevivingTokens() throws Exception {
    register("player_a");
    var expired = login("player_a");
    send("/api/auth/logout", null, csrf(), expired).andExpect(status().isNoContent());
    clock.at(Instant.parse("2026-10-10T01:30:00Z"));
    var liveRevoked = login("player_a");
    send("/api/auth/logout", null, csrf(), liveRevoked).andExpect(status().isNoContent());
    var issuer = context.getBean(TokenIssuer.class);
    var expiredId = issuer.validate(expired.getValue()).orElseThrow().jti();
    var liveId = issuer.validate(liveRevoked.getValue()).orElseThrow().jti();
    assertEquals(2, jdbc.queryForObject("select count(*) from revoked_tokens", Integer.class));
    clock.at(Instant.parse("2026-10-10T01:59:59Z"));
    context.getBean(RevokedTokenCleanup.class).clean();
    assertEquals(2, jdbc.queryForObject("select count(*) from revoked_tokens", Integer.class));
    clock.at(Instant.parse("2026-10-10T02:00:00Z"));
    context.getBean(RevokedTokenCleanup.class).clean();
    assertEquals(1, jdbc.queryForObject("select count(*) from revoked_tokens", Integer.class));
    assertEquals(
        liveId, jdbc.queryForObject("select jti from revoked_tokens", java.util.UUID.class));
    assertFalse(context.getBean(RevokedTokenStore.class).isRevoked(expiredId));
    assertTrue(context.getBean(RevokedTokenStore.class).isRevoked(liveId));
    mvc.perform(get("/api/auth/me").cookie(expired)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/auth/me").cookie(liveRevoked)).andExpect(status().isUnauthorized());
  }
}
