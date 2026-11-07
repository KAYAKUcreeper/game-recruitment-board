CREATE TABLE accounts (
 id uuid PRIMARY KEY,
 login_id varchar(30) NOT NULL UNIQUE CHECK (login_id ~ '^[a-z0-9_]{3,30}$'),
 display_name varchar(30) NOT NULL CHECK (char_length(display_name) BETWEEN 1 AND 30),
 password_hash text NOT NULL,
 created_at timestamptz NOT NULL
);
