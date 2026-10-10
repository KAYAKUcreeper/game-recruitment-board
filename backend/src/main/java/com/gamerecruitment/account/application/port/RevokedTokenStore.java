package com.gamerecruitment.account.application.port;

import java.time.Instant;
import java.util.UUID;

public interface RevokedTokenStore {
  void revoke(UUID jti, Instant expiresAt, Instant revokedAt);

  boolean isRevoked(UUID jti);

  int deleteExpired(Instant now);
}
