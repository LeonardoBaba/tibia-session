ALTER TABLE party_session ADD COLUMN name VARCHAR(255);
ALTER TABLE party_session ADD COLUMN comment TEXT;
ALTER TABLE party_session ADD COLUMN owner_discord_id VARCHAR(255);

ALTER TABLE party_session
    ALTER COLUMN start_time TYPE TIMESTAMP
        USING TO_TIMESTAMP(NULLIF(start_time, ''), 'YYYY-MM-DD, HH24:MI:SS');

ALTER TABLE party_session
    ALTER COLUMN end_time TYPE TIMESTAMP
        USING TO_TIMESTAMP(NULLIF(end_time, ''), 'YYYY-MM-DD, HH24:MI:SS');
