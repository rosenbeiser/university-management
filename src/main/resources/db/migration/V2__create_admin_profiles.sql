-- V2: Create admins profile table
CREATE TABLE admins (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT          NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    department      VARCHAR(100),
    CONSTRAINT pk_admins PRIMARY KEY (id),
    CONSTRAINT uq_admins_user_account UNIQUE (user_account_id),
    CONSTRAINT fk_admins_user_account
        FOREIGN KEY (user_account_id)
        REFERENCES user_accounts (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_admins_user_account_id ON admins (user_account_id);
