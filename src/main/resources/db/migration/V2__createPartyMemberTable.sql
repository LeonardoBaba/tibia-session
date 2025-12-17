CREATE TABLE party_member (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    loot BIGINT,
    supplies BIGINT,
    balance BIGINT,
    damage BIGINT,
    healing BIGINT,
    party_session_id UUID,
    CONSTRAINT fk_party_session
      FOREIGN KEY(party_session_id)
      REFERENCES party_session(id)
);