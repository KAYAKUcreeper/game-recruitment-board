package com.gamerecruitment.recruitment.application;

import com.gamerecruitment.recruitment.application.port.RecruitmentRepository;
import com.gamerecruitment.recruitment.domain.*;
import java.time.Clock;
import java.util.UUID;

public class JoinRecruitment {
  private final RecruitmentRepository repository;
  private final Clock clock;

  public JoinRecruitment(RecruitmentRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public void execute(UUID recruitmentId, UUID accountId) {
    Recruitment recruitment =
        repository
            .lock(recruitmentId)
            .orElseThrow(
                () -> new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_NOT_FOUND));
    int count = repository.participantCount(recruitmentId);
    if (recruitment.organizerId().equals(accountId)) fail(RecruitmentFailure.Code.OWN_RECRUITMENT);
    if (repository.isParticipating(recruitmentId, accountId))
      fail(RecruitmentFailure.Code.ALREADY_PARTICIPATING);
    ensureOpen(recruitment.state(count, clock.instant()));
    repository.addParticipation(recruitmentId, accountId, clock.instant());
    ensureBeforeStart(recruitment);
  }

  private void ensureOpen(RecruitmentState state) {
    switch (state) {
      case OPEN -> {}
      case FULL -> fail(RecruitmentFailure.Code.RECRUITMENT_FULL);
      case MANUALLY_CLOSED -> fail(RecruitmentFailure.Code.RECRUITMENT_CLOSED);
      case CANCELLED -> fail(RecruitmentFailure.Code.RECRUITMENT_CANCELLED);
      case STARTED -> fail(RecruitmentFailure.Code.RECRUITMENT_STARTED);
    }
  }

  private void ensureBeforeStart(Recruitment recruitment) {
    if (!clock.instant().isBefore(recruitment.startsAt()))
      fail(RecruitmentFailure.Code.RECRUITMENT_STARTED);
  }

  private void fail(RecruitmentFailure.Code code) {
    throw new RecruitmentFailure(code);
  }
}
