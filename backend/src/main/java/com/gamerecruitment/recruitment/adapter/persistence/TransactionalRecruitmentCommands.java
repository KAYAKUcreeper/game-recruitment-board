package com.gamerecruitment.recruitment.adapter.persistence;

import java.util.function.Supplier;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionalRecruitmentCommands {
  private final TransactionTemplate transaction;

  public TransactionalRecruitmentCommands(TransactionTemplate transaction) {
    this.transaction = transaction;
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  public <T> T execute(Supplier<T> command) {
    return transaction.execute(status -> command.get());
  }
}
