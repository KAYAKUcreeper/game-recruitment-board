package com.gamerecruitment.recruitment.adapter.web;

import com.gamerecruitment.recruitment.adapter.web.generated.api.*;
import com.gamerecruitment.recruitment.adapter.web.generated.model.*;
import com.gamerecruitment.recruitment.application.RecruitmentFailure;
import com.gamerecruitment.recruitment.application.port.RecruitmentQueries;
import java.time.*;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PublicRecruitmentController implements GamesApi, RecruitmentsApi {
  private final RecruitmentQueries queries;
  private final Clock clock;

  public PublicRecruitmentController(RecruitmentQueries queries, Clock clock) {
    this.queries = queries;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<GameList> listGames() {
    var items =
        queries.games().stream()
            .map(
                game ->
                    new com.gamerecruitment.recruitment.adapter.web.generated.model.Game(
                        GameCode.valueOf(game.name()),
                        com.gamerecruitment.recruitment.adapter.web.generated.model.Game.NameEnum
                            .valueOf(game.name())))
            .toList();
    return ResponseEntity.ok(new GameList(items));
  }

  @Override
  public ResponseEntity<RecruitmentPage> listRecruitments(GameCode game, Integer page) {
    try {
      var result =
          queries.publicRecruitments(
              Optional.ofNullable(game)
                  .map(value -> com.gamerecruitment.recruitment.domain.Game.valueOf(value.name())),
              page,
              clock.instant());
      return ResponseEntity.ok(
          new RecruitmentPage(
              result.items().stream().map(this::summary).toList(),
              page,
              RecruitmentPage.PageSizeEnum.NUMBER_20,
              result.hasNext()));
    } catch (IllegalArgumentException exception) {
      throw new RecruitmentFailure(
          RecruitmentFailure.Code.VALIDATION_ERROR, java.util.List.of("page"));
    }
  }

  @Override
  public ResponseEntity<RecruitmentDetail> getRecruitment(UUID recruitmentId) {
    return ResponseEntity.ok(
        queries
            .publicRecruitment(recruitmentId, clock.instant())
            .map(this::detail)
            .orElseThrow(
                () -> new RecruitmentFailure(RecruitmentFailure.Code.RECRUITMENT_NOT_FOUND)));
  }

  @Override
  public ResponseEntity<RecruitmentDetail> createRecruitment(CreateRecruitmentRequest request) {
    throw new UnsupportedOperationException("Implemented with US2");
  }

  @Override
  public ResponseEntity<RecruitmentDetail> closeRecruitment(UUID recruitmentId) {
    throw new UnsupportedOperationException("Implemented with US5");
  }

  @Override
  public ResponseEntity<RecruitmentDetail> reopenRecruitment(UUID recruitmentId) {
    throw new UnsupportedOperationException("Implemented with US5");
  }

  @Override
  public ResponseEntity<RecruitmentDetail> cancelRecruitment(UUID recruitmentId) {
    throw new UnsupportedOperationException("Implemented with US5");
  }

  private RecruitmentSummary summary(RecruitmentQueries.Summary value) {
    return new RecruitmentSummary(
        value.id(),
        GameCode.valueOf(value.game().name()),
        value.title(),
        OffsetDateTime.ofInstant(value.startsAt(), ZoneOffset.UTC),
        value.capacity(),
        value.participantCount(),
        com.gamerecruitment.recruitment.adapter.web.generated.model.RecruitmentState.valueOf(
            value.state().name()),
        profile(value.organizer()));
  }

  private RecruitmentDetail detail(RecruitmentQueries.Detail value) {
    return new RecruitmentDetail(
        value.id(),
        GameCode.valueOf(value.game().name()),
        value.title(),
        OffsetDateTime.ofInstant(value.startsAt(), ZoneOffset.UTC),
        value.capacity(),
        value.participantCount(),
        com.gamerecruitment.recruitment.adapter.web.generated.model.RecruitmentState.valueOf(
            value.state().name()),
        profile(value.organizer()),
        value.description(),
        value.participants().stream().map(this::profile).toList());
  }

  private AccountProfile profile(RecruitmentQueries.Profile value) {
    return new AccountProfile(value.id(), value.displayName());
  }
}
