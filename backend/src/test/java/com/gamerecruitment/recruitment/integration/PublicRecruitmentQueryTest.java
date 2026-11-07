package com.gamerecruitment.recruitment.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.gamerecruitment.account.ApiTestSupport;
import com.gamerecruitment.recruitment.application.port.RecruitmentQueries;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PublicRecruitmentQueryTest extends ApiTestSupport {
  @Autowired RecruitmentQueries queries;

  private UUID insert(UUID owner, String game, Instant start, boolean closed, boolean cancelled) {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into recruitments (id,organizer_id,game,title,description,starts_at,capacity,meeting_info,manually_closed,cancelled,created_at) values (?,?,?,?,?,?,1,?,?,?,?)",
        id, owner, game, "title", "public body", Timestamp.from(start), "private secret", closed, cancelled,
        Timestamp.from(clock.instant()));
    return id;
  }

  @Test
  void openOnlyWithDeterministicOrderAndTwentyItemPages() throws Exception {
    register("query_owner");
    UUID owner = jdbc.queryForObject("select id from accounts", UUID.class);
    Instant start = clock.instant().plusSeconds(3600);
    List<UUID> expected = new ArrayList<>();
    for (int n = 0; n < 21; n++) expected.add(insert(owner, "VALORANT", start.plusSeconds(n * 60), false, false));
    insert(owner, "OVERWATCH", start, true, false);
    insert(owner, "OVERWATCH", start, false, true);
    insert(owner, "OVERWATCH", clock.instant(), false, false);
    var first = queries.publicRecruitments(Optional.empty(), 1, clock.instant());
    assertEquals(expected.subList(0, 20), first.items().stream().map(RecruitmentQueries.Summary::id).toList());
    assertTrue(first.hasNext());
    var second = queries.publicRecruitments(Optional.empty(), 2, clock.instant());
    assertEquals(List.of(expected.getLast()), second.items().stream().map(RecruitmentQueries.Summary::id).toList());
    assertFalse(second.hasNext());
    assertTrue(queries.publicRecruitments(Optional.of(com.gamerecruitment.recruitment.domain.Game.OVERWATCH), 1, clock.instant()).items().isEmpty());
  }

  @Test
  void fullIsHiddenFromListButDetailKeepsMatchingCountAndProfiles() throws Exception {
    register("query_owner");
    UUID owner = jdbc.queryForObject("select id from accounts", UUID.class);
    register("query_player");
    UUID player = jdbc.queryForObject("select id from accounts where login_id='query_player'", UUID.class);
    UUID id = insert(owner, "APEX_LEGENDS", clock.instant().plusSeconds(3600), false, false);
    jdbc.update("insert into participations values (?,?,?)", id, player, Timestamp.from(clock.instant()));
    assertTrue(queries.publicRecruitments(Optional.empty(), 1, clock.instant()).items().isEmpty());
    var detail = queries.publicRecruitment(id, clock.instant()).orElseThrow();
    assertEquals(detail.participants().size(), detail.participantCount());
    assertEquals(player, detail.participants().getFirst().id());
    assertEquals(owner, detail.organizer().id());
    assertEquals(com.gamerecruitment.recruitment.domain.RecruitmentState.FULL, detail.state());
  }
}
