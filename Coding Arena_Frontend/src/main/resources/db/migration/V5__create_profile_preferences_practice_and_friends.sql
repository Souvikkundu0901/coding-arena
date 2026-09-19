ALTER TABLE users ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    theme VARCHAR NOT NULL DEFAULT 'dark',
    email_notifications BOOLEAN NOT NULL DEFAULT true
);

ALTER TABLE submissions ALTER COLUMN match_id DROP NOT NULL;
ALTER TABLE submissions ADD COLUMN problem_id UUID REFERENCES problems(id);
UPDATE submissions SET problem_id = (SELECT problem_id FROM matches WHERE matches.id = submissions.match_id) WHERE match_id IS NOT NULL;

CREATE TABLE friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL REFERENCES users(id),
    addressee_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT now(),
    responded_at TIMESTAMP
);
