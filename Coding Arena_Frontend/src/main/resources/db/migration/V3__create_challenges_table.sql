CREATE TABLE challenges (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  challenger_id UUID NOT NULL REFERENCES users(id),
  challenged_id UUID NOT NULL REFERENCES users(id),
  status VARCHAR NOT NULL DEFAULT 'PENDING',
  match_id UUID REFERENCES matches(id),
  created_at TIMESTAMP DEFAULT now(),
  responded_at TIMESTAMP
);
