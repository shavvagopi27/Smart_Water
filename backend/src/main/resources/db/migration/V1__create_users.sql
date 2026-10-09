-- Users (admins and residents). apartment_id / household_id foreign keys are added in V3
-- because those tables do not exist yet at this point.
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'RESIDENT')),
    apartment_id  BIGINT,
    household_id  BIGINT,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_role ON users (role);
