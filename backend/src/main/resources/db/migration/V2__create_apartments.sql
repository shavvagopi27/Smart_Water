CREATE TABLE apartments (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    address    VARCHAR(255) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
