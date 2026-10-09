CREATE TABLE households (
    id             BIGSERIAL PRIMARY KEY,
    apartment_id   BIGINT        NOT NULL REFERENCES apartments (id),
    flat_number    VARCHAR(20)   NOT NULL,
    household_size INTEGER       NOT NULL CHECK (household_size > 0),
    area_sqft      NUMERIC(10,2) NOT NULL CHECK (area_sqft > 0),
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_household_flat UNIQUE (apartment_id, flat_number)
);
CREATE INDEX idx_households_apartment ON households (apartment_id);

ALTER TABLE users ADD CONSTRAINT fk_users_apartment FOREIGN KEY (apartment_id) REFERENCES apartments (id) ON DELETE SET NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_household FOREIGN KEY (household_id) REFERENCES households (id) ON DELETE SET NULL;
CREATE INDEX idx_users_household ON users (household_id);
