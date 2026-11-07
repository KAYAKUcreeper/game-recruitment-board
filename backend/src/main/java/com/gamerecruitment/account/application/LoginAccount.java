package com.gamerecruitment.account.application;

import com.gamerecruitment.account.application.port.*;
import com.gamerecruitment.account.domain.AccountInput;
import java.util.List;

public class LoginAccount {
  public record Result(AccountRepository.StoredAccount account, TokenIssuer.Issued token) {}

  private final AccountRepository repository;
  private final PasswordHasher hasher;
  private final TokenIssuer tokens;
  private final String dummyHash;

  public LoginAccount(AccountRepository repository, PasswordHasher hasher, TokenIssuer tokens) {
    this.repository = repository;
    this.hasher = hasher;
    this.tokens = tokens;
    this.dummyHash = hasher.hash("dummy-passphrase-not-a-real-login");
  }

  public Result execute(String loginId, String password, boolean alreadyAuthenticated) {
    if (alreadyAuthenticated) throw new AccountFailure(AccountFailure.Code.ALREADY_AUTHENTICATED);
    String normalized;
    try {
      normalized = AccountInput.loginId(loginId);
    } catch (IllegalArgumentException e) {
      throw new AccountFailure(AccountFailure.Code.VALIDATION_ERROR, List.of("loginId"));
    }
    try {
      AccountInput.password(password);
    } catch (IllegalArgumentException e) {
      throw new AccountFailure(AccountFailure.Code.VALIDATION_ERROR, List.of("password"));
    }
    var account = repository.findByLoginId(normalized);
    boolean matches =
        hasher.matches(
            password, account.map(AccountRepository.StoredAccount::passwordHash).orElse(dummyHash));
    if (account.isEmpty() || !matches)
      throw new AccountFailure(AccountFailure.Code.INVALID_CREDENTIALS);
    return new Result(account.get(), tokens.issue(account.get().id()));
  }
}
