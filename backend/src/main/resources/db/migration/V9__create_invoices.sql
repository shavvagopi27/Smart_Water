CREATE TABLE invoices (
    id                BIGSERIAL PRIMARY KEY,
    invoice_number    VARCHAR(50)   NOT NULL,
    household_id      BIGINT        NOT NULL REFERENCES households (id),
    billing_cycle_id  BIGINT        NOT NULL REFERENCES billing_cycles (id),
    resident_name     VARCHAR(255)  NOT NULL,
    previous_reading  NUMERIC(12,3) NOT NULL,
    current_reading   NUMERIC(12,3) NOT NULL,
    consumption       NUMERIC(12,3) NOT NULL CHECK (consumption >= 0),
    water_charge      NUMERIC(14,2) NOT NULL CHECK (water_charge >= 0),
    shared_cost       NUMERIC(14,2) NOT NULL CHECK (shared_cost >= 0),
    adjustment        NUMERIC(14,2) NOT NULL DEFAULT 0,
    adjustment_reason VARCHAR(255),
    total_amount      NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
    shared_cost_basis VARCHAR(20)   NOT NULL CHECK (shared_cost_basis IN ('METERED', 'AREA_FALLBACK', 'NONE')),
    payment_status    VARCHAR(10)   NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PENDING', 'PAID')),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_invoice_number UNIQUE (invoice_number),
    CONSTRAINT uq_invoice_household_cycle UNIQUE (household_id, billing_cycle_id)
);
CREATE INDEX idx_invoices_household ON invoices (household_id);
CREATE INDEX idx_invoices_cycle ON invoices (billing_cycle_id);

CREATE TABLE invoice_items (
    id          BIGSERIAL PRIMARY KEY,
    invoice_id  BIGINT        NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    item_type   VARCHAR(20)   NOT NULL CHECK (item_type IN ('TARIFF_TIER', 'SHARED_COST', 'ADJUSTMENT')),
    description VARCHAR(255)  NOT NULL,
    quantity    NUMERIC(12,3),
    rate        NUMERIC(12,2),
    amount      NUMERIC(14,2) NOT NULL,
    sort_order  INTEGER       NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoice_items_invoice ON invoice_items (invoice_id);
