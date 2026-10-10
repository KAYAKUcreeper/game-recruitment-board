package com.gamerecruitment.account.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
  record StoredAccount(
      UUID id, String loginId, String displayName, String passwordHash, Instant createdAt) {}

  void save(StoredAccount account);

  Optional<StoredAccount> findByLoginId(String loginId);

  Optional<StoredAccount> findById(UUID id);
}
