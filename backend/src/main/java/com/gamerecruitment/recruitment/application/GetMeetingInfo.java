package com.gamerecruitment.recruitment.application;

import com.gamerecruitment.recruitment.application.port.RecruitmentRepository;
import java.time.Clock;
import java.util.UUID;

public class GetMeetingInfo {
  private final RecruitmentRepository repository;
  private final Clock clock;

  public GetMeetingInfo(RecruitmentRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public String execute(UUID recruitmentId, UUID accountId) {
    var value =
        repository
            .lockForShare(recruitmentId)
            .orElseThrow(
                () -> new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_NOT_FOUND));
    if (value.organizerId().equals(accountId)) return value.meetingInfo();
    boolean participant = repository.isParticipating(recruitmentId, accountId);
    if (!participant || value.cancelled())
      throw new RecruitmentFailure(RecruitmentFailure.Code.MEETING_INFO_FORBIDDEN);
    if (!clock.instant().isBefore(value.startsAt()))
      throw new RecruitmentFailure(RecruitmentFailure.Code.MEETING_INFO_FORBIDDEN);
    return value.meetingInfo();
  }
}
