-- V1: Create user_accounts table
CREATE TABLE user_accounts (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)     NOT NULL,
    email        VARCHAR(100)    NOT NULL,
    password_hash VARCHAR(255)   NOT NULL,
    role         ENUM('ADMIN','TEACHER','STUDENT') NOT NULL,
    enabled      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_user_accounts PRIMARY KEY (id),
    CONSTRAINT uq_user_accounts_username UNIQUE (username),
    CONSTRAINT uq_user_accounts_email    UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_user_accounts_username ON user_accounts (username);
CREATE INDEX idx_user_accounts_email    ON user_accounts (email);
CREATE INDEX idx_user_accounts_role     ON user_accounts (role);
