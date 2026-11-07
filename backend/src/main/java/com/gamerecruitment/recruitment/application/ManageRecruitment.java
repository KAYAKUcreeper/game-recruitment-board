package com.gamerecruitment.recruitment.application;

import com.gamerecruitment.recruitment.application.port.RecruitmentRepository;
import com.gamerecruitment.recruitment.domain.Recruitment;
import com.gamerecruitment.recruitment.domain.RecruitmentInput;
import java.time.Clock;
import java.util.UUID;

public class ManageRecruitment {
  private final RecruitmentRepository repository;
  private final Clock clock;

  public ManageRecruitment(RecruitmentRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public void close(UUID id, UUID accountId) {
    Recruitment value = lockedForOwner(id, accountId);
    mutable(value);
    repository.setManuallyClosed(id, true);
    mutable(value);
  }

  public void reopen(UUID id, UUID accountId) {
    Recruitment value = lockedForOwner(id, accountId);
    mutable(value);
    if (!value.manuallyClosed())
      throw new RecruitmentFailure(RecruitmentFailure.Code.NOT_MANUALLY_CLOSED);
    if (repository.participantCount(id) >= value.capacity())
      throw new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_FULL);
    repository.setManuallyClosed(id, false);
    mutable(value);
  }

  public void cancel(UUID id, UUID accountId) {
    Recruitment value = lockedForOwner(id, accountId);
    mutable(value);
    repository.setCancelled(id);
    if (!clock.instant().isBefore(value.startsAt()))
      throw new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_STARTED);
  }

  public void updateMeetingInfo(UUID id, UUID accountId, String text) {
    Recruitment value = lockedForOwner(id, accountId);
    mutable(value);
    repository.updateMeetingInfo(id, RecruitmentInput.meetingInfo(text));
    mutable(value);
  }

  private Recruitment lockedForOwner(UUID id, UUID accountId) {
    Recruitment value =
        repository
            .lock(id)
            .orElseThrow(
                () -> new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_NOT_FOUND));
    if (!value.organizerId().equals(accountId))
      throw new RecruitmentFailure(RecruitmentFailure.Code.NOT_OWNER);
    return value;
  }

  private void mutable(Recruitment value) {
    if (value.cancelled())
      throw new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_CANCELLED);
    if (!clock.instant().isBefore(value.startsAt()))
      throw new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_STARTED);
  }
}
