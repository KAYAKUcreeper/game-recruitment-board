package com.gamerecruitment.account.security;

import static org.junit.jupiter.api.Assertions.*;

import com.gamerecruitment.account.adapter.security.Argon2PasswordHasher;
import org.junit.jupiter.api.Test;

class PasswordHashTest {
  @Test
  void longUnicodePasswordsAreNotTruncatedOrNormalized() {
    var hasher = new Argon2PasswordHasher();
    String password = " あ😀".repeat(40) + " ";
    String hash = hasher.hash(password);
    assertTrue(hash.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"));
    assertTrue(hasher.matches(password, hash));
    assertFalse(hasher.matches(password.strip(), hash));
    assertFalse(hasher.matches(password.substring(0, 72), hash));
    assertNotEquals(hash, hasher.hash(password));
  }
}
