package com.gamerecruitment.account.adapter.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieTokenResolver {
  public String resolve(HttpServletRequest request) {
    String value = null;
    if (request.getCookies() == null) return null;
    for (var cookie : request.getCookies())
      if ("auth_token".equals(cookie.getName())) {
        if (value != null) return null;
        value = cookie.getValue();
      }
    return value;
  }
}
