package com.gamerecruitment.account.contract;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.gamerecruitment.account.ApiTestSupport;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthenticationContractTest extends ApiTestSupport {
  @Test
  void loginAndCookieRules() throws Exception {
    register("player_a");
    var token = login("PLAYER_A");
    assertTrue(token.isHttpOnly());
    assertEquals("/api", token.getPath());
    assertEquals(3600, token.getMaxAge());
    assertFalse(token.getSecure());
    assertEquals("Lax", token.getAttribute("SameSite"));
    assertNull(token.getDomain());
    mvc.perform(get("/api/auth/me").cookie(token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.account.displayName").value("もち"))
        .andExpect(jsonPath("$.expiresAt").value("2026-10-10T02:00:00Z"));
    send(
            "/api/auth/login",
            Map.of("loginId", "player_a", "password", "test-passphrase-12345"),
            csrf(),
            token)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ALREADY_AUTHENTICATED"));
    var out =
        send("/api/auth/logout", null, csrf(), token)
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse();
    assertEquals("", out.getContentAsString());
    assertEquals(0, out.getCookie("auth_token").getMaxAge());
    assertEquals(0, out.getCookie("XSRF-TOKEN").getMaxAge());
    mvc.perform(get("/api/auth/me").cookie(token)).andExpect(status().isUnauthorized());
  }

  @Test
  void badCredentialsAndOldCookies() throws Exception {
    register("player_a");
    for (String id : new String[] {"player_a", "unknown_a"})
      send("/api/auth/login", Map.of("loginId", id, "password", "wrong-passphrase-12345"), csrf())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    var bad = new Cookie("auth_token", "invalid");
    mvc.perform(get("/api/auth/csrf").cookie(bad)).andExpect(status().isOk());
    send(
            "/api/auth/login",
            Map.of("loginId", "player_a", "password", "test-passphrase-12345"),
            csrf(),
            bad)
        .andExpect(status().isOk());
    send("/api/auth/logout", null, csrf(), bad).andExpect(status().isNoContent());
    send("/api/auth/logout", null, csrf()).andExpect(status().isNoContent());
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer ignored"))
        .andExpect(status().isUnauthorized());
  }
}
