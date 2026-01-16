CREATE TABLE IF NOT EXISTS events (
                                      event_id        VARCHAR(64) PRIMARY KEY,
    machine_id      VARCHAR(64) NOT NULL,
    event_time      TIMESTAMPTZ NOT NULL,
    received_time   TIMESTAMPTZ NOT NULL,
    duration_ms     BIGINT NOT NULL,
    defect_count    INT NOT NULL,
    payload_hash    VARCHAR(64) NOT NULL,
    factory_id      VARCHAR(32),
    line_id         VARCHAR(32)
    );

CREATE INDEX IF NOT EXISTS idx_events_machine_time
    ON events(machine_id, event_time);
