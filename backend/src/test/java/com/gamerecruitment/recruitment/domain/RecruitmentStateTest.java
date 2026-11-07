package com.gamerecruitment.recruitment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RecruitmentStateTest {
  private static final Instant STARTS_AT = Instant.parse("2026-11-07T03:00:00Z");

  @Test
  void appliesStatePriority() {
    Instant before = STARTS_AT.minusNanos(1);
    assertEquals(
        RecruitmentState.CANCELLED,
        RecruitmentState.determine(true, STARTS_AT, true, 9, 9, STARTS_AT.plusSeconds(1)));
    assertEquals(
        RecruitmentState.STARTED,
        RecruitmentState.determine(false, STARTS_AT, true, 9, 9, STARTS_AT));
    assertEquals(
        RecruitmentState.MANUALLY_CLOSED,
        RecruitmentState.determine(false, STARTS_AT, true, 9, 9, before));
    assertEquals(
        RecruitmentState.FULL,
        RecruitmentState.determine(false, STARTS_AT, false, 9, 9, before));
    assertEquals(
        RecruitmentState.OPEN,
        RecruitmentState.determine(false, STARTS_AT, false, 8, 9, before));
  }

  @Test
  void startsExactlyAtTheScheduledInstant() {
    assertEquals(
        RecruitmentState.OPEN,
        RecruitmentState.determine(false, STARTS_AT, false, 0, 1, STARTS_AT.minusNanos(1)));
    assertEquals(
        RecruitmentState.STARTED,
        RecruitmentState.determine(false, STARTS_AT, false, 0, 1, STARTS_AT));
    assertEquals(
        RecruitmentState.STARTED,
        RecruitmentState.determine(false, STARTS_AT, false, 0, 1, STARTS_AT.plusNanos(1)));
  }
}
