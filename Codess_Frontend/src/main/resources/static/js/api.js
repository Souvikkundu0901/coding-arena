const MOCK_MODE = false; // flip to false when backend is ready
const CODESS_DEFAULT_REMOTE_BACKEND = "https://codess-8rrg.onrender.com";

function getCodessBackendOrigin() {
  if (typeof window !== "undefined") {
    if (window.CODESS_BACKEND_URL) {
      return window.CODESS_BACKEND_URL.replace(/\/$/, "");
    }
    const stored = localStorage.getItem("codess_backend_url");
    if (stored) {
      return stored.replace(/\/$/, "");
    }
    if (window.location && window.location.hostname) {
      const host = window.location.hostname;
      if (host === "localhost" || host === "127.0.0.1") {
        return window.location.port === "8080" ? window.location.origin : "http://localhost:8080";
      }
      if (host.endsWith("onrender.com")) {
        return window.location.origin;
      }
      return CODESS_DEFAULT_REMOTE_BACKEND;
    }
  }
  return "http://localhost:8080";
}

const API_BASE = `${getCodessBackendOrigin()}/api`;

/**
 * Get the JWT token stored in localStorage.
 * @returns {string|null} The stored JWT token, or null if not found.
 */
function getToken() {
  return localStorage.getItem("ca_token");
}

/**
 * Save the JWT token to localStorage.
 * @param {string} token - JWT token to store.
 * @returns {void}
 */
function setToken(token) {
  localStorage.setItem("ca_token", token);
}

/**
 * Remove the JWT token from localStorage.
 * @returns {void}
 */
function clearToken() {
  localStorage.removeItem("ca_token");
}

/**
 * Make an API request to the Coding Arena backend.
 * @param {string} endpoint - API endpoint, e.g. "/auth/login".
 * @param {string} [method="GET"] - HTTP method.
 * @param {Object|null} [body=null] - Request body.
 * @param {boolean} [authRequired=true] - Whether a JWT is required.
 * @returns {Promise<Object>} Parsed JSON response.
 * @throws {Error} When the request fails.
 */
async function apiRequest(
  endpoint,
  method = "GET",
  body = null,
  authRequired = true
) {
  const headers = {
    "Content-Type": "application/json"
  };

  if (authRequired) {
    const token = getToken();

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  const options = {
    method,
    headers
  };

  if (body !== null) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_BASE}${endpoint}`, options);

  let data = null;

  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    throw new Error(
      data?.error || "Something went wrong"
    );
  }

  return data;
}

/**
 * Mock user accounts for testing multi-user flows (login, friends, etc.)
 * before the real backend exists.
 */
const MOCK_USERS = [
  { id: 1, username: "testuser", email: "test@test.com", rating: 1450, wins: 128, losses: 42, avatarSeed: "CodeKnight" },
  { id: 2, username: "byte_bandit", email: "bandit@test.com", rating: 1510, wins: 90, losses: 30, avatarSeed: "ByteBandit" },
  { id: 3, username: "stack_sorcerer", email: "sorcerer@test.com", rating: 1380, wins: 60, losses: 55, avatarSeed: "StackSorcerer" }
];

/**
 * Register a new Coding Arena user.
 * @param {string} username - Username.
 * @param {string} email - User email.
 * @param {string} password - User password.
 * @param {string} [captchaToken] - Google reCAPTCHA response token.
 * @returns {Promise<Object>} Registration response.
 */
async function registerUser(username, email, password, captchaToken) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 500));
    return {
      token: "fake.jwt.token",
      user: { id: 1, username, email, rating: 0, wins: 0, losses: 0, createdAt: new Date().toISOString() }
    };
  } else {
    try {
      const payload = { username, email, password };
      if (captchaToken) {
        payload.captchaToken = captchaToken;
      }
      return await apiRequest(
        "/auth/register",
        "POST",
        payload,
        false
      );
    } catch (error) {
      throw new Error(`Registration failed: ${error.message}`);
    }
  }
}

/**
 * Log in an existing Coding Arena user.
 * @param {string} email - User email.
 * @param {string} password - User password.
 * @returns {Promise<Object>} Login response containing authentication data.
 */
async function loginUser(email, password) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 500));

    const matchedUser = MOCK_USERS.find(
      u => u.email.toLowerCase() === String(email).toLowerCase()
    ) || MOCK_USERS[0];

    return {
      token: `fake.jwt.token.${matchedUser.id}`,
      user: { ...matchedUser, email, createdAt: new Date().toISOString() }
    };
  } else {
    try {
      return await apiRequest("/auth/login", "POST", { email, password }, false);
    } catch (error) {
      throw new Error(`Login failed: ${error.message}`);
    }
  }
}

/**
 * Get the currently authenticated user.
 * @returns {Promise<Object>} Current user data.
 */
async function getCurrentUser() {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const storedUser = localStorage.getItem("ca_user");

    if (storedUser) {
      return JSON.parse(storedUser);
    }

    return { id: 1, username: "testuser", email: "test@test.com", rating: 1450, wins: 128, losses: 42, avatarSeed: "CodeKnight", createdAt: new Date().toISOString() };
  } else {
    try {
      return await apiRequest("/auth/me", "GET");
    } catch (error) {
      throw new Error(`Fetching current user failed: ${error.message}`);
    }
  }
}

/**
 * Join the matchmaking queue.
 * @returns {Promise<Object>} Queue response.
 */
async function joinQueue() {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));
    return { message: "Joined matchmaking queue successfully" };
  } else {
    try {
      return await apiRequest("/matchmaking/queue", "POST");
    } catch (error) {
      throw new Error(`Joining queue failed: ${error.message}`);
    }
  }
}

/**
 * Leave the matchmaking queue.
 * @returns {Promise<Object>} Queue response.
 */
async function leaveQueue() {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));
    return { message: "Left queue" };
  } else {
    try {
      return await apiRequest("/matchmaking/queue", "DELETE");
    } catch (error) {
      throw new Error(`Leaving queue failed: ${error.message}`);
    }
  }
}

/**
 * Get match details by match ID.
 * @param {string|number} matchId - Match ID.
 * @returns {Promise<Object>} Match data.
 */
async function getMatch(matchId) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 400));
    return {
      id: matchId,
      status: "IN_PROGRESS",
      players: [
        { id: 1, username: "testuser", rating: 1450 },
        { id: 2, username: "opponent123", rating: 1470 }
      ],
      problem: {
        title: "Two Sum",
        difficulty: "Easy",
        description: "Given an array of integers, return indices of the two numbers that add up to a target.",
        examples: "Input: [2,7,11,15], target=9\nOutput: [0,1]"
      },
      createdAt: new Date().toISOString()
    };
  } else {
    try {
      return await apiRequest(`/matches/${matchId}`, "GET");
    } catch (error) {
      throw new Error(`Fetching match failed: ${error.message}`);
    }
  }
}

/**
 * Check if the current user has an active match in progress.
 * @returns {Promise<Object>}
 */
async function getActiveMatch() {
  if (MOCK_MODE) {
    return { active: false };
  } else {
    try {
      return await apiRequest("/matches/active", "GET");
    } catch (error) {
      return { active: false };
    }
  }
}

/**
 * Submit code for a match.
 * @param {string|number} matchId - Match ID.
 * @param {string} code - Source code to submit.
 * @param {string} language - Programming language.
 * @returns {Promise<Object>} Submission response.
 */
async function submitCode(matchId, code, language) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 700));

    if (!code || !code.trim()) {
      return { verdict: "WRONG_ANSWER", message: "Empty submission." };
    }

    const verdicts = ["ACCEPTED", "ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED"];
    const verdict = verdicts[Math.floor(Math.random() * verdicts.length)];

    return {
      verdict,
      matchId,
      language,
      submittedAt: new Date().toISOString()
    };
  } else {
    try {
      return await apiRequest(
        `/matches/${matchId}/submissions`,
        "POST",
        { code, language }
      );
    } catch (error) {
      throw new Error(`Code submission failed: ${error.message}`);
    }
  }
}

/**
 * Get all submissions for a match.
 * @param {string|number} matchId - Match ID.
 * @returns {Promise<Object>} Submission data.
 */
async function getSubmissions(matchId) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));
    return [
      { id: 101, matchId, language: "python", verdict: "WRONG_ANSWER", submittedAt: new Date(Date.now() - 60000).toISOString() }
    ];
  } else {
    try {
      return await apiRequest(`/matches/${matchId}/submissions`, "GET");
    } catch (error) {
      throw new Error(`Fetching submissions failed: ${error.message}`);
    }
  }
}

/**
 * Get the current user's recent completed matches.
 * @param {number} [limit=4] - Max number of matches to return.
 * @returns {Promise<Array>} Recent match summaries.
 */
async function getRecentMatches(limit = 4) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));
    return [
      { id: 501, opponentUsername: "byte_bandit", result: "WIN", ratingDelta: 18 },
      { id: 502, opponentUsername: "stack_sorcerer", result: "LOSS", ratingDelta: -12 },
      { id: 503, opponentUsername: "null_pointer", result: "WIN", ratingDelta: 21 },
      { id: 504, opponentUsername: "recursion_rex", result: "WIN", ratingDelta: 15 }
    ].slice(0, limit);
  } else {
    try {
      return await apiRequest(`/users/me/matches?limit=${limit}`, "GET");
    } catch (error) {
      throw new Error(`Fetching recent matches failed: ${error.message}`);
    }
  }
}

/**
 * Update the current user's profile (username / avatar seed).
 * @param {Object} updates - Fields to update.
 * @param {string} [updates.username] - New username.
 * @param {string} [updates.avatarSeed] - New DiceBear avatar seed.
 * @returns {Promise<Object>} Updated user data.
 */
async function updateProfile(updates) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 400));

    const storedUser = JSON.parse(localStorage.getItem("ca_user") || "{}");
    const updatedUser = { ...storedUser, ...updates };
    localStorage.setItem("ca_user", JSON.stringify(updatedUser));
    return updatedUser;
  } else {
    try {
      const updatedUser = await apiRequest("/users/me", "PATCH", updates);
      if (updatedUser) {
        const storedUser = JSON.parse(localStorage.getItem("ca_user") || "{}");
        const merged = { ...storedUser, ...updatedUser };
        localStorage.setItem("ca_user", JSON.stringify(merged));
      }
      return updatedUser;
    } catch (error) {
      throw new Error(`Updating profile failed: ${error.message}`);
    }
  }
}

/**
 * Change the current user's email address.
 * @param {string} newEmail - The new email address.
 * @returns {Promise<Object>} Response confirming the change.
 */
async function changeEmail(newEmail, password) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 400));

    if (!newEmail || !newEmail.includes("@")) {
      throw new Error("Please enter a valid email address.");
    }

    return { message: "Email updated successfully", email: newEmail };
  } else {
    try {
      return await apiRequest("/users/me/email", "PATCH", { newEmail, password });
    } catch (error) {
      throw new Error(`Updating email failed: ${error.message}`);
    }
  }
}

/**
 * Change the current user's password.
 * @param {string} currentPassword - The user's current password.
 * @param {string} newPassword - The desired new password.
 * @returns {Promise<Object>} Response confirming the change.
 */
async function changePassword(currentPassword, newPassword) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 400));

    if (!currentPassword) {
      throw new Error("Please enter your current password.");
    }

    if (!newPassword || newPassword.length < 6) {
      throw new Error("New password must be at least 6 characters.");
    }

    return { message: "Password updated successfully" };
  } else {
    try {
      return await apiRequest("/users/me/password", "PATCH", {
        currentPassword,
        newPassword
      });
    } catch (error) {
      throw new Error(`Changing password failed: ${error.message}`);
    }
  }
}

/**
 * Update the current user's app preferences.
 * @param {Object} preferences - Preference fields to update.
 * @returns {Promise<Object>} Updated preferences.
 */
async function updatePreferences(preferences) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const stored = JSON.parse(localStorage.getItem("ca_preferences") || "{}");
    const updated = { ...stored, ...preferences };

    localStorage.setItem("ca_preferences", JSON.stringify(updated));

    return updated;
  } else {
    try {
      return await apiRequest("/users/me/preferences", "PATCH", preferences);
    } catch (error) {
      throw new Error(`Updating preferences failed: ${error.message}`);
    }
  }
}

/**
 * Get the current user's saved app preferences.
 * @returns {Promise<Object>} Stored preferences.
 */
async function getPreferences() {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 200));

    return JSON.parse(localStorage.getItem("ca_preferences") || "{}");
  } else {
    try {
      const prefs = await apiRequest("/users/me/preferences", "GET");
      // Cache locally so theme.js can apply theme on next page load without an API call
      try {
        localStorage.setItem("ca_preferences", JSON.stringify(prefs));
      } catch (e) { /* storage full — ignore */ }
      // Apply theme immediately on this page too
      if (prefs && prefs.theme && window.CodingArenaTheme) {
        window.CodingArenaTheme.applyTheme(prefs.theme);
      }
      return prefs;
    } catch (error) {
      throw new Error(`Fetching preferences failed: ${error.message}`);
    }
  }
}

/**
 * Permanently delete the current user's account.
 * @returns {Promise<Object>} Response confirming deletion.
 */
async function deleteAccount(password) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 500));

    return { message: "Account deleted successfully" };
  } else {
    try {
      return await apiRequest("/users/me", "DELETE", { password });
    } catch (error) {
      throw new Error(`Deleting account failed: ${error.message}`);
    }
  }
}

/**
 * Mock practice problem bank. In a real backend this data lives server-side
 * and is fetched via /problems endpoints; kept here only so the frontend
 * is fully testable before the backend exists.
 */
const MOCK_PROBLEMS = [
  {
    id: 1,
    title: "Two Sum Sprint",
    difficulty: "EASY",
    tags: ["Arrays", "Hash Map"],
    description: "Given an array of integers, return indices of the two numbers that add up to a target.",
    examples: [
      { input: "[2,7,11,15], target=9", output: "[0,1]" }
    ]
  },
  {
    id: 2,
    title: "Binary Tree Duel",
    difficulty: "MEDIUM",
    tags: ["Trees", "DFS"],
    description: "Given the root of a binary tree, return the maximum depth of the tree.",
    examples: [
      { input: "[3,9,20,null,null,15,7]", output: "3" }
    ]
  },
  {
    id: 3,
    title: "Graph Gauntlet",
    difficulty: "HARD",
    tags: ["Graphs", "Dijkstra"],
    description: "Given a weighted graph, find the shortest path between two nodes.",
    examples: [
      { input: "edges=[[0,1,4],[0,2,1],[2,1,2]], start=0, end=1", output: "3" }
    ]
  },
  {
    id: 4,
    title: "Sliding Window Showdown",
    difficulty: "MEDIUM",
    tags: ["Strings", "Two Pointers"],
    description: "Find the length of the longest substring without repeating characters.",
    examples: [
      { input: '"abcabcbb"', output: "3" }
    ]
  },
  {
    id: 5,
    title: "Stack Standoff",
    difficulty: "EASY",
    tags: ["Stack", "Parsing"],
    description: "Given a string containing just brackets, determine if the input is valid.",
    examples: [
      { input: '"()[]{}"', output: "true" }
    ]
  },
  {
    id: 6,
    title: "DP Deathmatch",
    difficulty: "HARD",
    tags: ["Dynamic Programming"],
    description: "Given an array of coin denominations and an amount, return the fewest coins needed to make up that amount.",
    examples: [
      { input: "coins=[1,2,5], amount=11", output: "3" }
    ]
  }
];

/**
 * Get a list of practice problems, optionally filtered.
 * @param {Object} [filters] - Filter options.
 * @param {string} [filters.difficulty] - "EASY" | "MEDIUM" | "HARD".
 * @param {string} [filters.tag] - Tag to filter by.
 * @param {string} [filters.search] - Text search on title.
 * @param {number} [filters.limit] - Max number of results.
 * @returns {Promise<Array>} Matching problems.
 */
async function getProblems(filters = {}) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    let results = [...MOCK_PROBLEMS];

    if (filters.difficulty) {
      results = results.filter(
        p => p.difficulty === String(filters.difficulty).toUpperCase()
      );
    }

    if (filters.tag) {
      results = results.filter(p =>
        p.tags.some(t => t.toLowerCase() === String(filters.tag).toLowerCase())
      );
    }

    if (filters.search) {
      const query = String(filters.search).toLowerCase();
      results = results.filter(p => p.title.toLowerCase().includes(query));
    }

    if (filters.limit) {
      results = results.slice(0, filters.limit);
    }

    return results;
  } else {
    const params = new URLSearchParams();

    if (filters.difficulty) params.set("difficulty", filters.difficulty);
    if (filters.tag) params.set("tag", filters.tag);
    if (filters.search) params.set("search", filters.search);
    if (filters.limit) params.set("limit", filters.limit);

    try {
      return await apiRequest(`/problems?${params.toString()}`, "GET");
    } catch (error) {
      throw new Error(`Fetching problems failed: ${error.message}`);
    }
  }
}

/**
 * Get a single practice problem by ID.
 * @param {string|number} problemId - Problem ID.
 * @returns {Promise<Object>} Problem data.
 */
async function getProblem(problemId) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const problem = MOCK_PROBLEMS.find(
      p => String(p.id) === String(problemId)
    );

    if (!problem) {
      throw new Error("Problem not found.");
    }

    return problem;
  } else {
    try {
      return await apiRequest(`/problems/${problemId}`, "GET");
    } catch (error) {
      throw new Error(`Fetching problem failed: ${error.message}`);
    }
  }
}

/**
 * Submit code for a practice problem (no opponent/match involved).
 * @param {string|number} problemId - Problem ID.
 * @param {string} code - Source code to submit.
 * @param {string} language - Programming language.
 * @returns {Promise<Object>} Submission result with a verdict.
 */
async function submitPracticeCode(problemId, code, language) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 700));

    if (!code || !code.trim()) {
      return { verdict: "WRONG_ANSWER", message: "Empty submission." };
    }

    // Mock verdict: randomly accept most non-trivial submissions so the
    // flow is testable without a real judge.
    const verdicts = ["ACCEPTED", "ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED"];
    const verdict = verdicts[Math.floor(Math.random() * verdicts.length)];

    return {
      verdict,
      problemId,
      language,
      submittedAt: new Date().toISOString()
    };
  } else {
    try {
      return await apiRequest(
        `/problems/${problemId}/submissions`,
        "POST",
        { code, language }
      );
    } catch (error) {
      throw new Error(`Submission failed: ${error.message}`);
    }
  }
}

/**
 * Mock friend requests "database", shared across accounts via
 * localStorage so testing with multiple mock users works realistically.
 * Each entry: { id, fromUsername, toUsername, status }
 * status is one of: "PENDING", "ACCEPTED", "DECLINED"
 */
function readMockFriendRequests() {
  return JSON.parse(localStorage.getItem("ca_mock_friend_requests") || "[]");
}

function writeMockFriendRequests(requests) {
  localStorage.setItem("ca_mock_friend_requests", JSON.stringify(requests));
}

/**
 * Send a friend request to another user by username.
 * @param {string} targetUsername - The username to send a request to.
 * @returns {Promise<Object>} Confirmation of the sent request.
 */
async function sendFriendRequest(targetUsername) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 400));

    const username = String(targetUsername || "").trim();

    if (!username) {
      throw new Error("Please enter a username.");
    }

    const currentUser = JSON.parse(localStorage.getItem("ca_user") || "{}");

    if (username.toLowerCase() === String(currentUser.username || "").toLowerCase()) {
      throw new Error("You can't add yourself as a friend.");
    }

    const targetExists = MOCK_USERS.some(
      u => u.username.toLowerCase() === username.toLowerCase()
    );

    if (!targetExists) {
      throw new Error(`No user found with username "${username}".`);
    }

    const requests = readMockFriendRequests();

    const alreadyPending = requests.some(
      r =>
        r.fromUsername.toLowerCase() === currentUser.username.toLowerCase() &&
        r.toUsername.toLowerCase() === username.toLowerCase() &&
        r.status === "PENDING"
    );

    if (alreadyPending) {
      throw new Error("Friend request already sent to this user.");
    }

    requests.push({
      id: Date.now(),
      fromUsername: currentUser.username,
      toUsername: username,
      status: "PENDING"
    });

    writeMockFriendRequests(requests);

    return { message: `Friend request sent to ${username}.`, targetUsername: username };
  } else {
    try {
      return await apiRequest("/friends/requests", "POST", { targetUsername });
    } catch (error) {
      throw new Error(`Sending friend request failed: ${error.message}`);
    }
  }
}

/**
 * Get pending friend requests sent TO the current user.
 * @returns {Promise<Array>} Pending requests awaiting a response.
 */
async function getPendingRequests() {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const currentUser = JSON.parse(localStorage.getItem("ca_user") || "{}");
    const requests = readMockFriendRequests();

    return requests.filter(
      r =>
        r.toUsername.toLowerCase() === String(currentUser.username || "").toLowerCase() &&
        r.status === "PENDING"
    );
  } else {
    try {
      return await apiRequest("/friends/requests/pending", "GET");
    } catch (error) {
      throw new Error(`Fetching pending requests failed: ${error.message}`);
    }
  }
}

/**
 * Accept a pending friend request.
 * @param {string|number} requestId - The request ID to accept.
 * @returns {Promise<Object>} Confirmation.
 */
async function acceptFriendRequest(requestId) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const requests = readMockFriendRequests();
    const request = requests.find(r => String(r.id) === String(requestId));

    if (!request) {
      throw new Error("Request not found.");
    }

    request.status = "ACCEPTED";
    writeMockFriendRequests(requests);

    return { message: `You are now friends with ${request.fromUsername}.` };
  } else {
    try {
      return await apiRequest(`/friends/requests/${requestId}/accept`, "POST");
    } catch (error) {
      throw new Error(`Accepting request failed: ${error.message}`);
    }
  }
}

/**
 * Decline a pending friend request.
 * @param {string|number} requestId - The request ID to decline.
 * @returns {Promise<Object>} Confirmation.
 */
async function declineFriendRequest(requestId) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const requests = readMockFriendRequests();
    const request = requests.find(r => String(r.id) === String(requestId));

    if (!request) {
      throw new Error("Request not found.");
    }

    request.status = "DECLINED";
    writeMockFriendRequests(requests);

    return { message: "Request declined." };
  } else {
    try {
      return await apiRequest(`/friends/requests/${requestId}/decline`, "POST");
    } catch (error) {
      throw new Error(`Declining request failed: ${error.message}`);
    }
  }
}


/**
 * Get the current user's confirmed friends (accepted requests, either direction).
 * @returns {Promise<Array>} List of friend usernames.
 */
async function getFriendsList() {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const currentUser = JSON.parse(localStorage.getItem("ca_user") || "{}");
    const myUsername = String(currentUser.username || "").toLowerCase();
    const requests = readMockFriendRequests();

    const friendUsernames = new Set();

    requests.forEach((r) => {
      if (r.status !== "ACCEPTED") {
        return;
      }

      if (r.fromUsername.toLowerCase() === myUsername) {
        friendUsernames.add(r.toUsername);
      } else if (r.toUsername.toLowerCase() === myUsername) {
        friendUsernames.add(r.fromUsername);
      }
    });

    return Array.from(friendUsernames).map((username) => {
      const matchedUser = MOCK_USERS.find(
        u => u.username.toLowerCase() === username.toLowerCase()
      );

      return {
        username,
        rating: matchedUser?.rating ?? "—"
      };
    });
  } else {
    try {
      return await apiRequest("/friends", "GET");
    } catch (error) {
      throw new Error(`Fetching friends failed: ${error.message}`);
    }
  }
}

/**
 * Mock challenge "database", shared across accounts via localStorage.
 * Each entry: { id, fromUsername, toUsername, status, matchId }
 * status is one of: "PENDING", "ACCEPTED", "DECLINED"
 */
function readMockChallenges() {
  return JSON.parse(localStorage.getItem("ca_mock_challenges") || "[]");
}

function writeMockChallenges(challenges) {
  localStorage.setItem("ca_mock_challenges", JSON.stringify(challenges));
}

/**
 * Challenge a friend to a match.
 * @param {string} friendUsername - The friend's username to challenge.
 * @returns {Promise<Object>} Confirmation of the sent challenge.
 */
async function challengeFriend(friendUsername) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const currentUser = JSON.parse(localStorage.getItem("ca_user") || "{}");
    const challenges = readMockChallenges();

    const alreadyPending = challenges.some(
      c =>
        c.fromUsername.toLowerCase() === currentUser.username.toLowerCase() &&
        c.toUsername.toLowerCase() === friendUsername.toLowerCase() &&
        c.status === "PENDING"
    );

    if (alreadyPending) {
      throw new Error(`You already challenged ${friendUsername}.`);
    }

    challenges.push({
      id: Date.now(),
      fromUsername: currentUser.username,
      toUsername: friendUsername,
      status: "PENDING",
      matchId: null
    });

    writeMockChallenges(challenges);

    return { message: `Challenge sent to ${friendUsername}.` };
  } else {
    try {
      return await apiRequest("/challenges", "POST", { username: friendUsername });
    } catch (error) {
      throw new Error(`Sending challenge failed: ${error.message}`);
    }
  }
}

/**
 * Get pending match challenges sent TO the current user.
 * @returns {Promise<Array>} Pending challenges awaiting a response.
 */
async function getPendingChallenges() {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 200));

    const currentUser = JSON.parse(localStorage.getItem("ca_user") || "{}");
    const challenges = readMockChallenges();

    return challenges.filter(
      c =>
        c.toUsername.toLowerCase() === String(currentUser.username || "").toLowerCase() &&
        c.status === "PENDING"
    );
  } else {
    try {
      return await apiRequest("/challenges/pending", "GET");
    } catch (error) {
      throw new Error(`Fetching pending challenges failed: ${error.message}`);
    }
  }
}

/**
 * Respond to a match challenge (accept starts a match, decline dismisses it).
 * @param {string|number} challengeId
 * @param {"accept"|"decline"} action
 * @returns {Promise<Object>} Response, including matchId if accepted.
 */
async function respondToChallenge(challengeId, action) {
  if (MOCK_MODE) {
    await new Promise(r => setTimeout(r, 300));

    const challenges = readMockChallenges();
    const challenge = challenges.find(c => String(c.id) === String(challengeId));

    if (!challenge) {
      throw new Error("Challenge not found.");
    }

    if (action === "accept") {
      challenge.status = "ACCEPTED";
      challenge.matchId = Date.now();
      writeMockChallenges(challenges);

      return { message: "Challenge accepted!", matchId: challenge.matchId };
    } else {
      challenge.status = "DECLINED";
      writeMockChallenges(challenges);

      return { message: "Challenge declined." };
    }
  } else {
    try {
      return await apiRequest(`/challenges/${challengeId}/${action}`, "POST");
    } catch (error) {
      throw new Error(`Responding to challenge failed: ${error.message}`);
    }
  }
}

// Expose the API functions globally for vanilla JS files.
window.CodingArenaAPI = {
  getToken,
  setToken,
  clearToken,
  apiRequest,
  registerUser,
  loginUser,
  getCurrentUser,
  joinQueue,
  leaveQueue,
  getMatch,
  getActiveMatch,
  submitCode,
  getSubmissions,
  getRecentMatches,
  updateProfile,
  changeEmail,
  changePassword,
  updatePreferences,
  getPreferences,
  deleteAccount,
  sendFriendRequest,
  getPendingRequests,
  acceptFriendRequest,
  declineFriendRequest,
  getFriendsList,
  challengeFriend,
  getPendingChallenges,
  respondToChallenge,
  getProblems,
  getProblem,
  submitPracticeCode
};

console.log("[api.js] Loaded and exported:", window.CodingArenaAPI);