package com.gamerecruitment.account.security;

import static org.junit.jupiter.api.Assertions.*;

import com.gamerecruitment.account.adapter.security.AuthCookieWriter;
import com.gamerecruitment.configuration.CookieEnvironmentGuard;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieEnvironmentTest {
  @Test
  void nonSecureCookiesAreLoopbackOnly() {
    var env = new MockEnvironment().withProperty("app.secure-cookies", "false");
    assertThrows(IllegalStateException.class, () -> new CookieEnvironmentGuard(env));
    env.withProperty("server.address", "0.0.0.0");
    assertThrows(IllegalStateException.class, () -> new CookieEnvironmentGuard(env));
    env.withProperty("server.address", "127.0.0.1");
    assertDoesNotThrow(() -> new CookieEnvironmentGuard(env));
  }

  @Test
  void normalCookiesAreSecureHttpOnlyAndCanBeDeleted() {
    var writer = new AuthCookieWriter(true);
    var response = new MockHttpServletResponse();
    writer.set(response, "test-token");
    assertTrue(response.getCookie("auth_token").getSecure());
    assertTrue(response.getCookie("auth_token").isHttpOnly());
    assertEquals("/api", response.getCookie("auth_token").getPath());
    writer.clear(response);
    assertTrue(
        response.getHeaders("Set-Cookie").stream()
            .anyMatch(
                h -> h.contains("Max-Age=0") && h.contains("Secure") && h.contains("HttpOnly")));
  }
}
