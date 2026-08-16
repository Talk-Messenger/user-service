CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    avatar_url VARCHAR(1024),
    bio VARCHAR(1024),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX ux_users_username(username);
CREATE INDEX ix_users_deleted_at(deleted_at);