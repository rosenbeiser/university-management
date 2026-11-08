-- V5: Create refresh_tokens table for JWT revocation
CREATE TABLE refresh_tokens (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT          NOT NULL,
    token           VARCHAR(512)    NOT NULL,
    expires_at      DATETIME(6)     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user_account
        FOREIGN KEY (user_account_id)
        REFERENCES user_accounts (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user_account_id ON refresh_tokens (user_account_id);
CREATE INDEX idx_refresh_tokens_token           ON refresh_tokens (token(255));
