-- =============================================================================
-- V7__password_reset_token.sql  --  Password reset token (EP-BE-02 reopened)
-- =============================================================================
-- Single-use, short-lived tokens emitted by POST /auth/password-reset and
-- consumed by POST /auth/password-reset/confirm. Tokens are random 256-bit
-- strings (Base64-URL); the database stores them in plain text because their
-- TTL is short (RFC 6.2 acceptable per §6.2.3). user_id has no FK to users
-- (RFC 5.2.1, no REFERENCES); referential integrity enforced by the service.
-- =============================================================================

CREATE TABLE password_reset_token (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token (user_id);
CREATE INDEX idx_password_reset_token_expires ON password_reset_token (expires_at);
