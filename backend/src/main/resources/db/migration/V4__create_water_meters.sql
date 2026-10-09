CREATE TABLE water_meters (
    id                BIGSERIAL PRIMARY KEY,
    meter_number      VARCHAR(50)   NOT NULL,
    household_id      BIGINT        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    installation_date DATE          NOT NULL,
    status            VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'FAULTY')),
    initial_reading   NUMERIC(12,3) NOT NULL CHECK (initial_reading >= 0),
    current_reading   NUMERIC(12,3) NOT NULL CHECK (current_reading >= 0),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_meter_number UNIQUE (meter_number),
    CONSTRAINT uq_meter_household UNIQUE (household_id)
);
