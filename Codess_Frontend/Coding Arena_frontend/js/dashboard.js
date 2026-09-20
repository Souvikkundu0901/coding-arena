/**
 * Coding Arena — dashboard (index.html) initializer.
 * Populates rating, wins/losses, avatar/username, and recent matches
 * from the current logged-in user, instead of the static placeholder
 * markup shipped in index.html.
 */

document.addEventListener("DOMContentLoaded", () => {
    const api = window.CodingArenaAPI;

    /**
     * Guard: only run on pages that actually have the dashboard markup.
     * @returns {boolean}
     */
    function isDashboardPage() {
        return Boolean(document.getElementById("userRatingValue"));
    }

    /**
     * Redirect to login if there's no auth token.
     * @returns {string|null} token if present
     */
    function requireAuth() {
        const token = api?.getToken?.();

        if (!token) {
            window.location.href = "login.html";
            return null;
        }

        return token;
    }

    /**
     * Populate rating, wins, losses.
     * @param {Object} user
     */
    function renderUserStats(user) {
        const ratingEl = document.getElementById("userRatingValue");
        const winsEl = document.getElementById("userWinsCount");
        const lossesEl = document.getElementById("userLossesCount");

        if (ratingEl) {
            ratingEl.textContent = user?.rating ?? "—";
        }

        if (winsEl) {
            winsEl.textContent = user?.wins ?? 0;
        }

        if (lossesEl) {
            lossesEl.textContent = user?.losses ?? 0;
        }
    }

    /**
     * Populate avatar image and display name.
     * @param {Object} user
     */
    function getAvatarNumberFromSeed(seed) {
        const value = String(seed || "");

        if (value.startsWith("CA2D:")) {
            const number = Number(value.slice(5));

            if (
                Number.isInteger(number) &&
                number >= 1 &&
                number <= 8
            ) {
                return number;
            }
        }

        return 1;
    }

    function setup3DAvatar(modelViewer) {
        if (!modelViewer) {
            return;
        }

        // Prevent accidental user interaction.
        modelViewer.style.pointerEvents = "none";

        // Keep the camera framing stable while the model rotates.
        modelViewer.cameraOrbit = "0deg 75deg 4.5m";
        modelViewer.fieldOfView = "30deg";

    }

    function renderUserIdentity(user) {
        const avatarEl =
            document.getElementById("userAvatarImg");

        const nameEl =
            document.getElementById("userDisplayName");

        const avatarImage =
            document.getElementById("ca-2d-avatar");

        const username =
            user?.username || "Player";

        const seed =
            user?.avatarSeed || username;


        /*
         * EXISTING PROFILE DROPDOWN AVATAR
         * --------------------------------
         * Do not remove this.
         */
        if (nameEl) {
            nameEl.textContent = username;
        }

        if (avatarEl) {
            const avatarNumber = getAvatarNumberFromSeed(seed);

            avatarEl.src =
                `characters/character-${String(avatarNumber).padStart(2, "0")}.png`;

            avatarEl.alt = `${username}'s character`;
        }


        /*
         * DASHBOARD 3D AVATAR
         * -------------------
         */
        if (avatarImage) {
            const avatarNumber =
                getAvatarNumberFromSeed(seed);

            avatarImage.src =
                `characters/character-${String(avatarNumber).padStart(2, "0")}.png`;

            avatarImage.alt =
                `${username}'s character`;
        }
    }

    /**
     * Build one <li> for the recent matches list.
     * @param {Object} match
     * @returns {HTMLLIElement}
     */
    function createMatchRow(match) {
        const li = document.createElement("li");
        li.className = "ca-match-row";

        const isWin = String(match?.result).toUpperCase() === "WIN";
        const delta = Number(match?.ratingDelta) || 0;
        const deltaSign = delta >= 0 ? "+" : "\u2212";
        const deltaAbs = Math.abs(delta);

        const opponentSpan = document.createElement("span");
        opponentSpan.className = "ca-match-row__opponent";
        opponentSpan.textContent = `vs ${match?.opponentUsername || "Unknown"}`;

        const resultSpan = document.createElement("span");
        resultSpan.className = `ca-result-pill ca-result-pill--${isWin ? "win" : "loss"}`;
        resultSpan.textContent = isWin ? "Win" : "Loss";

        const deltaSpan = document.createElement("span");
        deltaSpan.className = `ca-match-row__delta ca-match-row__delta--${delta >= 0 ? "up" : "down"}`;
        deltaSpan.textContent = `${deltaSign}${deltaAbs}`;

        li.appendChild(opponentSpan);
        li.appendChild(resultSpan);
        li.appendChild(deltaSpan);

        return li;
    }

    /**
     * Populate the recent matches list.
     * @param {Array} matches
     */
    function renderRecentMatches(matches) {
        const listEl = document.getElementById("recentMatchesList");

        if (!listEl) {
            return;
        }

        listEl.innerHTML = "";

        if (!Array.isArray(matches) || matches.length === 0) {
            const emptyLi = document.createElement("li");
            emptyLi.className = "ca-match-row ca-match-row--placeholder";
            emptyLi.textContent = "No matches played yet.";
            listEl.appendChild(emptyLi);
            return;
        }

        matches.forEach((match) => {
            listEl.appendChild(createMatchRow(match));
        });
    }

    /**
     * Get the CSS class suffix for a difficulty.
     * @param {string} difficulty
     * @returns {string}
     */
    function difficultyClass(difficulty) {
        const normalized = String(difficulty || "").toUpperCase();

        if (normalized === "EASY") return "easy";
        if (normalized === "MEDIUM") return "medium";
        if (normalized === "HARD") return "hard";

        return "medium";
    }

    /**
     * Build one recommended-problem card, linking to the solve page.
     * @param {Object} problem
     * @returns {HTMLAnchorElement}
     */
    function createRecommendedProblemCard(problem) {
        const card = document.createElement("a");
        card.className = "ca-problem-card";
        card.href = `practice-solve.html?id=${encodeURIComponent(problem.id)}`;

        const top = document.createElement("div");
        top.className = "ca-problem-card__top";

        const title = document.createElement("h3");
        title.className = "ca-problem-card__title";
        title.textContent = problem.title;

        const diffPill = document.createElement("span");
        const difficulty = String(problem.difficulty || "MEDIUM").toUpperCase();
        diffPill.className = `ca-diff-pill ca-diff-pill--${difficultyClass(difficulty)}`;
        diffPill.textContent = difficulty.charAt(0) + difficulty.slice(1).toLowerCase();

        top.appendChild(title);
        top.appendChild(diffPill);

        const tagRow = document.createElement("div");
        tagRow.className = "ca-tag-row";

        (problem.tags || []).forEach((tag) => {
            const tagPill = document.createElement("span");
            tagPill.className = "ca-tag-pill";
            tagPill.textContent = tag;
            tagRow.appendChild(tagPill);
        });

        card.appendChild(top);
        card.appendChild(tagRow);

        return card;
    }

    /**
     * Populate the "Recommended Problems" scroller.
     * @param {Array} problems
     */
    function renderRecommendedProblems(problems) {
        const scrollerEl = document.getElementById("recommendedProblemsScroller");

        if (!scrollerEl) {
            return;
        }

        scrollerEl.innerHTML = "";

        if (!Array.isArray(problems) || problems.length === 0) {
            const emptyEl = document.createElement("p");
            emptyEl.className = "ca-problems-loading";
            emptyEl.textContent = "No recommended problems right now.";
            scrollerEl.appendChild(emptyEl);
            return;
        }

        problems.forEach((problem) => {
            scrollerEl.appendChild(createRecommendedProblemCard(problem));
        });
    }

    /**
     * Load and render everything the dashboard needs.
     */
    async function initializeDashboard() {
        if (!isDashboardPage()) {
            return;
        }

        if (!api) {
            console.error("[Dashboard] api.js is not loaded.");
            return;
        }

        if (!requireAuth()) {
            return;
        }

        try {
            const user = await api.getCurrentUser();

            // Keep localStorage's cached user in sync with the freshest data.
            localStorage.setItem("ca_user", JSON.stringify(user));

            renderUserStats(user);
            renderUserIdentity(user);
        } catch (error) {
            console.error("[Dashboard] Failed to load current user:", error);

            // If the token is invalid/expired, send the user back to login.
            api.clearToken?.();
            window.location.href = "login.html";
            return;
        }

        try {
            const matches = await api.getRecentMatches(4);
            renderRecentMatches(matches);
        } catch (error) {
            console.error("[Dashboard] Failed to load recent matches:", error);
            renderRecentMatches([]);
        }

        try {
            const problems = await api.getProblems({ limit: 6 });
            renderRecommendedProblems(problems);
        } catch (error) {
            console.error("[Dashboard] Failed to load recommended problems:", error);
            renderRecommendedProblems([]);
        }
    }

    initializeDashboard();
});