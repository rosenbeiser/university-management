-- V3: Create teachers profile table
CREATE TABLE teachers (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT          NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    department      VARCHAR(100)    NOT NULL,
    designation     VARCHAR(100),
    phone           VARCHAR(20),
    CONSTRAINT pk_teachers PRIMARY KEY (id),
    CONSTRAINT uq_teachers_user_account UNIQUE (user_account_id),
    CONSTRAINT fk_teachers_user_account
        FOREIGN KEY (user_account_id)
        REFERENCES user_accounts (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_teachers_user_account_id ON teachers (user_account_id);
CREATE INDEX idx_teachers_department       ON teachers (department);
