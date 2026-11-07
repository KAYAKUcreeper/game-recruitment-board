package com.gamerecruitment.account.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AccountInputTest {
  @Test
  void idBoundsAndNormalization() {
    assertEquals("player_a", AccountInput.loginId("PLAYER_A"));
    for (int n : new int[] {3, 30}) assertDoesNotThrow(() -> AccountInput.loginId("a".repeat(n)));
    for (String v : new String[] {"aa", "a".repeat(31), " abc", "abc ", "あいう", "abc\n"})
      assertThrows(IllegalArgumentException.class, () -> AccountInput.loginId(v));
  }

  @Test
  void codePointsAndWhitespace() {
    for (int n : new int[] {1, 30})
      assertDoesNotThrow(() -> AccountInput.displayName("😀".repeat(n)));
    for (String v : new String[] {"", "😀".repeat(31), "\u0085\u00a0\u3000", "\n\t "})
      assertThrows(IllegalArgumentException.class, () -> AccountInput.displayName(v));
    for (int n : new int[] {15, 128})
      assertDoesNotThrow(() -> AccountInput.password("😀".repeat(n)));
    for (String v :
        new String[] {"a".repeat(14), "a".repeat(129), "\u3000".repeat(15), "\uD800".repeat(15)})
      assertThrows(IllegalArgumentException.class, () -> AccountInput.password(v));
    String password = " 日本語Aa".repeat(4) + " ";
    assertEquals(password, AccountInput.password(password));
  }
}
