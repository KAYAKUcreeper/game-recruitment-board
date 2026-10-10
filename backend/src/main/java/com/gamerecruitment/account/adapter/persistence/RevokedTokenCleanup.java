package com.gamerecruitment.account.adapter.persistence;

import com.gamerecruitment.account.application.port.RevokedTokenStore;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RevokedTokenCleanup {
  private final RevokedTokenStore store;
  private final Clock clock;

  public RevokedTokenCleanup(RevokedTokenStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  @Scheduled(fixedDelay = 3600000)
  public void clean() {
    store.deleteExpired(clock.instant());
  }
}
