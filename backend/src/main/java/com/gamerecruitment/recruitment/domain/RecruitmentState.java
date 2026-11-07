package com.gamerecruitment.recruitment.domain;

import java.time.Instant;

public enum RecruitmentState {
  CANCELLED,
  STARTED,
  MANUALLY_CLOSED,
  FULL,
  OPEN;

  public static RecruitmentState determine(
      boolean cancelled,
      Instant startsAt,
      boolean manuallyClosed,
      int participantCount,
      int capacity,
      Instant now) {
    if (cancelled) return CANCELLED;
    if (!now.isBefore(startsAt)) return STARTED;
    if (manuallyClosed) return MANUALLY_CLOSED;
    if (participantCount >= capacity) return FULL;
    return OPEN;
  }
}
