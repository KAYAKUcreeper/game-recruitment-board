package com.gamerecruitment.recruitment.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class RecruitmentInputTest {
  @Test
  void validatesRequiredTextByUnicodeCodePointWithoutChangingIt() {
    for (int length : new int[] {1, 100}) {
      String value = "😀".repeat(length);
      assertEquals(value, RecruitmentInput.title(value));
    }
    for (int length : new int[] {1, 2000})
      assertDoesNotThrow(() -> RecruitmentInput.description("a".repeat(length)));
    for (String value : new String[] {"", "a".repeat(101), "\u0085\u00a0\u3000", "\n\t "})
      assertThrows(IllegalArgumentException.class, () -> RecruitmentInput.title(value));
    assertThrows(
        IllegalArgumentException.class, () -> RecruitmentInput.description("a".repeat(2001)));
    assertThrows(IllegalArgumentException.class, () -> RecruitmentInput.title("\uD800"));
    assertEquals(" A\n😀 ", RecruitmentInput.title(" A\n😀 "));
  }

  @Test
  void validatesCapacityMeetingInfoAndMinutePrecision() {
    for (int capacity : new int[] {1, 9})
      assertEquals(capacity, RecruitmentInput.capacity(capacity));
    for (int capacity : new int[] {0, 10})
      assertThrows(IllegalArgumentException.class, () -> RecruitmentInput.capacity(capacity));
    assertEquals("", RecruitmentInput.meetingInfo(null));
    assertEquals("", RecruitmentInput.meetingInfo("\u3000\n"));
    assertEquals("x".repeat(1000), RecruitmentInput.meetingInfo("x".repeat(1000)));
    assertThrows(
        IllegalArgumentException.class, () -> RecruitmentInput.meetingInfo("x".repeat(1001)));
    assertDoesNotThrow(
        () -> RecruitmentInput.startsAt(OffsetDateTime.parse("2026-11-07T12:34:00+09:00")));
    assertThrows(
        IllegalArgumentException.class,
        () -> RecruitmentInput.startsAt(OffsetDateTime.parse("2026-11-07T12:34:01+09:00")));
  }

  @Test
  void gameHasExactlyTheThreeSpecifiedValues() {
    assertArrayEquals(
        new Game[] {Game.VALORANT, Game.APEX_LEGENDS, Game.OVERWATCH}, Game.values());
    assertThrows(IllegalArgumentException.class, () -> Game.valueOf("UNKNOWN"));
  }
}
