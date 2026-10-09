CREATE TABLE alerts (
    id           BIGSERIAL PRIMARY KEY,
    household_id BIGINT       NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    alert_type   VARCHAR(30)  NOT NULL CHECK (alert_type IN ('HIGH_CONSUMPTION', 'THRESHOLD_EXCEEDED', 'ABNORMAL_USAGE', 'POTENTIAL_LEAKAGE')),
    severity     VARCHAR(10)  NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    message      VARCHAR(500) NOT NULL,
    usage_date   DATE         NOT NULL,
    read_flag    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_alert_once UNIQUE (household_id, alert_type, usage_date)
);
CREATE INDEX idx_alerts_household_read ON alerts (household_id, read_flag);
