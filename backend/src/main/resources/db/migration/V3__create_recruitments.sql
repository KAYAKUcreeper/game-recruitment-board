CREATE TABLE recruitments (
 id uuid PRIMARY KEY,
 organizer_id uuid NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
 game varchar(20) NOT NULL CHECK (game IN ('VALORANT', 'APEX_LEGENDS', 'OVERWATCH')),
 title varchar(100) NOT NULL,
 description varchar(2000) NOT NULL,
 starts_at timestamptz NOT NULL,
 capacity smallint NOT NULL CHECK (capacity BETWEEN 1 AND 9),
 meeting_info varchar(1000) NOT NULL DEFAULT '',
 manually_closed boolean NOT NULL DEFAULT false,
 cancelled boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL
);

CREATE INDEX recruitments_public_order_idx
 ON recruitments (starts_at, created_at, id);
CREATE INDEX recruitments_game_public_order_idx
 ON recruitments (game, starts_at, created_at, id);
CREATE INDEX recruitments_organizer_order_idx
 ON recruitments (organizer_id, starts_at, created_at, id);
