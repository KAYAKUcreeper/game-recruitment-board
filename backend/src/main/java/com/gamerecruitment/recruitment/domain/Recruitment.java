package com.gamerecruitment.recruitment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Recruitment(
    UUID id,
    UUID organizerId,
    Game game,
    String title,
    String description,
    Instant startsAt,
    int capacity,
    String meetingInfo,
    boolean manuallyClosed,
    boolean cancelled,
    Instant createdAt) {

  public Recruitment {
    Objects.requireNonNull(id);
    Objects.requireNonNull(organizerId);
    Objects.requireNonNull(game);
    title = RecruitmentInput.title(title);
    description = RecruitmentInput.description(description);
    Objects.requireNonNull(startsAt);
    capacity = RecruitmentInput.capacity(capacity);
    meetingInfo = RecruitmentInput.meetingInfo(meetingInfo);
    Objects.requireNonNull(createdAt);
  }

  public RecruitmentState state(int participantCount, Instant now) {
    return RecruitmentState.determine(
        cancelled, startsAt, manuallyClosed, participantCount, capacity, now);
  }
}
