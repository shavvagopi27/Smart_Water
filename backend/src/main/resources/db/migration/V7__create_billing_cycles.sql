CREATE TABLE billing_cycles (
    id           BIGSERIAL PRIMARY KEY,
    apartment_id BIGINT       NOT NULL REFERENCES apartments (id) ON DELETE CASCADE,
    name         VARCHAR(120) NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE         NOT NULL,
    status       VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN', 'FINALIZED', 'ARCHIVED')),
    finalized_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_cycle_dates CHECK (end_date >= start_date)
);
CREATE INDEX idx_cycles_apartment_dates ON billing_cycles (apartment_id, start_date, end_date);
