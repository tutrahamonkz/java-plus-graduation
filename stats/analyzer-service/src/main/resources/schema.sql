CREATE TABLE IF NOT EXISTS user_actions (
    user_action_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT,
    event_id BIGINT,
    action_type VARCHAR,
    created TIMESTAMP
);

CREATE TABLE IF NOT EXISTS event_actions (
    event_a BIGINT,
    event_b BIGINT,
    score_event DOUBLE PRECISION,
    created TIMESTAMP,
    CONSTRAINT pk_event_actions PRIMARY KEY (event_a, event_b)
);