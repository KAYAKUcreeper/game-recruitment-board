package com.gamerecruitment.account.adapter.persistence;

import com.gamerecruitment.account.application.RegisterAccount;
import com.gamerecruitment.account.domain.Account;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionalRegistration {
  private final RegisterAccount registration;
  private final TransactionTemplate transaction;

  public TransactionalRegistration(RegisterAccount registration, TransactionTemplate transaction) {
    this.registration = registration;
    this.transaction = transaction;
  }

  public Account execute(String id, String password, String displayName) {
    return transaction.execute(status -> registration.execute(id, password, displayName));
  }
}
