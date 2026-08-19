CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    avatar_url VARCHAR(1024),
    bio VARCHAR(1024),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX ix_users_deleted_at ON users(deleted_at);