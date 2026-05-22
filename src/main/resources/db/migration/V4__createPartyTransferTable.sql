CREATE TABLE party_transfer (
    id UUID PRIMARY KEY,
    from_player VARCHAR(255),
    to_player VARCHAR(255),
    amount BIGINT,
    party_session_id UUID,
    CONSTRAINT fk_party_session_transfer
      FOREIGN KEY(party_session_id)
      REFERENCES party_session(id)
);
