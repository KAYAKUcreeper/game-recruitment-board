package com.gamerecruitment.account.domain;

import java.util.Objects;
import java.util.UUID;

public record Account(UUID id, String loginId, String displayName) {
  public Account {
    Objects.requireNonNull(id);
    loginId = AccountInput.loginId(loginId);
    displayName = AccountInput.displayName(displayName);
  }
}
