ALTER TABLE users ALTER COLUMN rating SET DEFAULT 0;
UPDATE users SET rating = 0 WHERE rating = 1200 AND wins = 0 AND losses = 0;
