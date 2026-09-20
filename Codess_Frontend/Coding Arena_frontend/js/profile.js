/**
 * Coding Arena — profile.html initializer.
 * Loads the current user's profile + stats + recent matches, and
 * handles the "Edit Profile" modal (username / avatar seed).
 */

document.addEventListener("DOMContentLoaded", () => {
    const api = window.CodingArenaAPI;

    const CHARACTER_PREFIX = "CA2D:";
    const CHARACTER_COUNT = 8;

    /**
     * Guard: only run on the profile page.
     * @returns {boolean}
     */
    function isProfilePage() {
        return Boolean(document.getElementById("profileUsername"));
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
     * Build a DiceBear avatar URL from a seed.
     * @param {string} seed
     * @returns {string}
     */

    function getCharacterNumberFromSeed(seed) {
        const value = String(seed || "");

        if (value.startsWith(CHARACTER_PREFIX)) {
            const number = Number(value.slice(CHARACTER_PREFIX.length));

            if (
                Number.isInteger(number) &&
                number >= 1 &&
                number <= CHARACTER_COUNT
            ) {
                return number;
            }
        }

        return 1;
    }

    /**
     * Format an ISO date string as a readable "Joined" date.
     * @param {string} isoDate
     * @returns {string}
     */
    function formatJoinDate(isoDate) {
        if (!isoDate) {
            return "—";
        }

        try {
            return new Date(isoDate).toLocaleDateString(undefined, {
                year: "numeric",
                month: "long",
                day: "numeric"
            });
        } catch {
            return "—";
        }
    }

    /**
     * Compute win rate as a percentage string.
     * @param {number} wins
     * @param {number} losses
     * @returns {string}
     */
    function computeWinRate(wins, losses) {
        const total = (Number(wins) || 0) + (Number(losses) || 0);

        if (total === 0) {
            return "—";
        }

        const rate = (Number(wins) / total) * 100;
        return `${rate.toFixed(1)}%`;
    }

    /**
     * Render the header avatar/username pill.
     * @param {Object} user
     */
    function renderHeader(user) {
        const headerAvatar = document.getElementById("headerAvatar");
        const headerUsername = document.getElementById("headerUsername");

        if (headerAvatar) {
            const characterNumber = getCharacterNumberFromSeed(user?.avatarSeed);
            headerAvatar.src = `characters/character-${String(characterNumber).padStart(2, "0")}.png`;
            headerAvatar.alt = `${user?.username || "Player"}'s character`;
        }

        if (headerUsername) {
            headerUsername.textContent = user?.username || "Player";
        }
    }

    /**
     * Render the main profile card (avatar, name, stats).
     * @param {Object} user
     */
    function renderProfileCard(user) {
        const profileAvatar = document.getElementById("profileAvatar");
        const profileUsername = document.getElementById("profileUsername");
        const ratingEl = document.getElementById("userRatingValue");
        const winsEl = document.getElementById("userWinsValue");
        const lossesEl = document.getElementById("userLossesValue");
        const winRateEl = document.getElementById("userWinRateValue");
        const joinDateEl = document.getElementById("userJoinDate");

        if (profileAvatar) {
            const characterNumber =
                getCharacterNumberFromSeed(user?.avatarSeed);

            profileAvatar.src =
                `characters/character-${String(characterNumber).padStart(2, "0")}.png`;

            profileAvatar.alt =
                `${user?.username || "Player"}'s character`;
        }

        if (profileUsername) {
            profileUsername.textContent = user?.username || "Player";
        }

        if (ratingEl) {
            ratingEl.textContent = user?.rating ?? "—";
        }

        if (winsEl) {
            winsEl.textContent = user?.wins ?? 0;
        }

        if (lossesEl) {
            lossesEl.textContent = user?.losses ?? 0;
        }

        if (winRateEl) {
            winRateEl.textContent = computeWinRate(user?.wins, user?.losses);
        }

        if (joinDateEl) {
            joinDateEl.textContent = formatJoinDate(user?.createdAt);
        }
    }

    /**
     * Pre-fill the edit modal's inputs with current values.
     * @param {Object} user
     */
    function prefillEditForm(user) {
        const usernameInput =
            document.getElementById("editUsernameInput");

        const avatarSeedInput =
            document.getElementById("editAvatarSeedInput");

        if (usernameInput) {
            usernameInput.value = user?.username || "";
        }

        if (avatarSeedInput) {
            const characterNumber =
                getCharacterNumberFromSeed(user?.avatarSeed);

            avatarSeedInput.value =
                `${CHARACTER_PREFIX}${String(characterNumber).padStart(2, "0")}`;
        }

        setupCharacterPicker(user);
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
     * Render the recent matches list.
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
            emptyLi.textContent = "No match history";
            listEl.appendChild(emptyLi);
            return;
        }

        matches.forEach((match) => {
            listEl.appendChild(createMatchRow(match));
        });
    }

    /**
     * Handle the Edit Profile form submission.
     * @param {SubmitEvent} e
     */
    async function handleEditProfileSubmit(e) {
        e.preventDefault();

        const saveBtn = document.getElementById("saveProfileBtn");
        const usernameInput = document.getElementById("editUsernameInput");
        const avatarSeedInput = document.getElementById("editAvatarSeedInput");

        const updates = {
            username: usernameInput?.value?.trim() || undefined,
            avatarSeed: avatarSeedInput?.value?.trim() || undefined
        };

        if (saveBtn) {
            saveBtn.disabled = true;
            saveBtn.textContent = "Saving...";
        }

        try {
            const updatedUser = await api.updateProfile(updates);

            localStorage.setItem("ca_user", JSON.stringify(updatedUser));

            renderHeader(updatedUser);
            renderProfileCard(updatedUser);

            const modalEl = document.getElementById("editProfileModal");
            const modalInstance = window.bootstrap?.Modal?.getInstance(modalEl);
            modalInstance?.hide();
        } catch (error) {
            console.error("[Profile] Failed to update profile:", error);
            alert(error?.message || "Failed to update profile. Please try again.");
        } finally {
            if (saveBtn) {
                saveBtn.disabled = false;
                saveBtn.textContent = "Save Changes";
            }
        }
    }

    function setupCharacterPicker(user) {
        const picker = document.getElementById("characterPicker");
        const hiddenInput = document.getElementById("editAvatarSeedInput");
        const selectedLabel = document.getElementById("selectedCharacterLabel");

        if (!picker || !hiddenInput) {
            return;
        }

        const currentCharacter =
            getCharacterNumberFromSeed(user?.avatarSeed);

        hiddenInput.value =
            `${CHARACTER_PREFIX}${String(currentCharacter).padStart(2, "0")}`;

        function updateSelection(characterNumber) {
            const formattedNumber =
                String(characterNumber).padStart(2, "0");

            hiddenInput.value =
                `${CHARACTER_PREFIX}${formattedNumber}`;

            picker
                .querySelectorAll(".ca-character-option")
                .forEach((button) => {
                    const isSelected =
                        button.dataset.character === formattedNumber;

                    button.classList.toggle("is-selected", isSelected);

                    button.setAttribute(
                        "aria-pressed",
                        String(isSelected)
                    );
                });

            if (selectedLabel) {
                selectedLabel.textContent =
                    `Character ${formattedNumber}`;
            }
        }

        picker.querySelectorAll(".ca-character-option").forEach((button) => {
            button.addEventListener("click", () => {
                const characterNumber =
                    button.dataset.character;

                if (!characterNumber) {
                    return;
                }

                updateSelection(Number(characterNumber));
            });
        });

        updateSelection(currentCharacter);
    }

    /**
     * Wire up page event listeners.
     */
    function setupEventListeners() {
        const editForm = document.getElementById("editProfileForm");

        if (editForm) {
            editForm.addEventListener("submit", handleEditProfileSubmit);
        }
    }

    /**
     * Load and render everything the profile page needs.
     */
    async function initializeProfile() {
        if (!isProfilePage()) {
            return;
        }

        if (!api) {
            console.error("[Profile] api.js is not loaded.");
            return;
        }

        if (!requireAuth()) {
            return;
        }

        setupEventListeners();

        try {
            const user = await api.getCurrentUser();

            localStorage.setItem("ca_user", JSON.stringify(user));

            renderHeader(user);
            renderProfileCard(user);
            prefillEditForm(user);
        } catch (error) {
            console.error("[Profile] Failed to load current user:", error);

            api.clearToken?.();
            window.location.href = "login.html";
            return;
        }

        try {
            const matches = await api.getRecentMatches(6);
            renderRecentMatches(matches);
        } catch (error) {
            console.error("[Profile] Failed to load recent matches:", error);
            renderRecentMatches([]);
        }
    }

    initializeProfile();
});