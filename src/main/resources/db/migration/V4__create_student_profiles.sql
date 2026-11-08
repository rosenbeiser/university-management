-- V4: Create students profile table
CREATE TABLE students (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_account_id BIGINT          NOT NULL,
    full_name       VARCHAR(100)    NOT NULL,
    student_number  VARCHAR(30)     NOT NULL,
    department      VARCHAR(100)    NOT NULL,
    semester        INT,
    phone           VARCHAR(20),
    CONSTRAINT pk_students PRIMARY KEY (id),
    CONSTRAINT uq_students_user_account  UNIQUE (user_account_id),
    CONSTRAINT uq_students_student_number UNIQUE (student_number),
    CONSTRAINT fk_students_user_account
        FOREIGN KEY (user_account_id)
        REFERENCES user_accounts (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_students_user_account_id ON students (user_account_id);
CREATE INDEX idx_students_student_number  ON students (student_number);
CREATE INDEX idx_students_department      ON students (department);
