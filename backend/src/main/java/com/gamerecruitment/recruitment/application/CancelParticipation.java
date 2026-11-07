package com.gamerecruitment.recruitment.application;

import com.gamerecruitment.recruitment.application.port.RecruitmentRepository;
import java.time.Clock;
import java.util.UUID;

public class CancelParticipation {
  private final RecruitmentRepository repository;
  private final Clock clock;

  public CancelParticipation(RecruitmentRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public void execute(UUID recruitmentId, UUID accountId) {
    var recruitment =
        repository
            .lock(recruitmentId)
            .orElseThrow(
                () -> new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_NOT_FOUND));
    if (!clock.instant().isBefore(recruitment.startsAt()))
      throw new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_STARTED);
    if (!repository.isParticipating(recruitmentId, accountId))
      throw new RecruitmentFailure(RecruitmentFailure.Code.NOT_PARTICIPATING);
    repository.removeParticipation(recruitmentId, accountId);
    if (!clock.instant().isBefore(recruitment.startsAt()))
      throw new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_STARTED);
  }
}
