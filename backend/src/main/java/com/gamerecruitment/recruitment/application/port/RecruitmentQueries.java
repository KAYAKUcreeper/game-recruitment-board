package com.gamerecruitment.recruitment.application.port;

import com.gamerecruitment.recruitment.domain.Game;
import com.gamerecruitment.recruitment.domain.RecruitmentState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecruitmentQueries {
  record Profile(UUID id, String displayName) {}

  record Summary(
      UUID id,
      Game game,
      String title,
      Instant startsAt,
      int participantCount,
      int capacity,
      RecruitmentState state,
      Profile organizer) {}

  record Detail(
      UUID id,
      Game game,
      String title,
      String description,
      Instant startsAt,
      int participantCount,
      int capacity,
      RecruitmentState state,
      Profile organizer,
      List<Profile> participants) {}

  record Page(List<Summary> items, boolean hasNext) {}

  List<Game> games();

  Page publicRecruitments(Optional<Game> game, int page, Instant now);

  Optional<Detail> publicRecruitment(UUID recruitmentId, Instant now);

  Page myRecruitments(UUID accountId, boolean created, boolean upcoming, int page, Instant now);
}
