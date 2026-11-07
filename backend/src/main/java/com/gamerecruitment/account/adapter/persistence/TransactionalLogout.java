package com.gamerecruitment.account.adapter.persistence;

import com.gamerecruitment.account.application.LogoutAccount;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionalLogout {
  private final LogoutAccount logout;
  private final TransactionTemplate transaction;

  public TransactionalLogout(LogoutAccount logout, TransactionTemplate transaction) {
    this.logout = logout;
    this.transaction = transaction;
  }

  public void execute(String token) {
    logout
        .prepare(token)
        .ifPresent(details -> transaction.executeWithoutResult(status -> logout.execute(details)));
  }
}
