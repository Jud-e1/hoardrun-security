CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT now(),
    type VARCHAR(64) NOT NULL,
    principal VARCHAR(190) NOT NULL,
    details_json TEXT
);
