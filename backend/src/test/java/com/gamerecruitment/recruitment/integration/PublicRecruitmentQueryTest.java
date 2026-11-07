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

  @Test
  void sameStartUsesCreationTimeThenUuidAndEachGameCanBeSelected() throws Exception {
    register("order_owner");
    UUID owner = jdbc.queryForObject("select id from accounts", UUID.class);
    Instant start = clock.instant().plusSeconds(3600);
    UUID oldest = insert(owner, "VALORANT", start, false, false);
    UUID first = insert(owner, "APEX_LEGENDS", start, false, false);
    UUID second = insert(owner, "OVERWATCH", start, false, false);
    jdbc.update("update recruitments set created_at=? where id=?", Timestamp.from(clock.instant().minusSeconds(1)), oldest);
    List<UUID> expected = new ArrayList<>(List.of(first, second));
    expected.sort(Comparator.comparing(UUID::toString));
    expected.addFirst(oldest);
    assertEquals(expected, queries.publicRecruitments(Optional.empty(), 1, clock.instant()).items().stream().map(RecruitmentQueries.Summary::id).toList());
    for (var game : com.gamerecruitment.recruitment.domain.Game.values()) {
      var result = queries.publicRecruitments(Optional.of(game), 1, clock.instant());
      assertEquals(1, result.items().size());
      assertEquals(game, result.items().getFirst().game());
    }
  }

  @Test
  void concurrentParticipationChangesKeepDetailCountAndArrayConsistent() throws Exception {
    register("snapshot_owner");
    UUID owner = jdbc.queryForObject("select id from accounts", UUID.class);
    register("snapshot_player");
    UUID player = jdbc.queryForObject("select id from accounts where login_id='snapshot_player'", UUID.class);
    UUID id = insert(owner, "VALORANT", clock.instant().plusSeconds(3600), false, false);
    var started = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var writer = executor.submit(() -> {
        started.countDown();
        for (int i = 0; i < 50; i++) {
          jdbc.update("insert into participations values (?,?,?)", id, player, Timestamp.from(clock.instant()));
          jdbc.update("delete from participations where recruitment_id=? and account_id=?", id, player);
        }
      });
      assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
      for (int i = 0; i < 50; i++) {
        var value = queries.publicRecruitment(id, clock.instant()).orElseThrow();
        assertEquals(value.participants().size(), value.participantCount());
      }
      writer.get(10, java.util.concurrent.TimeUnit.SECONDS);
    }
  }

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
