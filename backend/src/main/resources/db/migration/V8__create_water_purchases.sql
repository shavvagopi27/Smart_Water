CREATE TABLE water_purchases (
    id               BIGSERIAL PRIMARY KEY,
    apartment_id     BIGINT        NOT NULL REFERENCES apartments (id) ON DELETE CASCADE,
    billing_cycle_id BIGINT        REFERENCES billing_cycles (id) ON DELETE SET NULL,
    supplier         VARCHAR(150)  NOT NULL,
    supply_type      VARCHAR(20)   NOT NULL CHECK (supply_type IN ('TANKER', 'MUNICIPAL')),
    purchase_date    DATE          NOT NULL,
    volume_kl        NUMERIC(12,3) NOT NULL CHECK (volume_kl > 0),
    unit_cost        NUMERIC(12,2) NOT NULL CHECK (unit_cost >= 0),
    total_cost       NUMERIC(14,2) NOT NULL CHECK (total_cost >= 0),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_purchases_apartment_date ON water_purchases (apartment_id, purchase_date);
CREATE INDEX idx_purchases_cycle ON water_purchases (billing_cycle_id);
