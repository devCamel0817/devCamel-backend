CREATE TABLE content_script (
    id          BIGSERIAL PRIMARY KEY,
    target_date DATE         NOT NULL,
    headlines   TEXT,
    script      TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_content_script_target_date ON content_script (target_date DESC);

