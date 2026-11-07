CREATE TABLE participations (
 recruitment_id uuid NOT NULL REFERENCES recruitments(id) ON DELETE RESTRICT,
 account_id uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
 joined_at timestamptz NOT NULL,
 PRIMARY KEY (recruitment_id, account_id)
);

CREATE INDEX participations_account_recruitment_idx
 ON participations (account_id, recruitment_id);
