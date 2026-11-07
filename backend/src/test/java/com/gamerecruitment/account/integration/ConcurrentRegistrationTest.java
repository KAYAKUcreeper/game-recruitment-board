package com.gamerecruitment.account.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.gamerecruitment.account.ApiTestSupport;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class ConcurrentRegistrationTest extends ApiTestSupport {
  @Test
  void tenConcurrentRegistrationsHaveExactlyOneWinner() throws Exception {
    var requests = new ArrayList<Callable<Integer>>();
    var start = new CountDownLatch(1);
    for (int i = 0; i < 10; i++) {
      String id = i % 2 == 0 ? "player_a" : "PLAYER_A";
      var t = csrf();
      requests.add(
          () -> {
            start.await();
            return send("/api/accounts", registration(id), t).andReturn().getResponse().getStatus();
          });
    }
    try (var pool = Executors.newFixedThreadPool(10)) {
      var futures = requests.stream().map(pool::submit).toList();
      start.countDown();
      var statuses = new ArrayList<Integer>();
      for (var f : futures) statuses.add(f.get(30, TimeUnit.SECONDS));
      assertEquals(1, statuses.stream().filter(s -> s == 201).count());
      assertEquals(9, statuses.stream().filter(s -> s == 409).count());
    }
    assertEquals(1, jdbc.queryForObject("select count(*) from accounts", Integer.class));
    // A fresh connection observes the committed account.
    try (var connection = context.getBean(javax.sql.DataSource.class).getConnection();
        var st = connection.createStatement();
        var rows = st.executeQuery("select count(*) from accounts")) {
      rows.next();
      assertEquals(1, rows.getInt(1));
    }
  }
}
