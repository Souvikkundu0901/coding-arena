/**
 * Coding Arena WebSocket client.
 *
 * Requires these globals to be loaded before this file:
 * - StompJs
 * - SockJS
 */

const CAWebSocket = {
    client: null,
    _mockUserCallback: null,
    _mockMatchCallback: null,

    /**
     * Connect to the Coding Arena WebSocket server.
     *
     * @param {string|number} userId - Authenticated user ID.
     * @param {string} token - JWT authentication token.
     * @param {Function} onConnectedCallback - Called after successful connection.
     */
    connect(userId, token, onConnectedCallback) {
        if (MOCK_MODE) {
            console.log("[WS] (mock) connected");
            onConnectedCallback();
            setTimeout(() => {
                this._mockUserCallback && this._mockUserCallback({
                    type: "MATCH_FOUND",
                    data: { id: 999, matchId: 999, opponent: { username: "opponent123", rating: 1470 } }
                });
            }, 4000);
            return;
        } else {
            try {
                const CODESS_DEFAULT_REMOTE_BACKEND = "https://codess-8rrg.onrender.com";
                let backendOrigin = "http://localhost:8080";
                if (typeof window !== "undefined") {
                    if (window.CODESS_BACKEND_URL) {
                        backendOrigin = window.CODESS_BACKEND_URL.replace(/\/$/, "");
                    } else if (localStorage.getItem("codess_backend_url")) {
                        backendOrigin = localStorage.getItem("codess_backend_url").replace(/\/$/, "");
                    } else if (window.location && window.location.hostname) {
                        const host = window.location.hostname;
                        if (host === "localhost" || host === "127.0.0.1") {
                            backendOrigin = window.location.port === "8080" ? window.location.origin : "http://localhost:8080";
                        } else if (host.endsWith("onrender.com")) {
                            backendOrigin = window.location.origin;
                        } else {
                            backendOrigin = CODESS_DEFAULT_REMOTE_BACKEND;
                        }
                    }
                }
                const wsUrl = `${backendOrigin}/ws`;

                this.client = new StompJs.Client({
                    webSocketFactory: () => new SockJS(wsUrl),
                    connectHeaders: { Authorization: `Bearer ${token}` },
                    reconnectDelay: 5000,
                    onConnect: () => {
                        console.log("[WS] connected");
                        onConnectedCallback();
                        this.subscribeToUserTopic(userId, token, this._mockUserCallback);
                    },
                    onStompError: (frame) => console.error("[WS] STOMP error:", frame),
                    onWebSocketError: (error) => console.error("[WS] WebSocket error:", error)
                });
                this.client.activate();
            } catch (error) {
                console.error("[WS] Connection setup failed:", error.message);
            }
        }
    },

    /**
     * Subscribe to the authenticated user's topic.
     *
     * @param {string|number} userId - User ID.
     * @param {string} token - JWT authentication token.
     * @param {Function} onMessageCallback - Called with the parsed event.
     */
    subscribeToUserTopic(userId, token, onMessageCallback) {
        if (MOCK_MODE) {
            this._mockUserCallback = onMessageCallback;
            console.log("[WS] (mock) subscribed to user topic:", `/topic/user/${userId}`);
            return;
        }

        if (!this.client) {
            console.error("[WS] Cannot subscribe: client is not initialized.");
            return;
        }

        const destination = `/topic/user/${userId}`;

        const subscription = this.client.subscribe(
            destination,
            (message) => {
                console.log("[WS] Message received:", destination);

                try {
                    const parsedEvent = JSON.parse(message.body);

                    if (typeof onMessageCallback === "function") {
                        onMessageCallback(parsedEvent);
                    }
                } catch (error) {
                    console.error(
                        "[WS] Failed to parse user message:",
                        error
                    );
                }
            },
            {
                Authorization: `Bearer ${token}`
            }
        );

        console.log(
            "[WS] Subscribed to user topic:",
            destination,
            subscription
        );
    },

    /**
     * Subscribe to a specific match topic.
     *
     * @param {string|number} matchId - Match ID.
     * @param {string} token - JWT authentication token.
     * @param {Function} onMessageCallback - Called with the parsed event.
     */
    subscribeToMatchTopic(matchId, token, onMessageCallback) {
        if (MOCK_MODE) {
            this._mockMatchCallback = onMessageCallback;
            console.log("[WS] (mock) subscribed to match topic:", `/topic/match/${matchId}`);

            // Simulate MATCH_START shortly after subscribing.
            setTimeout(() => {
                this._mockMatchCallback && this._mockMatchCallback({
                    type: "MATCH_START",
                    data: {
                        id: matchId,
                        status: "IN_PROGRESS",
                        durationSeconds: 1800 // 30 minutes
                    }
                });
            }, 1000);

            // Simulate MATCH_END a while after that, so you can test
            // the end-of-match modal and rating update flow.
            setTimeout(() => {
                this._mockMatchCallback && this._mockMatchCallback({
                    type: "MATCH_END",
                    data: {
                        winnerName: "You",
                        ratingChanges: { currentUser: 12 }
                    }
                });
            }, 20000); // fires 20s after subscribing

            return;
        }

        if (!this.client) {
            console.error("[WS] Cannot subscribe: client is not initialized.");
            return;
        }

        const destination = `/topic/match/${matchId}`;

        const subscription = this.client.subscribe(
            destination,
            (message) => {
                console.log("[WS] Message received:", destination);

                try {
                    const parsedEvent = JSON.parse(message.body);

                    if (typeof onMessageCallback === "function") {
                        onMessageCallback(parsedEvent);
                    }
                } catch (error) {
                    console.error(
                        "[WS] Failed to parse match message:",
                        error
                    );
                }
            },
            {
                Authorization: `Bearer ${token}`
            }
        );

        console.log(
            "[WS] Subscribed to match topic:",
            destination,
            subscription
        );
    },

    /**
     * Disconnect from the Coding Arena WebSocket server.
     *
     * @returns {Promise<void>}
     */
    async disconnect() {
        if (MOCK_MODE) {
            console.log("[WS] (mock) disconnected");
            this.client = null;
            this._mockUserCallback = null;
            this._mockMatchCallback = null;
            return;
        }

        if (!this.client) {
            console.log("[WS] No active client to disconnect.");
            return;
        }

        try {
            await this.client.deactivate();
            console.log("[WS] Disconnected");
        } catch (error) {
            console.error("[WS] Disconnect error:", error);
        } finally {
            this.client = null;
        }
    }
};

window.CAWebSocket = CAWebSocket;