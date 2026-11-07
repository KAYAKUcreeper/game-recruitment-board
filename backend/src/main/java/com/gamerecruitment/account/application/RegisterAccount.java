package com.gamerecruitment.account.application;

import com.gamerecruitment.account.application.port.*;
import com.gamerecruitment.account.domain.*;
import java.time.Clock;
import java.util.*;

public class RegisterAccount {
  private final AccountRepository repository;
  private final PasswordHasher hasher;
  private final Clock clock;

  public RegisterAccount(AccountRepository repository, PasswordHasher hasher, Clock clock) {
    this.repository = repository;
    this.hasher = hasher;
    this.clock = clock;
  }

  public Account execute(String loginId, String password, String displayName) {
    List<String> fields = new ArrayList<>();
    try {
      AccountInput.loginId(loginId);
    } catch (IllegalArgumentException e) {
      fields.add("loginId");
    }
    try {
      AccountInput.password(password);
    } catch (IllegalArgumentException e) {
      fields.add("password");
    }
    try {
      AccountInput.displayName(displayName);
    } catch (IllegalArgumentException e) {
      fields.add("displayName");
    }
    if (!fields.isEmpty()) throw new AccountFailure(AccountFailure.Code.VALIDATION_ERROR, fields);
    var account = new Account(UUID.randomUUID(), loginId, displayName);
    repository.save(
        new AccountRepository.StoredAccount(
            account.id(),
            account.loginId(),
            account.displayName(),
            hasher.hash(password),
            clock.instant()));
    return account;
  }
}
