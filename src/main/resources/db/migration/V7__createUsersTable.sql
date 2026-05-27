CREATE TABLE users (
    id UUID PRIMARY KEY,
    discord_id VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(255),
    avatar_url VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_discord_id ON users (discord_id);
