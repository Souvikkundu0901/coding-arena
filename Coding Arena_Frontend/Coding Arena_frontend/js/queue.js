console.log("[queue.js] File is executing");

document.addEventListener("DOMContentLoaded", () => {
    console.log(
        "[queue.js] DOMContentLoaded fired, window.CodingArenaAPI is:",
        window.CodingArenaAPI
    );

    console.log(
        "[queue.js] window.CAWebSocket is:",
        window.CAWebSocket
    );

    const api = window.CodingArenaAPI;
    const ws = window.CAWebSocket;

    let queueSeconds = 0;
    let queueTimerInterval = null;
    let redirectTimeout = null;
    let matchFound = false;

    /**
     * Get the current authentication token.
     * @returns {string|null}
     */
    function getAuthToken() {
        return api?.getToken?.() || null;
    }

    /**
     * Format elapsed queue time as MM:SS.
     * @param {number} seconds
     * @returns {string}
     */
    function formatTimer(seconds) {
        const minutes = Math.floor(seconds / 60);
        const remainingSeconds = seconds % 60;

        return `${String(minutes).padStart(2, "0")}:${String(
            remainingSeconds
        ).padStart(2, "0")}`;
    }

    /**
     * Start the queue timer.
     */
    function startQueueTimer() {
        const timerElement =
            document.querySelector("#queueTimer");

        stopQueueTimer();

        queueSeconds = 0;

        if (timerElement) {
            timerElement.textContent = "00:00";
        }

        queueTimerInterval = setInterval(() => {
            queueSeconds += 1;

            if (timerElement) {
                timerElement.textContent =
                    formatTimer(queueSeconds);
            }
        }, 1000);
    }

    /**
     * Stop the queue timer.
     */
    function stopQueueTimer() {
        if (queueTimerInterval) {
            clearInterval(queueTimerInterval);
            queueTimerInterval = null;
        }
    }

    /**
     * Update queue status.
     * @param {string} message
     */
    function updateQueueStatus(message) {
        const statusElement =
            document.querySelector("#queueStatus");

        if (statusElement) {
            statusElement.textContent = message;
        }
    }

    /**
     * Handle MATCH_FOUND.
     * Navigation happens immediately.
     *
     * @param {Object} event
     */
    function handleMatchFound(event) {
        if (matchFound) {
            return;
        }

        matchFound = true;

        stopQueueTimer();

        updateQueueStatus("Match found!");

        const banner =
            document.querySelector("#matchFoundBanner");

        const opponentNameElement =
            document.querySelector("#opponentName");

        const matchData = event?.data || event?.match || event || {};

        let currentUsername = "";
        try {
            const rawUser = localStorage.getItem("ca_user");
            if (rawUser) {
                currentUsername = JSON.parse(rawUser)?.username || "";
            }
        } catch (e) {}

        let opponentName =
            matchData.opponentName ||
            matchData.opponent?.username ||
            matchData.opponent?.name;

        if (!opponentName && Array.isArray(matchData.players) && matchData.players.length > 0) {
            const opp = matchData.players.find(p => p.username && p.username !== currentUsername);
            if (opp) opponentName = opp.username;
        }
        if (!opponentName) {
            if (matchData.playerAUsername && matchData.playerAUsername !== currentUsername) {
                opponentName = matchData.playerAUsername;
            } else if (matchData.playerBUsername && matchData.playerBUsername !== currentUsername) {
                opponentName = matchData.playerBUsername;
            }
        }
        opponentName = opponentName || "Opponent";

        if (opponentNameElement) {
            opponentNameElement.textContent = opponentName;
        }

        if (banner) {
            banner.hidden = false;
        }

        const matchId =
            matchData.matchId ??
            matchData.id ??
            event?.matchId ??
            event?.id;

        if (!matchId) {
            updateQueueStatus(
                "Match found, but match ID is missing."
            );

            console.error(
                "[Queue] MATCH_FOUND event has no match ID:",
                event
            );

            return;
        }

        /*
         * IMPORTANT:
         * No intentional delay here.
         *
         * The browser moves to match.html immediately.
         * The match page starts its own 5-second countdown
         * immediately when it loads.
         */
        if (redirectTimeout) {
            clearTimeout(redirectTimeout);
            redirectTimeout = null;
        }

        window.location.href =
            `match.html?matchId=${encodeURIComponent(matchId)}`;
    }

    /**
     * Handle user WebSocket event.
     * @param {Object} event
     */
    function handleUserEvent(event) {
        if (event?.type === "MATCH_FOUND") {
            handleMatchFound(event);
        }
    }

    /**
     * Connect to queue WebSocket.
     *
     * @param {Object} user
     * @param {string} token
     */
    function connectToQueueSocket(user, token) {
        ws.connect(
            user.id,
            token,
            () => {
                console.log(
                    "[Queue] WebSocket connected."
                );

                ws.subscribeToUserTopic(
                    user.id,
                    token,
                    handleUserEvent
                );
            }
        );
    }

    /**
     * Leave queue.
     */
    async function handleLeaveQueue() {
        const leaveButton =
            document.querySelector("#leaveQueueBtn");

        try {
            if (leaveButton) {
                leaveButton.disabled = true;
            }

            stopQueueTimer();

            if (redirectTimeout) {
                clearTimeout(redirectTimeout);
                redirectTimeout = null;
            }

            await api.leaveQueue();
        } catch (error) {
            console.error(
                "[Queue] Failed to leave queue:",
                error
            );
        } finally {
            try {
                await ws.disconnect();
            } catch (error) {
                console.error(
                    "[Queue] Failed to disconnect WebSocket:",
                    error
                );
            }

            window.location.href = "index.html";
        }
    }

    /**
     * Start matchmaking queue.
     *
     * @param {Object} user
     * @param {string} token
     */
    async function startQueue(user, token) {
        try {
            updateQueueStatus(
                "Finding an opponent..."
            );

            await api.joinQueue();

            startQueueTimer();

            connectToQueueSocket(
                user,
                token
            );
        } catch (error) {
            console.error(
                "[Queue] Failed to join queue:",
                error
            );

            stopQueueTimer();

            const errorMessage =
                error?.message ||
                "Unable to join the matchmaking queue.";

            updateQueueStatus(
                errorMessage
            );

            const leaveButton =
                document.querySelector(
                    "#leaveQueueBtn"
                );

            if (leaveButton) {
                leaveButton.textContent =
                    "Back to Home";

                leaveButton.disabled =
                    false;
            }

            alert(errorMessage);
        }
    }

    /**
     * Initialize queue page.
     */
    function initializeQueuePage() {
        if (!api || !ws) {
            console.error(
                "[Queue] Required modules are missing. " +
                "Make sure api.js and websocket.js are loaded first."
            );

            return;
        }

        const token =
            getAuthToken();

        if (!token) {
            window.location.href =
                "login.html";

            return;
        }

        let user;

        try {
            const storedUser =
                localStorage.getItem("ca_user");

            user = storedUser
                ? JSON.parse(storedUser)
                : null;
        } catch (error) {
            console.error(
                "[Queue] Invalid ca_user data:",
                error
            );

            user = null;
        }

        if (!user || !user.id) {
            console.error(
                "[Queue] Current user data is missing."
            );

            window.location.href =
                "login.html";

            return;
        }

        const ratingElement =
            document.querySelector("#queueRating");

        if (ratingElement) {
            ratingElement.textContent =
                user.rating ??
                user.ratingScore ??
                "—";
        }

        const leaveButton =
            document.querySelector("#leaveQueueBtn");

        if (leaveButton) {
            leaveButton.addEventListener(
                "click",
                handleLeaveQueue
            );
        }

        startQueue(
            user,
            token
        );
    }

    initializeQueuePage();
});