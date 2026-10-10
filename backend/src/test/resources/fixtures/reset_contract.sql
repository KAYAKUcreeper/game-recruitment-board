-- Only the dedicated contract database can be reset. Development data is preserved.
DO $$
BEGIN
  IF current_database() <> 'game_recruitment_contract' THEN
    RAISE EXCEPTION 'Reset is permitted only for game_recruitment_contract';
  END IF;
END
$$;
TRUNCATE TABLE revoked_tokens, accounts;
