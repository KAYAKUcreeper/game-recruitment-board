package com.gamerecruitment.configuration;

import com.gamerecruitment.account.adapter.persistence.*;
import com.gamerecruitment.account.adapter.security.*;
import com.gamerecruitment.account.application.*;
import com.gamerecruitment.account.application.port.*;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
public class AccountConfiguration {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  TokenIssuer tokens(@Value("${app.jwt-secret-base64}") String key, Clock clock) {
    return new JwtTokenService(key, clock);
  }

  @Bean
  RegisterAccount register(AccountRepository repo, PasswordHasher hasher, Clock clock) {
    return new RegisterAccount(repo, hasher, clock);
  }

  @Bean
  TransactionalRegistration registration(
      RegisterAccount registration, PlatformTransactionManager manager) {
    return new TransactionalRegistration(registration, new TransactionTemplate(manager));
  }

  @Bean
  LoginAccount login(AccountRepository repo, PasswordHasher hasher, TokenIssuer tokens) {
    return new LoginAccount(repo, hasher, tokens);
  }

  @Bean
  LogoutAccount logout(RevokedTokenStore store, TokenIssuer tokens, Clock clock) {
    return new LogoutAccount(store, tokens, clock);
  }

  @Bean
  TransactionalLogout transactionalLogout(
      LogoutAccount logout, PlatformTransactionManager manager) {
    return new TransactionalLogout(logout, new TransactionTemplate(manager));
  }

  @Bean
  CurrentAccountAuthenticator authenticator(
      TokenIssuer tokens, RevokedTokenStore store, AccountRepository repo) {
    return new CurrentAccountAuthenticator(tokens, store, repo);
  }
}
