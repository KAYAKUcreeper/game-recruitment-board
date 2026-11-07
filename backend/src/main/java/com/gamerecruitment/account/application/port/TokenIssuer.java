package com.gamerecruitment.account.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TokenIssuer {
  record Details(UUID accountId, UUID jti, Instant issuedAt, Instant expiresAt) {}

  record Issued(String value, Details details) {}

  Issued issue(UUID accountId);

  Optional<Details> validate(String value);
}
