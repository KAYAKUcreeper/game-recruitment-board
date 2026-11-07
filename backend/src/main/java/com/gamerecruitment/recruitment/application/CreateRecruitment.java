package com.gamerecruitment.recruitment.application;

import com.gamerecruitment.recruitment.application.port.RecruitmentRepository;
import com.gamerecruitment.recruitment.domain.*;
import java.time.*;
import java.util.UUID;

public class CreateRecruitment {
  private final RecruitmentRepository repository;
  private final Clock clock;

  public CreateRecruitment(RecruitmentRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public Recruitment execute(
      UUID organizerId,
      Game game,
      String title,
      String description,
      OffsetDateTime startsAt,
      int capacity,
      String meetingInfo) {
    Instant start = RecruitmentInput.startsAt(startsAt).toInstant();
    Instant now = clock.instant();
    if (!start.isAfter(now))
      throw new RecruitmentFailure(
          RecruitmentFailure.Code.VALIDATION_ERROR, java.util.List.of("startsAt"));
    Recruitment recruitment =
        new Recruitment(
            UUID.randomUUID(),
            organizerId,
            game,
            title,
            description,
            start,
            capacity,
            meetingInfo,
            false,
            false,
            now);
    if (!start.isAfter(clock.instant()))
      throw new RecruitmentFailure(
          RecruitmentFailure.Code.VALIDATION_ERROR, java.util.List.of("startsAt"));
    repository.insert(recruitment);
    if (!start.isAfter(clock.instant()))
      throw new RecruitmentFailure(
          RecruitmentFailure.Code.VALIDATION_ERROR, java.util.List.of("startsAt"));
    return recruitment;
  }
}
