// match.js
// Code editor: CodeMirror 5 (loaded globally via CDN as `CodeMirror`).

document.addEventListener(
    "DOMContentLoaded",
    async () => {
        const api =
            window.CodingArenaAPI;

        const ws =
            window.CAWebSocket;

        let matchId = null;
        let currentUser = null;
        let opponentUsername = "Opponent";
        let editor = null;

        let matchTimerInterval = null;
        let matchEnded = false;

        let preMatchCountdownStarted =
            false;

        let matchDurationSeconds =
            30 * 60;

        /**
         * Get authentication token.
         *
         * @returns {string|null}
         */
        function getAuthToken() {
            return api?.getToken?.() || null;
        }

        /**
         * Get selected language.
         *
         * @returns {string}
         */
        function getSelectedLanguage() {
            return (
                document.querySelector(
                    "#languageSelect"
                )?.value ||
                "javascript"
            );
        }

        /**
         * Show page error.
         *
         * @param {string} message
         */
        function showPageError(message) {
            const errorElement =
                document.querySelector(
                    "#matchError"
                ) ||
                document.querySelector(
                    "#problemDescription"
                );

            if (errorElement) {
                errorElement.textContent =
                    message;
            }
        }

        /**
         * Format MM:SS.
         *
         * @param {number} seconds
         * @returns {string}
         */
        function formatTime(seconds) {
            const safeSeconds =
                Math.max(seconds, 0);

            const minutes =
                Math.floor(
                    safeSeconds / 60
                );

            const remainingSeconds =
                safeSeconds % 60;

            return `${String(minutes).padStart(
                2,
                "0"
            )}:${String(
                remainingSeconds
            ).padStart(2, "0")}`;
        }

        /**
         * Stop match timer.
         */
        function stopMatchTimer() {
            if (matchTimerInterval) {
                clearInterval(
                    matchTimerInterval
                );

                matchTimerInterval = null;
            }
        }

        /**
         * Run the immediate 5-second
         * pre-match countdown.
         *
         * IMPORTANT:
         * This starts as soon as match.html
         * has a valid matchId.
         *
         * @param {number} durationSeconds
         */
        function runPreMatchCountdown(
            durationSeconds
        ) {
            if (
                preMatchCountdownStarted
            ) {
                return;
            }

            preMatchCountdownStarted =
                true;

            const duration =
                Number(durationSeconds);

            if (
                Number.isFinite(duration) &&
                duration > 0
            ) {
                matchDurationSeconds =
                    duration;
            }

            const countdownOverlay =
                document.querySelector(
                    "#matchCountdownOverlay"
                );

            const countdownNumber =
                document.querySelector(
                    "#matchCountdownNumber"
                );

            const startCodingOverlay =
                document.querySelector(
                    "#startCodingOverlay"
                );

            let secondsLeft = 5;

            setEditorDisabled(true);

            const submitButton =
                document.querySelector(
                    "#submitBtn"
                );

            if (submitButton) {
                submitButton.disabled =
                    true;
            }

            if (countdownOverlay) {
                countdownOverlay.style.display =
                    "flex";
            }

            if (countdownNumber) {
                countdownNumber.textContent =
                    "5";
            }

            /*
             * Start the countdown immediately.
             */
            const countdownInterval =
                setInterval(() => {
                    secondsLeft -= 1;

                    if (
                        secondsLeft > 0
                    ) {
                        if (countdownNumber) {
                            countdownNumber.textContent =
                                String(
                                    secondsLeft
                                );
                        }

                        return;
                    }

                    clearInterval(
                        countdownInterval
                    );

                    if (countdownOverlay) {
                        countdownOverlay.style.display =
                            "none";
                    }

                    if (startCodingOverlay) {
                        startCodingOverlay.style.display =
                            "flex";
                    }

                    /*
                     * Small 1-second START CODING
                     * transition is kept.
                     */
                    setTimeout(() => {
                        if (
                            startCodingOverlay
                        ) {
                            startCodingOverlay.style.display =
                                "none";
                        }

                        setEditorDisabled(
                            false
                        );

                        if (
                            submitButton &&
                            !matchEnded
                        ) {
                            submitButton.disabled =
                                false;
                        }

                        startMatchTimer(
                            matchDurationSeconds
                        );
                    }, 1000);
                }, 1000);
        }

        /**
         * Start the real match timer.
         *
         * @param {number} durationSeconds
         */
        function startMatchTimer(
            durationSeconds
        ) {
            const timerElement =
                document.querySelector(
                    "#matchTimer"
                );

            stopMatchTimer();

            let remaining =
                Number(
                    durationSeconds
                );

            if (
                !Number.isFinite(
                    remaining
                ) ||
                remaining <= 0
            ) {
                console.warn(
                    "[Match] Invalid match duration. Timer will not start."
                );

                return;
            }

            if (timerElement) {
                timerElement.textContent =
                    formatTime(
                        remaining
                    );
            }

            matchTimerInterval =
                setInterval(() => {
                    remaining -= 1;

                    if (timerElement) {
                        timerElement.textContent =
                            formatTime(
                                remaining
                            );
                    }

                    if (
                        remaining <= 0
                    ) {
                        stopMatchTimer();
                    }
                }, 1000);
        }

        /**
         * Render problem.
         *
         * @param {Object} problem
         */
        function renderProblem(
            problem = {}
        ) {
            const descriptionElement =
                document.querySelector(
                    "#problemDescription"
                );

            const examplesElement =
                document.querySelector(
                    "#problemExamples"
                );

            const difficultyElement =
                document.querySelector(
                    "#problemDifficulty"
                );

            if (descriptionElement) {
                descriptionElement.textContent =
                    problem.description ||
                    "Problem description unavailable.";
            }

            if (examplesElement) {
                examplesElement.innerHTML =
                    "";

                const examples =
                    Array.isArray(
                        problem.examples
                    )
                        ? problem.examples
                        : [];

                if (
                    examples.length ===
                    0
                ) {
                    examplesElement.textContent =
                        "No examples available.";
                } else {
                    examples.forEach(
                        (example) => {
                            const exampleElement =
                                document.createElement(
                                    "div"
                                );

                            if (
                                typeof example ===
                                "string"
                            ) {
                                exampleElement.textContent =
                                    example;
                            } else {
                                exampleElement.textContent =
                                    example?.input !==
                                        undefined ||
                                        example?.output !==
                                        undefined
                                        ? `Input: ${example.input ??
                                        ""
                                        }\nOutput: ${example.output ??
                                        ""
                                        }`
                                        : JSON.stringify(
                                            example
                                        );
                            }

                            exampleElement.className =
                                "problem-example";

                            examplesElement.appendChild(
                                exampleElement
                            );
                        }
                    );
                }
            }

            if (difficultyElement) {
                const difficulty =
                    String(
                        problem.difficulty ||
                        "UNKNOWN"
                    ).toUpperCase();

                difficultyElement.textContent =
                    difficulty;

                difficultyElement.classList.remove(
                    "easy",
                    "medium",
                    "hard",
                    "difficulty-easy",
                    "difficulty-medium",
                    "difficulty-hard"
                );

                if (
                    difficulty ===
                    "EASY"
                ) {
                    difficultyElement.classList.add(
                        "difficulty-easy"
                    );
                } else if (
                    difficulty ===
                    "MEDIUM"
                ) {
                    difficultyElement.classList.add(
                        "difficulty-medium"
                    );
                } else if (
                    difficulty ===
                    "HARD"
                ) {
                    difficultyElement.classList.add(
                        "difficulty-hard"
                    );
                }
            }

            const titleElement =
                document.querySelector(
                    "#problemTitle"
                );

            if (
                titleElement &&
                problem.title
            ) {
                titleElement.textContent =
                    problem.title;
            }
        }

        /**
         * Render opponent.
         *
         * @param {Object} match
         */
        function renderOpponent(match) {
            const opponentElement = document.querySelector("#opponentStatus");

            if (!opponentElement) {
                return;
            }

            const players =
                Array.isArray(
                    match?.players
                )
                    ? match.players
                    : [match?.playerA, match?.playerB].filter(Boolean);

            const opponent =
                players.find(
                    (player) =>
                        String(
                            player?.id
                        ) !==
                        String(
                            currentUser?.id
                        )
                );

            if (!opponent) {
                opponentElement.textContent =
                    "Opponent unavailable";

                return;
            }

            const opponentName =
                opponent.username ||
                opponent.name ||
                opponent.displayName ||
                "Opponent";

            opponentUsername =
                opponentName;

            const status =
                opponent.status ||
                opponent.connectionStatus ||
                "Connected";

            opponentElement.textContent =
                `${opponentName} • ${status}`;
        }

        /**
         * Initialize CodeMirror 5.
         */
        function initializeEditor() {
            const container =
                document.querySelector(
                    "#codeEditorContainer"
                );

            if (!container) {
                console.error(
                    "[Match] #codeEditorContainer not found."
                );

                return;
            }

            if (
                typeof CodeMirror ===
                "undefined"
            ) {
                console.error(
                    "[Match] CodeMirror is not loaded. " +
                    "Make sure the CDN scripts are included before match.js."
                );

                return;
            }

            const textarea =
                document.createElement(
                    "textarea"
                );

            textarea.id =
                "caCodeEditor";

            textarea.setAttribute(
                "spellcheck",
                "false"
            );

            container.innerHTML =
                "";

            container.appendChild(
                textarea
            );

            const language =
                getSelectedLanguage();

            editor =
                CodeMirror.fromTextArea(
                    textarea,
                    {
                        mode:
                            getCodeMirrorMode(
                                language
                            ),

                        theme: "dracula",

                        lineNumbers:
                            true,

                        indentUnit: 4,

                        tabSize: 4,

                        autofocus:
                            true,

                        lineWrapping:
                            false,

                        autoCloseBrackets:
                            true
                    }
                );

            editor.setSize(
                "100%",
                "100%"
            );

            /*
             * Ensure correct dimensions
             * after layout calculation.
             */
            setTimeout(() => {
                if (editor) {
                    editor.setSize(
                        "100%",
                        "100%"
                    );

                    editor.refresh();
                }
            }, 100);
        }

        /**
         * Map application language
         * to CodeMirror mode.
         *
         * @param {string} language
         * @returns {string}
         */
        function getCodeMirrorMode(
            language
        ) {
            const normalized =
                String(
                    language
                ).toLowerCase();

            const modes = {
                java:
                    "text/x-java",

                javascript:
                    "javascript",

                js:
                    "javascript",

                python:
                    "python",

                cpp:
                    "text/x-c++src",

                "c++":
                    "text/x-c++src",

                c:
                    "text/x-csrc"
            };

            return (
                modes[
                normalized
                ] ||
                "text/plain"
            );
        }

        /**
         * Update CodeMirror language.
         *
         * @param {string} language
         */
        function updateEditorLanguage(
            language
        ) {
            if (!editor) {
                return;
            }

            editor.setOption(
                "mode",
                getCodeMirrorMode(
                    language
                )
            );

            editor.refresh();
        }

        /**
         * Enable/disable editor.
         *
         * @param {boolean} disabled
         */
        function setEditorDisabled(
            disabled
        ) {
            if (editor) {
                editor.setOption(
                    "readOnly",
                    disabled
                );
            }
        }

        /**
         * Get current editor code.
         *
         * @returns {string}
         */
        function getEditorCode() {
            return editor
                ? editor.getValue()
                : "";
        }

        /**
         * Render verdict.
         *
         * @param {string} verdict
         */
        function renderVerdict(
            verdict
        ) {
            const verdictElement =
                document.querySelector(
                    "#verdictDisplay"
                );

            if (!verdictElement) {
                return;
            }

            verdictElement.style.display =
                "flex";

            const normalizedVerdict =
                String(
                    verdict ||
                    "PENDING"
                ).toUpperCase();

            verdictElement.textContent =
                normalizedVerdict;

            verdictElement.classList.remove(
                "verdict-accepted",
                "verdict-wrong-answer",
                "verdict-tle",
                "verdict-re",
                "verdict-pending"
            );

            const classes = {
                ACCEPTED:
                    "verdict-accepted",

                WRONG_ANSWER:
                    "verdict-wrong-answer",

                TLE:
                    "verdict-tle",

                RE:
                    "verdict-re",

                PENDING:
                    "verdict-pending"
            };

            verdictElement.classList.add(
                classes[
                normalizedVerdict
                ] ||
                "verdict-pending"
            );
        }

        /**
         * Create submission-history item.
         *
         * @param {Object} submission
         * @returns {HTMLElement}
         */
        function createSubmissionElement(
            submission
        ) {
            const item =
                document.createElement(
                    "li"
                );

            const verdict =
                String(
                    submission?.verdict ||
                    "PENDING"
                ).toUpperCase();

            const language =
                submission?.language ||
                "Unknown";

            const timestamp =
                submission?.submittedAt ||
                submission?.createdAt ||
                "";

            const timeText =
                timestamp
                    ? new Date(
                        timestamp
                    ).toLocaleString()
                    : "";

            item.className =
                "submission-item";

            item.innerHTML = `
                <div class="submission-main">
                    <span class="submission-verdict">
                        ${escapeHtml(verdict)}
                    </span>

                    <span class="submission-language">
                        ${escapeHtml(language)}
                    </span>
                </div>

                <div class="submission-meta">
                    <span>
                        ${escapeHtml(
                timeText
            )}
                    </span>
                </div>
            `;

            const verdictElement =
                item.querySelector(
                    ".submission-verdict"
                );

            if (verdictElement) {
                verdictElement.classList.add(
                    getVerdictClass(
                        verdict
                    )
                );
            }

            return item;
        }

        /**
         * Get CSS verdict class.
         *
         * @param {string} verdict
         * @returns {string}
         */
        function getVerdictClass(
            verdict
        ) {
            const classes = {
                ACCEPTED:
                    "verdict-accepted",

                WRONG_ANSWER:
                    "verdict-wrong-answer",

                TLE:
                    "verdict-tle",

                RE:
                    "verdict-re",

                PENDING:
                    "verdict-pending"
            };

            return (
                classes[verdict] ||
                "verdict-pending"
            );
        }

        /**
         * Escape HTML.
         *
         * @param {unknown} value
         * @returns {string}
         */
        function escapeHtml(value) {
            return String(
                value ?? ""
            )
                .replace(
                    /&/g,
                    "&amp;"
                )
                .replace(
                    /</g,
                    "&lt;"
                )
                .replace(
                    />/g,
                    "&gt;"
                )
                .replace(
                    /"/g,
                    "&quot;"
                )
                .replace(
                    /'/g,
                    "&#039;"
                );
        }

        /**
         * Add submission to history.
         *
         * @param {Object} submission
         */
        function prependSubmission(
            submission
        ) {
            const historyElement =
                document.querySelector(
                    "#submissionListItems"
                );

            if (!historyElement) {
                return;
            }

            const item =
                createSubmissionElement(
                    submission
                );

            historyElement.prepend(
                item
            );
        }

        /**
         * Render submission history.
         *
         * @param {Array|Object} response
         */
        function renderSubmissionHistory(
            response
        ) {
            const historyElement =
                document.querySelector(
                    "#submissionListItems"
                );

            if (!historyElement) {
                return;
            }

            historyElement.innerHTML =
                "";

            const submissions =
                Array.isArray(
                    response
                )
                    ? response
                    : response?.submissions ||
                    [];

            submissions.forEach(
                (submission) => {
                    historyElement.appendChild(
                        createSubmissionElement(
                            submission
                        )
                    );
                }
            );
        }

        /**
         * Render match-end modal.
         *
         * @param {Object} eventData
         */
        function renderMatchEnd(
            eventData = {}
        ) {
            const modal =
                document.querySelector(
                    "#matchEndModal"
                );

            if (!modal) {
                return;
            }

            const winnerElement =
                modal.querySelector(
                    "#matchWinnerText"
                );

            const player1NameElement =
                modal
                    .querySelector(
                        "#player1RatingChange"
                    )
                    ?.closest(
                        ".ca-rating-change-row"
                    )
                    ?.querySelector(
                        ".ca-rating-player"
                    );

            const player2NameElement =
                modal
                    .querySelector(
                        "#player2RatingChange"
                    )
                    ?.closest(
                        ".ca-rating-change-row"
                    )
                    ?.querySelector(
                        ".ca-rating-player"
                    );

            if (player1NameElement) {
                player1NameElement.textContent =
                    currentUser?.username ||
                    "You";
            }

            if (player2NameElement) {
                player2NameElement.textContent =
                    opponentUsername ||
                    "Opponent";
            }

            const winnerName =
                eventData.winnerName ||
                eventData.winner
                    ?.username ||
                eventData.winner
                    ?.name ||
                (String(
                    eventData.winnerId
                ) ===
                    String(
                        currentUser?.id
                    )
                    ? "You"
                    : "Opponent");

            if (winnerElement) {
                winnerElement.textContent =
                    `${winnerName} won`;
            }

            const ratingChanges =
                eventData.ratingChanges ||
                eventData.ratings ||
                {};

            const currentUserChange =
                ratingChanges.currentUser ??
                ratingChanges[
                String(
                    currentUser?.id
                )
                ] ??
                eventData.ratingChange ??
                0;

            const player1ChangeElement =
                modal.querySelector(
                    "#player1RatingChange"
                );

            const player2ChangeElement =
                modal.querySelector(
                    "#player2RatingChange"
                );

            if (
                player1ChangeElement
            ) {
                const sign =
                    Number(
                        currentUserChange
                    ) >= 0
                        ? "+"
                        : "";

                player1ChangeElement.textContent =
                    `${currentUser?.rating ??
                    "—"
                    } → ${sign}${currentUserChange}`;
            }

            if (
                player2ChangeElement
            ) {
                player2ChangeElement.textContent =
                    "— → —";
            }

            modal.style.display =
                "flex";

            modal.hidden =
                false;
        }

        /**
         * Handle MATCH_START.
         *
         * IMPORTANT:
         * Does NOT restart the countdown.
         *
         * @param {Object} eventData
         */
        function handleMatchStart(
            eventData = {}
        ) {
            const statusElement =
                document.querySelector(
                    "#matchLiveStatus"
                );

            if (statusElement) {
                statusElement.textContent =
                    "LIVE";
            }

            const durationSeconds =
                Number(
                    eventData.durationSeconds
                ) ||
                Number(
                    eventData.duration
                );

            if (
                Number.isFinite(
                    durationSeconds
                ) &&
                durationSeconds > 0
            ) {
                matchDurationSeconds =
                    durationSeconds;
            }

            /*
             * Do NOT call the countdown again
             * if it has already started.
             */
            runPreMatchCountdown(
                matchDurationSeconds
            );
        }

        /**
         * Handle MATCH_END.
         *
         * @param {Object} eventData
         */
        function handleMatchEnd(
            eventData = {}
        ) {
            if (matchEnded) {
                return;
            }

            matchEnded = true;
            stopMatchTimer();

            renderMatchEnd(
                eventData
            );

            const submitButton =
                document.querySelector(
                    "#submitBtn"
                );

            if (submitButton) {
                submitButton.disabled =
                    true;
            }

            setEditorDisabled(
                true
            );
        }

        /**
         * Handle WebSocket event.
         *
         * @param {Object} event
         */
        function handleMatchEvent(
            event
        ) {
            if (!event?.type) {
                return;
            }

            switch (event.type) {
                case "MATCH_START":
                    handleMatchStart(
                        event.data
                    );
                    break;

                case "MATCH_END":
                    handleMatchEnd(
                        event.data
                    );
                    break;

                default:
                    console.log(
                        "[Match] Ignoring event:",
                        event.type
                    );
            }
        }

        /**
         * Connect to match WebSocket.
         */
        function connectToMatchWebSocket() {
            if (!ws) {
                console.error(
                    "[Match] CAWebSocket is unavailable."
                );

                return;
            }

            const subscribe =
                () => {
                    ws.subscribeToMatchTopic(
                        matchId,
                        getAuthToken(),
                        handleMatchEvent
                    );
                };

            if (ws.client) {
                subscribe();

                return;
            }

            if (
                currentUser?.id
            ) {
                ws.connect(
                    currentUser.id,
                    getAuthToken(),
                    subscribe
                );
            }
        }

        /**
         * Submit code.
         */
        async function handleSubmit() {
            const submitButton =
                document.querySelector(
                    "#submitBtn"
                );

            if (
                !submitButton ||
                !editor ||
                matchEnded
            ) {
                return;
            }

            const code =
                getEditorCode();

            const language =
                getSelectedLanguage();

            if (!code.trim()) {
                renderVerdict(
                    "PENDING"
                );

                return;
            }

            submitButton.disabled =
                true;

            const originalText =
                submitButton.textContent;

            submitButton.textContent =
                "Submitting...";

            renderVerdict(
                "PENDING"
            );

            try {
                const response =
                    await api.submitCode(
                        matchId,
                        code,
                        language
                    );

                const submission =
                    response?.submission ||
                    response;

                const verdict =
                    submission?.verdict ||
                    response?.verdict ||
                    "PENDING";

                renderVerdict(
                    verdict
                );

                prependSubmission(
                    submission
                );
            } catch (error) {
                console.error(
                    "[Match] Submission failed:",
                    error
                );

                renderVerdict(
                    "PENDING"
                );

                const verdictElement =
                    document.querySelector(
                        "#verdictDisplay"
                    );

                if (
                    verdictElement
                ) {
                    verdictElement.textContent =
                        error?.message ||
                        "Submission failed. Please try again.";
                }
            } finally {
                if (!matchEnded) {
                    submitButton.disabled =
                        false;
                }

                submitButton.textContent =
                    originalText;
            }
        }

        /**
         * Load match.
         */
        async function loadMatch() {
            try {
                const match =
                    await api.getMatch(
                        matchId
                    );

                renderProblem(
                    match?.problem ||
                    {}
                );

                renderOpponent(
                    match
                );

                currentUser =
                    currentUser || {};

                const titleElement =
                    document.querySelector(
                        "#problemTitle"
                    );

                if (
                    titleElement &&
                    match?.problem?.title
                ) {
                    titleElement.textContent =
                        match.problem.title;
                }

                return match;
            } catch (error) {
                console.error(
                    "[Match] Failed to load match:",
                    error
                );

                showPageError(
                    error?.message ||
                    "Unable to load this match."
                );

                setTimeout(() => {
                    window.location.href =
                        "index.html";
                }, 1500);

                return null;
            }
        }

        /**
         * Load submissions.
         */
        async function loadSubmissions() {
            try {
                const response =
                    await api.getSubmissions(
                        matchId
                    );

                renderSubmissionHistory(
                    response
                );
            } catch (error) {
                console.error(
                    "[Match] Failed to load submissions:",
                    error
                );
            }
        }

        /**
         * Setup event listeners.
         */
        function setupEventListeners() {
            const languageSelect =
                document.querySelector(
                    "#languageSelect"
                );

            if (languageSelect) {
                languageSelect.addEventListener(
                    "change",
                    () => {
                        updateEditorLanguage(
                            languageSelect.value
                        );
                    }
                );
            }

            const submitButton =
                document.querySelector(
                    "#submitBtn"
                );

            if (submitButton) {
                submitButton.addEventListener(
                    "click",
                    handleSubmit
                );
            }

            const backButton =
                document.querySelector(
                    "#matchEndModal #backToDashboardBtn"
                );

            if (backButton) {
                backButton.addEventListener(
                    "click",
                    () => {
                        window.location.href =
                            "index.html";
                    }
                );
            }

            const genericBackButton =
                document.querySelector(
                    "#backToDashboardBtn"
                );

            if (
                genericBackButton &&
                genericBackButton !==
                backButton
            ) {
                genericBackButton.addEventListener(
                    "click",
                    () => {
                        window.location.href =
                            "index.html";
                    }
                );
            }
        }

        /**
         * Initialize match page.
         */
        async function initialize() {
            if (!api || !ws) {
                console.error(
                    "[Match] api.js or websocket.js is not loaded."
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

            const urlParams =
                new URLSearchParams(
                    window.location.search
                );

            matchId =
                urlParams.get(
                    "matchId"
                );

            if (!matchId) {
                showPageError(
                    "Invalid match ID."
                );

                setTimeout(() => {
                    window.location.href =
                        "index.html";
                }, 1000);

                return;
            }

            try {
                const storedUser =
                    localStorage.getItem(
                        "ca_user"
                    );

                currentUser =
                    storedUser
                        ? JSON.parse(
                            storedUser
                        )
                        : null;
            } catch (error) {
                console.error(
                    "[Match] Invalid ca_user:",
                    error
                );

                currentUser =
                    null;
            }

            if (!currentUser?.id) {
                window.location.href =
                    "login.html";

                return;
            }

            /*
             * IMPORTANT:
             *
             * Start the 5-second countdown
             * immediately.
             *
             * Do NOT wait for:
             * - API match loading
             * - submissions
             * - WebSocket connection
             * - MATCH_START event
             */
            runPreMatchCountdown(
                matchDurationSeconds
            );
            initializeEditor();
            setupEventListeners();
            const match =
                await loadMatch();

            if (!match) {
                return;
            }
            await loadSubmissions();
            connectToMatchWebSocket();
        }

        window.addEventListener(
            "beforeunload",
            () => {
                stopMatchTimer();

                if (
                    matchEnded &&
                    ws?.client
                ) {
                    ws.disconnect().catch(
                        () => { }
                    );
                }
            }
        );
        initialize();
    }
);