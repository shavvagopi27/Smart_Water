CREATE TABLE tariff_plans (
    id             BIGSERIAL PRIMARY KEY,
    apartment_id   BIGINT       NOT NULL REFERENCES apartments (id) ON DELETE CASCADE,
    name           VARCHAR(120) NOT NULL,
    effective_from DATE         NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_tariff_plans_apartment ON tariff_plans (apartment_id, active);

CREATE TABLE tariff_tiers (
    id             BIGSERIAL PRIMARY KEY,
    tariff_plan_id BIGINT        NOT NULL REFERENCES tariff_plans (id) ON DELETE CASCADE,
    tier_order     INTEGER       NOT NULL CHECK (tier_order > 0),
    from_kl        NUMERIC(12,3) NOT NULL CHECK (from_kl >= 0),
    to_kl          NUMERIC(12,3),                     -- NULL = open ended top tier
    rate_per_kl    NUMERIC(12,2) NOT NULL CHECK (rate_per_kl >= 0),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_tier_range CHECK (to_kl IS NULL OR to_kl > from_kl),
    CONSTRAINT uq_tier_order UNIQUE (tariff_plan_id, tier_order)
);
