package com.gamerecruitment.recruitment.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.gamerecruitment.account.ApiTestSupport;
import com.gamerecruitment.recruitment.adapter.persistence.TransactionalRecruitmentCommands;
import com.gamerecruitment.recruitment.application.*;
import com.gamerecruitment.recruitment.domain.Game;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecruitmentTransactionTest extends ApiTestSupport {
  @Autowired TransactionalRecruitmentCommands transactions;
  @Autowired CreateRecruitment create;
  @Autowired JoinRecruitment join;
  @Autowired CancelParticipation cancel;
  @Autowired ManageRecruitment manage;
  @Autowired GetMeetingInfo meeting;

  private UUID account(String loginId) throws Exception {
    register(loginId);
    return jdbc.queryForObject("select id from accounts where login_id=?", UUID.class, loginId);
  }

  @Test
  void committedChangesFollowParticipationAndMeetingPermissions() throws Exception {
    UUID owner = account("transaction_owner");
    UUID player = account("transaction_player");
    var recruitment = transactions.execute(() -> create.execute(owner, Game.VALORANT,
        "タイトル", "募集文", OffsetDateTime.parse("2026-10-10T11:00:00+09:00"), 1, "合流先"));
    transactions.execute(() -> { join.execute(recruitment.id(), player); return null; });
    assertEquals("合流先", transactions.execute(() -> meeting.execute(recruitment.id(), player)));
    transactions.execute(() -> { manage.close(recruitment.id(), owner); return null; });
    transactions.execute(() -> { cancel.execute(recruitment.id(), player); return null; });
    assertTrue(jdbc.queryForObject("select manually_closed from recruitments where id=?",
        Boolean.class, recruitment.id()));
    assertEquals(0, jdbc.queryForObject("select count(*) from participations", Integer.class));
    assertThrows(RecruitmentFailure.class,
        () -> transactions.execute(() -> meeting.execute(recruitment.id(), player)));
  }

  @Test
  void failureRollsBackInsertedRecruitment() throws Exception {
    UUID owner = account("rollback_owner");
    assertThrows(IllegalStateException.class, () -> transactions.execute(() -> {
      create.execute(owner, Game.OVERWATCH, "タイトル", "募集文",
          OffsetDateTime.parse("2026-10-10T11:00:00+09:00"), 9, "");
      throw new IllegalStateException("rollback test");
    }));
    assertEquals(0, jdbc.queryForObject("select count(*) from recruitments", Integer.class));
  }
}
