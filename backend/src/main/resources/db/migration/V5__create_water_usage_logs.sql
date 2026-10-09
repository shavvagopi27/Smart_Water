CREATE TABLE water_usage_logs (
    id           BIGSERIAL PRIMARY KEY,
    household_id BIGINT        NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    meter_id     BIGINT        NOT NULL REFERENCES water_meters (id) ON DELETE CASCADE,
    reading_date DATE          NOT NULL,
    meter_reading NUMERIC(12,3) NOT NULL CHECK (meter_reading >= 0),
    consumption  NUMERIC(12,3) NOT NULL CHECK (consumption >= 0),
    source       VARCHAR(10)   NOT NULL CHECK (source IN ('MANUAL', 'CSV')),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_usage_meter_date UNIQUE (meter_id, reading_date)
);
CREATE INDEX idx_usage_household_date ON water_usage_logs (household_id, reading_date);
