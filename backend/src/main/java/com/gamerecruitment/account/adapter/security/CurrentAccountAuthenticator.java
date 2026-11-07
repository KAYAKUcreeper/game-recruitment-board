package com.gamerecruitment.account.adapter.security;

import com.gamerecruitment.account.application.AccountFailure;
import com.gamerecruitment.account.application.port.*;
import java.util.Optional;

public class CurrentAccountAuthenticator {
  public record Current(AccountRepository.StoredAccount account, TokenIssuer.Details token) {}

  private final TokenIssuer tokens;
  private final RevokedTokenStore store;
  private final AccountRepository repository;

  public CurrentAccountAuthenticator(
      TokenIssuer tokens, RevokedTokenStore store, AccountRepository repository) {
    this.tokens = tokens;
    this.store = store;
    this.repository = repository;
  }

  public Optional<Current> optional(String token) {
    return tokens
        .validate(token)
        .filter(t -> !store.isRevoked(t.jti()))
        .flatMap(t -> repository.findById(t.accountId()).map(a -> new Current(a, t)));
  }

  public Current required(String token) {
    return optional(token)
        .orElseThrow(() -> new AccountFailure(AccountFailure.Code.UNAUTHENTICATED));
  }
}
