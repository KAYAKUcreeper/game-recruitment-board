package com.gamerecruitment.recruitment.domain;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

public final class RecruitmentInput {
  private static final Pattern NON_SPACE =
      Pattern.compile(
          "[^\\u0009-\\u000D\\u0020\\u0085\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000]");

  private RecruitmentInput() {}

  public static String title(String value) {
    return requiredText(value, 1, 100, "title");
  }

  public static String description(String value) {
    return requiredText(value, 1, 2000, "description");
  }

  public static String meetingInfo(String value) {
    if (value == null || value.isEmpty() || !NON_SPACE.matcher(value).find()) return "";
    return text(value, 0, 1000, "meetingInfo");
  }

  public static int capacity(int value) {
    if (value < 1 || value > 9) throw new IllegalArgumentException("capacity");
    return value;
  }

  public static OffsetDateTime startsAt(OffsetDateTime value) {
    if (value == null || value.getSecond() != 0 || value.getNano() != 0)
      throw new IllegalArgumentException("startsAt");
    return value;
  }

  private static String requiredText(String value, int min, int max, String field) {
    String result = text(value, min, max, field);
    if (!NON_SPACE.matcher(result).find()) throw new IllegalArgumentException(field);
    return result;
  }

  private static String text(String value, int min, int max, String field) {
    if (value == null) throw new IllegalArgumentException(field);
    validateSurrogates(value, field);
    int length = value.codePointCount(0, value.length());
    if (length < min || length > max) throw new IllegalArgumentException(field);
    return value;
  }

  private static void validateSurrogates(String value, String field) {
    for (int i = 0; i < value.length(); i++) {
      char current = value.charAt(i);
      if (Character.isHighSurrogate(current)) {
        if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(i + 1)))
          throw new IllegalArgumentException(field);
        i++;
      } else if (Character.isLowSurrogate(current)) {
        throw new IllegalArgumentException(field);
      }
    }
  }
}
