package com.gamerecruitment.account.adapter.persistence;

import com.gamerecruitment.account.application.port.RevokedTokenStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRevokedTokenStore implements RevokedTokenStore {
  private final JdbcClient jdbc;

  public JdbcRevokedTokenStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  public void revoke(UUID jti, Instant expiresAt, Instant revokedAt) {
    jdbc.sql(
            "insert into revoked_tokens (jti,expires_at,revoked_at) values (:jti,:expiry,:now) on"
                + " conflict (jti) do nothing")
        .param("jti", jti)
        .param("expiry", Timestamp.from(expiresAt))
        .param("now", Timestamp.from(revokedAt))
        .update();
  }

  public boolean isRevoked(UUID jti) {
    return jdbc.sql("select exists(select 1 from revoked_tokens where jti=:jti)")
        .param("jti", jti)
        .query(Boolean.class)
        .single();
  }

  public int deleteExpired(Instant now) {
    return jdbc.sql("delete from revoked_tokens where expires_at<=:now")
        .param("now", Timestamp.from(now))
        .update();
  }
}
