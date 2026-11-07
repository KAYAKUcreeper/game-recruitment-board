package com.gamerecruitment.recruitment.application.port;

import com.gamerecruitment.recruitment.domain.Recruitment;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RecruitmentRepository {
  void insert(Recruitment recruitment);

  Optional<Recruitment> lock(UUID recruitmentId);

  Optional<Recruitment> lockForShare(UUID recruitmentId);

  int participantCount(UUID recruitmentId);

  boolean isParticipating(UUID recruitmentId, UUID accountId);

  void addParticipation(UUID recruitmentId, UUID accountId, Instant joinedAt);

  int removeParticipation(UUID recruitmentId, UUID accountId);

  void setManuallyClosed(UUID recruitmentId, boolean closed);

  void setCancelled(UUID recruitmentId);

  void updateMeetingInfo(UUID recruitmentId, String meetingInfo);
}
