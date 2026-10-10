package com.gamerecruitment.account.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AccountInput {
  private static final Pattern NON_SPACE =
      Pattern.compile(
          "[^\\u0009-\\u000D\\u0020\\u0085\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]");

  private AccountInput() {}

  public static String loginId(String value) {
    if (value == null || !value.matches("[A-Za-z0-9_]{3,30}"))
      throw new IllegalArgumentException("loginId");
    return value.toLowerCase(Locale.ROOT);
  }

  public static String displayName(String value) {
    return text(value, 1, 30, "displayName");
  }

  public static String password(String value) {
    return text(value, 15, 128, "password");
  }

  private static String text(String value, int min, int max, String field) {
    if (value == null
        || value.codePointCount(0, value.length()) < min
        || value.codePointCount(0, value.length()) > max
        || !NON_SPACE.matcher(value).find()) throw new IllegalArgumentException(field);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (Character.isHighSurrogate(c)) {
        if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(++i)))
          throw new IllegalArgumentException(field);
      } else if (Character.isLowSurrogate(c)) throw new IllegalArgumentException(field);
    }
    return value;
  }
}
