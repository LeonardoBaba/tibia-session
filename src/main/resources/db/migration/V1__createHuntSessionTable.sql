CREATE TABLE party_session (
    id UUID PRIMARY KEY,
    start_time VARCHAR(255),
    end_time VARCHAR(255),
    session_duration VARCHAR(255),
    loot BIGINT,
    supplies BIGINT,
    balance BIGINT,
    input_session TEXT,
    processed_message TEXT
);
