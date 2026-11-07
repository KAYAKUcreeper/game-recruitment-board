package com.gamerecruitment.account.adapter.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieWriter {
  private final boolean secure;

  public AuthCookieWriter(@Value("${app.secure-cookies:true}") boolean secure) {
    this.secure = secure;
  }

  public void set(HttpServletResponse response, String token) {
    write(response, token, 3600);
  }

  public void clear(HttpServletResponse response) {
    write(response, "", 0);
  }

  private void write(HttpServletResponse response, String value, long age) {
    response.addHeader(
        "Set-Cookie",
        ResponseCookie.from("auth_token", value)
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/api")
            .maxAge(age)
            .build()
            .toString());
  }
}
