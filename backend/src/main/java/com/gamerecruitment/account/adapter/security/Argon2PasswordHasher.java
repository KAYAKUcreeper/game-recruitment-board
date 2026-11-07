package com.gamerecruitment.account.adapter.security;

import com.gamerecruitment.account.application.port.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {
  private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);

  public String hash(String password) {
    return encoder.encode(password);
  }

  public boolean matches(String password, String hash) {
    return encoder.matches(password, hash);
  }
}
