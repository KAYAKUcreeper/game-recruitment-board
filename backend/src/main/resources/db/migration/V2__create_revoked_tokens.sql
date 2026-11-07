CREATE TABLE revoked_tokens (
 jti uuid PRIMARY KEY,
 expires_at timestamptz NOT NULL,
 revoked_at timestamptz NOT NULL
);
CREATE INDEX revoked_tokens_expiry_idx ON revoked_tokens (expires_at);
