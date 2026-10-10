package com.gamerecruitment.account.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gamerecruitment.account.adapter.persistence.TransactionalLogout;
import com.gamerecruitment.account.application.LogoutAccount;
import com.gamerecruitment.account.application.port.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class LogoutCommitTest {
  @Test
  void successCannotReturnBeforeCommit() {
    var store = mock(RevokedTokenStore.class);
    var issuer = mock(TokenIssuer.class);
    var manager = mock(PlatformTransactionManager.class);
    var now = Instant.now();
    var details =
        new TokenIssuer.Details(UUID.randomUUID(), UUID.randomUUID(), now, now.plusSeconds(3600));
    when(issuer.validate("token")).thenReturn(Optional.of(details));
    when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    doThrow(new TransactionSystemException("test commit failure")).when(manager).commit(any());
    var logout =
        new TransactionalLogout(
            new LogoutAccount(store, issuer, Clock.fixed(now, ZoneOffset.UTC)),
            new TransactionTemplate(manager));
    assertThrows(TransactionSystemException.class, () -> logout.execute("token"));
    var order = inOrder(store, manager);
    order.verify(store).revoke(details.jti(), details.expiresAt(), now);
    order.verify(manager).commit(any());
  }

  @Test
  void invalidTokensDoNotNeedADatabaseTransaction() {
    var store = mock(RevokedTokenStore.class);
    var issuer = mock(TokenIssuer.class);
    var manager = mock(PlatformTransactionManager.class);
    when(issuer.validate("invalid")).thenReturn(Optional.empty());
    var logout =
        new TransactionalLogout(
            new LogoutAccount(store, issuer, Clock.systemUTC()), new TransactionTemplate(manager));
    assertDoesNotThrow(() -> logout.execute("invalid"));
    verifyNoInteractions(manager, store);
  }
}
