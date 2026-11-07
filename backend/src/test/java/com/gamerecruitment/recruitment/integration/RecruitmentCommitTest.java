package com.gamerecruitment.recruitment.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gamerecruitment.recruitment.adapter.persistence.TransactionalRecruitmentCommands;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class RecruitmentCommitTest {
  @Test
  void commitFailurePropagatesInsteadOfReturningSuccess() {
    PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    var status = new SimpleTransactionStatus();
    when(manager.getTransaction(any())).thenReturn(status);
    doThrow(new TransactionSystemException("commit failed")).when(manager).commit(status);
    var commands = new TransactionalRecruitmentCommands(new TransactionTemplate(manager));
    assertThrows(TransactionSystemException.class, () -> commands.execute(() -> "success"));
    verify(manager).getTransaction(argThat(definition ->
        definition.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_COMMITTED
            && !definition.isReadOnly()));
  }
}
