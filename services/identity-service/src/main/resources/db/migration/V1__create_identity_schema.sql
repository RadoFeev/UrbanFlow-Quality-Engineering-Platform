CREATE TABLE user_accounts (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_accounts_email UNIQUE (email),
    CONSTRAINT ck_user_accounts_email_lowercase CHECK (email = LOWER(email)),
    CONSTRAINT ck_user_accounts_role CHECK (role IN ('CUSTOMER', 'COURIER', 'OPERATOR', 'ADMIN')),
    CONSTRAINT ck_user_accounts_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_user_accounts_failed_attempts CHECK (failed_login_attempts >= 0),
    CONSTRAINT ck_user_accounts_version CHECK (version >= 0),
    CONSTRAINT ck_user_accounts_lock_state CHECK (
        (status = 'LOCKED' AND locked_until IS NOT NULL)
        OR status <> 'LOCKED'
    )
);

CREATE TABLE refresh_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT uq_refresh_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_sessions_lifetime CHECK (expires_at > issued_at),
    CONSTRAINT ck_refresh_sessions_revocation CHECK (revoked_at IS NULL OR revoked_at >= issued_at)
);

CREATE INDEX idx_refresh_sessions_user_active
    ON refresh_sessions (user_id, expires_at)
    WHERE revoked_at IS NULL;

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_active
    ON password_reset_tokens (user_id, expires_at)
    WHERE used_at IS NULL;
