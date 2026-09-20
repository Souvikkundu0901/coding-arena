-- Coding Arena — Problem Bank Seed Script
-- Run this once in Supabase SQL Editor.
-- 18 classic DSA problems: 7 Easy, 7 Medium, 4 Hard.
-- Each has 3-4 real test cases (not just samples).
-- Note: output format is a single line of plain text per test case,
-- matching what SubmissionService compares against stdout.

-- ============ EASY ============

-- 1. Two Sum
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Two Sum', 'EASY',
    'Given an array of integers nums and an integer target, print the indices of the two numbers that add up to target, space-separated. Input format: first line is the array (space-separated), second line is the target.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('2 7 11 15' || E'\n' || '9', '0 1', true),
  ('3 2 4' || E'\n' || '6', '1 2', true),
  ('3 3' || E'\n' || '6', '0 1', false)
) AS i(input, expected, is_sample);

-- 2. Reverse String
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Reverse String', 'EASY',
    'Given a string, print it reversed.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('hello', 'olleh', true),
  ('a', 'a', true),
  ('racecar', 'racecar', false),
  ('coding arena', 'anera gnidoc', false)
) AS i(input, expected, is_sample);

-- 3. FizzBuzz
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'FizzBuzz', 'EASY',
    'Given an integer n, print numbers 1 to n, one per line. For multiples of 3 print "Fizz", multiples of 5 print "Buzz", multiples of both print "FizzBuzz".')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('5', '1' || E'\n' || '2' || E'\n' || 'Fizz' || E'\n' || '4' || E'\n' || 'Buzz', true),
  ('15', '1' || E'\n' || '2' || E'\n' || 'Fizz' || E'\n' || '4' || E'\n' || 'Buzz' || E'\n' || 'Fizz' || E'\n' || '7' || E'\n' || '8' || E'\n' || 'Fizz' || E'\n' || 'Buzz' || E'\n' || '11' || E'\n' || 'Fizz' || E'\n' || '13' || E'\n' || '14' || E'\n' || 'FizzBuzz', false)
) AS i(input, expected, is_sample);

-- 4. Valid Palindrome
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Valid Palindrome', 'EASY',
    'Given a string, print "true" if it is a palindrome (reads the same forwards and backwards), else print "false".')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('racecar', 'true', true),
  ('hello', 'false', true),
  ('a', 'true', false),
  ('noon', 'true', false)
) AS i(input, expected, is_sample);

-- 5. Maximum Subarray Sum (Kadane's, simplified)
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Maximum Subarray Sum', 'EASY',
    'Given an array of integers, print the sum of the contiguous subarray with the largest sum.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('-2 1 -3 4 -1 2 1 -5 4', '6', true),
  ('1', '1', true),
  ('5 4 -1 7 8', '23', false)
) AS i(input, expected, is_sample);

-- 6. Count Vowels
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Count Vowels', 'EASY',
    'Given a lowercase string, print the count of vowels (a, e, i, o, u) in it.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('coding', '2', true),
  ('xyz', '0', true),
  ('aeiou', '5', false)
) AS i(input, expected, is_sample);

-- 7. Factorial
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Factorial', 'EASY',
    'Given a non-negative integer n, print n! (factorial).')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('5', '120', true),
  ('0', '1', true),
  ('10', '3628800', false)
) AS i(input, expected, is_sample);

-- ============ MEDIUM ============

-- 8. Longest Common Prefix
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Longest Common Prefix', 'MEDIUM',
    'Given space-separated words on one line, print the longest common prefix among them. Print empty string if none.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('flower flow flight', 'fl', true),
  ('dog racecar car', '', true),
  ('interview internet interval', 'inte', false)
) AS i(input, expected, is_sample);

-- 9. Valid Parentheses
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Valid Parentheses', 'MEDIUM',
    'Given a string containing only ()[]{}, print "true" if brackets are validly matched and nested, else "false".')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('()', 'true', true),
  ('()[]{}', 'true', true),
  ('(]', 'false', false),
  ('([)]', 'false', false)
) AS i(input, expected, is_sample);

-- 10. Binary Search
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Binary Search', 'MEDIUM',
    'Given a sorted array (space-separated, line 1) and a target (line 2), print the index of target, or -1 if not found.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('1 3 5 7 9 11' || E'\n' || '7', '3', true),
  ('1 3 5 7 9 11' || E'\n' || '4', '-1', true),
  ('2 4 6 8 10' || E'\n' || '2', '0', false)
) AS i(input, expected, is_sample);

-- 11. Merge Two Sorted Arrays
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Merge Two Sorted Arrays', 'MEDIUM',
    'Given two sorted arrays of integers (space-separated, one per line), print the merged sorted array, space-separated.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('1 3 5' || E'\n' || '2 4 6', '1 2 3 4 5 6', true),
  ('1 2 3' || E'\n' || E'\n', '1 2 3', false)
) AS i(input, expected, is_sample);

-- 12. First Non-Repeating Character
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'First Non-Repeating Character', 'MEDIUM',
    'Given a lowercase string, print the first character that does not repeat. If none, print "_".')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('swiss', 'w', true),
  ('aabbcc', '_', true),
  ('coding', 'c', false)
) AS i(input, expected, is_sample);

-- 13. Fibonacci Nth Term
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Fibonacci Nth Term', 'MEDIUM',
    'Given n, print the nth Fibonacci number (0-indexed: fib(0)=0, fib(1)=1).')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('10', '55', true),
  ('0', '0', true),
  ('20', '6765', false)
) AS i(input, expected, is_sample);

-- 14. Rotate Array
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Rotate Array', 'MEDIUM',
    'Given an array (line 1, space-separated) and k (line 2), rotate the array right by k steps and print it, space-separated.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('1 2 3 4 5 6 7' || E'\n' || '3', '5 6 7 1 2 3 4', true),
  ('1 2 3' || E'\n' || '1', '3 1 2', false)
) AS i(input, expected, is_sample);

-- ============ HARD ============

-- 15. Longest Palindromic Substring
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Longest Palindromic Substring', 'HARD',
    'Given a string, print the longest palindromic substring. If multiple, print the first one found.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('babad', 'bab', true),
  ('cbbd', 'bb', true)
) AS i(input, expected, is_sample);

-- 16. Merge Intervals
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Merge Intervals', 'HARD',
    'Given intervals as pairs on one line like "1,3 2,6 8,10 15,18", merge overlapping intervals and print the result in the same format, sorted by start.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('1,3 2,6 8,10 15,18', '1,6 8,10 15,18', true),
  ('1,4 4,5', '1,5', false)
) AS i(input, expected, is_sample);

-- 17. Word Break
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Word Break', 'HARD',
    'Given a string (line 1) and a space-separated dictionary of words (line 2), print "true" if the string can be segmented into dictionary words, else "false".')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('leetcode' || E'\n' || 'leet code', 'true', true),
  ('catsandog' || E'\n' || 'cats dog sand and cat', 'false', false)
) AS i(input, expected, is_sample);

-- 18. Kth Largest Element
WITH p AS (
  INSERT INTO problems (id, title, difficulty, description)
  VALUES (gen_random_uuid(), 'Kth Largest Element', 'HARD',
    'Given an array (line 1, space-separated) and k (line 2), print the kth largest element in the array.')
  RETURNING id
)
INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample)
SELECT gen_random_uuid(), p.id, i.input, i.expected, i.is_sample FROM p, (VALUES
  ('3 2 1 5 6 4' || E'\n' || '2', '5', true),
  ('3 2 3 1 2 4 5 5 6' || E'\n' || '4', '4', false)
) AS i(input, expected, is_sample);
