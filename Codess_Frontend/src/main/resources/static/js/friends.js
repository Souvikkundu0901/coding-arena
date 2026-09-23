/**
 * Coding Arena — friends panel (index.html only).
 * Handles the "Add Friend" search box. Pending requests, friends
 * list, and challenge buttons will be added in later steps.
 */

document.addEventListener("DOMContentLoaded", () => {
    const api = window.CodingArenaAPI;

    /**
 * Build one <li> for a pending friend request.
 * @param {Object} request
 * @returns {HTMLLIElement}
 */
    function createPendingRequestRow(request) {
        const li = document.createElement("li");
        li.className = "ca-pending-request-row";

        const nameSpan = document.createElement("span");
        nameSpan.className = "ca-pending-request-row__name";
        nameSpan.textContent = request.fromUsername;

        const acceptBtn = document.createElement("button");
        acceptBtn.className = "ca-pending-accept-btn";
        acceptBtn.innerHTML = '<i class="bi bi-check-lg"></i>';
        acceptBtn.addEventListener("click", () => handleRespondToRequest(request.id, "accept"));

        const declineBtn = document.createElement("button");
        declineBtn.className = "ca-pending-decline-btn";
        declineBtn.innerHTML = '<i class="bi bi-x-lg"></i>';
        declineBtn.addEventListener("click", () => handleRespondToRequest(request.id, "decline"));

        li.appendChild(nameSpan);
        li.appendChild(acceptBtn);
        li.appendChild(declineBtn);

        return li;
    }

    /**
     * Render the pending requests list.
     */
    async function renderPendingRequests() {
        const listEl = document.getElementById("pendingRequestsList");

        if (!listEl) {
            return;
        }

        try {
            const requests = await api.getPendingRequests();

            listEl.innerHTML = "";

            if (!requests.length) {
                const emptyLi = document.createElement("li");
                emptyLi.className = "ca-pending-requests__empty";
                emptyLi.textContent = "No pending requests.";
                listEl.appendChild(emptyLi);
                return;
            }

            requests.forEach((request) => {
                listEl.appendChild(createPendingRequestRow(request));
            });
        } catch (error) {
            console.error("[Friends] Failed to load pending requests:", error);
        }
    }

    /**
     * Handle accepting or declining a pending request.
     * @param {string|number} requestId
     * @param {"accept"|"decline"} action
     */
    async function handleRespondToRequest(requestId, action) {
        try {
            if (action === "accept") {
                await api.acceptFriendRequest(requestId);
            } else {
                await api.declineFriendRequest(requestId);
            }

            await renderPendingRequests();
            await renderFriendsList();
        } catch (error) {
            console.error(`[Friends] Failed to ${action} request:`, error);
            alert(error?.message || `Failed to ${action} request.`);
        }
    }


    /**
     * Build one <li> for a confirmed friend, with a Challenge button.
     * @param {Object} friend
     * @returns {HTMLLIElement}
     */
    function createFriendRow(friend) {
        const li = document.createElement("li");
        li.className = "ca-friend-row";

        const nameSpan = document.createElement("span");
        nameSpan.className = "ca-friend-row__name";
        nameSpan.textContent = `${friend.username} (${friend.rating})`;

        const challengeBtn = document.createElement("button");
        challengeBtn.className = "ca-friend-row__challenge-btn";
        challengeBtn.innerHTML = '<i class="bi bi-controller"></i> Challenge';
        challengeBtn.addEventListener("click", (e) => {
            e.stopPropagation();
            handleChallengeFriend(friend.username, challengeBtn);
        });

        li.appendChild(nameSpan);
        li.appendChild(challengeBtn);

        return li;
    }

    /**
     * Render the confirmed friends list.
     */
    async function renderFriendsList() {
        const listEl = document.getElementById("friendsList");

        if (!listEl) {
            return;
        }

        try {
            const friends = await api.getFriendsList();

            listEl.innerHTML = "";

            if (!friends.length) {
                const emptyLi = document.createElement("li");
                emptyLi.className = "ca-pending-requests__empty";
                emptyLi.textContent = "No friends yet.";
                listEl.appendChild(emptyLi);
                return;
            }

            friends.forEach((friend) => {
                listEl.appendChild(createFriendRow(friend));
            });
        } catch (error) {
            console.error("[Friends] Failed to load friends list:", error);
        }
    }

    let challengePollInterval = null;

    function startChallengeWaitPolling() {
        if (challengePollInterval) {
            clearInterval(challengePollInterval);
        }
        let elapsed = 0;
        challengePollInterval = setInterval(async () => {
            elapsed += 2;
            if (elapsed > 120) {
                clearInterval(challengePollInterval);
                challengePollInterval = null;
                return;
            }
            try {
                if (typeof api.getActiveMatch === "function") {
                    const res = await api.getActiveMatch();
                    if (res && res.active && res.matchId) {
                        clearInterval(challengePollInterval);
                        sessionStorage.setItem("currentMatchId", res.matchId);
                        window.location.href = "match.html";
                    }
                }
            } catch (err) {
                // silent
            }
        }, 2000);
    }

    /**
     * Handle clicking "Challenge" next to a friend.
     * @param {string} friendUsername
     * @param {HTMLButtonElement} button
     */
    async function handleChallengeFriend(friendUsername, button) {
        button.disabled = true;

        try {
            const result = await api.challengeFriend(friendUsername);
            alert(result?.message || `Challenge sent to ${friendUsername}! Waiting for them to accept...`);
            startChallengeWaitPolling();
        } catch (error) {
            console.error("[Friends] Failed to send challenge:", error);
            alert(error?.message || "Failed to send challenge.");
        } finally {
            button.disabled = false;
        }
    }

    /**
     * Show the incoming challenge popup for one challenge.
     * @param {Object} challenge
     */
    function showChallengePopup(challenge) {
        const overlay = document.getElementById("challengePopupOverlay");
        const textEl = document.getElementById("challengePopupText");
        const acceptBtn = document.getElementById("challengeAcceptBtn");
        const declineBtn = document.getElementById("challengeDeclineBtn");

        if (!overlay) {
            return;
        }

        if (textEl) {
            textEl.textContent = `${challenge.fromUsername} wants to battle you.`;
        }

        overlay.style.display = "flex";

        const cleanUp = () => {
            overlay.style.display = "none";
            acceptBtn.removeEventListener("click", onAccept);
            declineBtn.removeEventListener("click", onDecline);
        };

        const onAccept = async () => {
            try {
                const result = await api.respondToChallenge(challenge.id, "accept");
                cleanUp();
                sessionStorage.setItem("currentMatchId", result.matchId);
                window.location.href = "match.html";
            } catch (error) {
                console.error("[Friends] Failed to accept challenge:", error);
                alert(error?.message || "Failed to accept challenge.");
            }
        };

        const onDecline = async () => {
            try {
                await api.respondToChallenge(challenge.id, "decline");
            } catch (error) {
                console.error("[Friends] Failed to decline challenge:", error);
            } finally {
                cleanUp();
            }
        };

        acceptBtn.addEventListener("click", onAccept);
        declineBtn.addEventListener("click", onDecline);
    }

    /**
     * Check for new incoming challenges and show a popup if one exists.
     */
    async function checkForChallenges() {
        try {
            const challenges = await api.getPendingChallenges();

            if (challenges.length > 0) {
                showChallengePopup(challenges[0]);
            }
        } catch (error) {
            console.error("[Friends] Failed to check for challenges:", error);
        }
    }

    /**
     * Guard: only run if the friends panel exists on this page.
     * @returns {boolean}
     */
    function isFriendsPanelPresent() {
        return Boolean(document.getElementById("addFriendForm"));
    }

    /**
     * Show a short status message under the add-friend form.
     * @param {string} message
     * @param {boolean} [isError=false]
     */
    function showStatus(message, isError = false) {
        const statusEl = document.getElementById("addFriendStatus");

        if (!statusEl) {
            return;
        }

        statusEl.textContent = message;
        statusEl.style.color = isError ? "var(--ca-red)" : "var(--ca-green)";

        setTimeout(() => {
            statusEl.textContent = "";
        }, 4000);
    }

    /**
     * Handle the add-friend form submission.
     * @param {SubmitEvent} e
     */
    async function handleAddFriend(e) {
        e.preventDefault();

        const input = document.getElementById("addFriendUsernameInput");
        const button = document.getElementById("addFriendBtn");
        const username = input?.value?.trim();

        if (!username) {
            showStatus("Please enter a username.", true);
            return;
        }

        if (button) {
            button.disabled = true;
        }

        try {
            const result = await api.sendFriendRequest(username);
            showStatus(result?.message || "Friend request sent.");
            input.value = "";
        } catch (error) {
            console.error("[Friends] Failed to send friend request:", error);
            showStatus(error?.message || "Failed to send friend request.", true);
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    }

    /**
     * Initialize the friends panel.
     */
    function initializeFriendsPanel() {
        if (!isFriendsPanelPresent()) {
            return;
        }

        if (!api) {
            console.error("[Friends] api.js is not loaded.");
            return;
        }

        const form = document.getElementById("addFriendForm");

        if (form) {
            form.addEventListener("submit", handleAddFriend);
        }
        renderPendingRequests();
        renderFriendsList();

        checkForChallenges();
        setInterval(checkForChallenges, 5000);

        setupDashboardWebSocket();
    }

    function setupDashboardWebSocket() {
        const token = api.getToken();
        let user = null;
        try {
            user = JSON.parse(localStorage.getItem("ca_user") || "null");
        } catch (e) {}

        if (!token || !user?.id || typeof CAWebSocket === "undefined") {
            return;
        }

        try {
            CAWebSocket.connect(user.id, token, () => {
                console.log("[Friends] Connected to WebSocket for user", user.id);
                CAWebSocket.subscribeToUserTopic(user.id, token, (event) => {
                    console.log("[Friends] User topic event received:", event);
                    const type = event?.type;
                    if (type === "MATCH_FOUND" || type === "CHALLENGE_ACCEPTED") {
                        const matchId = event?.matchId ||
                            event?.data?.matchId ||
                            event?.data?.id ||
                            event?.match?.matchId ||
                            event?.match?.id ||
                            event?.id;

                        if (matchId) {
                            sessionStorage.setItem("currentMatchId", matchId);
                            window.location.href = "match.html";
                        }
                    } else if (type === "CHALLENGE_RECEIVED") {
                        checkForChallenges();
                    }
                });
            });
        } catch (err) {
            console.warn("[Friends] Could not connect dashboard WebSocket:", err.message);
        }
    }

    initializeFriendsPanel();
});