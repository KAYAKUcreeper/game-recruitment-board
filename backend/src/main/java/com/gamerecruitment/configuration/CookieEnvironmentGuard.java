package com.gamerecruitment.configuration;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CookieEnvironmentGuard {
  public CookieEnvironmentGuard(Environment environment) {
    if (!environment.getProperty("app.secure-cookies", Boolean.class, true)) {
      String address = environment.getProperty("server.address", "");
      if (!"127.0.0.1".equals(address) && !"::1".equals(address)) {
        throw new IllegalStateException(
            "Non-Secure cookies require an explicit loopback server address");
      }
    }
  }
}
