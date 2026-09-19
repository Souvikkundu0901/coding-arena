-- Core Database Schema for Coding Arena

-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rating INT DEFAULT 1200,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Problems Table
CREATE TABLE problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    difficulty VARCHAR(50) NOT NULL, -- EASY / MEDIUM / HARD
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Test Cases Table
CREATE TABLE test_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_sample BOOLEAN DEFAULT FALSE
);

-- 4. Matches Table
-- Note: matches.winner_id must only ever be set through an atomic conditional update:
-- UPDATE matches SET winner_id = ? WHERE id = ? AND winner_id IS NULL
CREATE TABLE matches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_a_id UUID NOT NULL REFERENCES users(id),
    player_b_id UUID NOT NULL REFERENCES users(id),
    problem_id UUID NOT NULL REFERENCES problems(id),
    winner_id UUID REFERENCES users(id),
    status VARCHAR(50) NOT NULL, -- WAITING / IN_PROGRESS / COMPLETED
    started_at TIMESTAMP,
    ended_at TIMESTAMP
);

-- 5. Submissions Table
CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL REFERENCES matches(id),
    user_id UUID NOT NULL REFERENCES users(id),
    code TEXT NOT NULL,
    language VARCHAR(50) NOT NULL,
    verdict VARCHAR(50) NOT NULL, -- ACCEPTED / WRONG_ANSWER / TLE / RE / PENDING
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
