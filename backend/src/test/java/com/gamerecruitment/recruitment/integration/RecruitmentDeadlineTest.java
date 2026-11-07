package com.gamerecruitment.recruitment.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import com.gamerecruitment.account.ApiTestSupport;
import com.gamerecruitment.recruitment.adapter.persistence.*;
import com.gamerecruitment.recruitment.application.*;
import com.gamerecruitment.recruitment.domain.Game;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class RecruitmentDeadlineTest extends ApiTestSupport {
  @Autowired TransactionalRecruitmentCommands transactions;
  @Autowired CreateRecruitment create;
  @Autowired JoinRecruitment join;
  @MockitoSpyBean JdbcRecruitmentRepository repository;
  private static final OffsetDateTime START = OffsetDateTime.parse("2026-10-10T11:00:00+09:00");

  @Test
  void creationRollsBackIfDeadlineArrivesAfterInsert() throws Exception {
    register("deadline_owner");
    UUID owner = jdbc.queryForObject("select id from accounts", UUID.class);
    doAnswer(invocation -> {
      Object result = invocation.callRealMethod();
      clock.at(START.toInstant());
      return result;
    }).when(repository).insert(any());
    var failure = assertThrows(RecruitmentFailure.class, () -> transactions.execute(() ->
        create.execute(owner, Game.VALORANT, "title", "body", START, 1, "")));
    assertEquals(RecruitmentFailure.Code.VALIDATION_ERROR, failure.code());
    assertEquals(0, jdbc.queryForObject("select count(*) from recruitments", Integer.class));
  }

  @Test
  void joiningRollsBackIfDeadlineArrivesAfterParticipationInsert() throws Exception {
    register("deadline_owner");
    UUID owner = jdbc.queryForObject("select id from accounts", UUID.class);
    register("deadline_player");
    UUID player = jdbc.queryForObject("select id from accounts where login_id='deadline_player'", UUID.class);
    var value = transactions.execute(() -> create.execute(owner, Game.VALORANT, "title", "body", START, 1, ""));
    doAnswer(invocation -> {
      Object result = invocation.callRealMethod();
      clock.at(START.toInstant());
      return result;
    }).when(repository).addParticipation(any(), any(), any());
    var failure = assertThrows(RecruitmentFailure.class, () -> transactions.execute(() -> {
      join.execute(value.id(), player);
      return null;
    }));
    assertEquals(RecruitmentFailure.Code.RECRUITMENT_STARTED, failure.code());
    assertEquals(0, jdbc.queryForObject("select count(*) from participations", Integer.class));
  }
}
