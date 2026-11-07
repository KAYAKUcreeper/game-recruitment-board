package com.gamerecruitment.account.application;

import com.gamerecruitment.account.application.port.*;
import java.time.Clock;

public class LogoutAccount {
  private final RevokedTokenStore store;
  private final TokenIssuer tokens;
  private final Clock clock;

  public LogoutAccount(RevokedTokenStore store, TokenIssuer tokens, Clock clock) {
    this.store = store;
    this.tokens = tokens;
    this.clock = clock;
  }

  public java.util.Optional<TokenIssuer.Details> prepare(String value) {
    return tokens.validate(value);
  }

  public void execute(TokenIssuer.Details token) {
    store.revoke(token.jti(), token.expiresAt(), clock.instant());
  }
}
