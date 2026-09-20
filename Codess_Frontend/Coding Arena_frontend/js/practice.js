/**
 * Coding Arena — practice.html initializer.
 * Loads all practice problems and applies client-side filtering by
 * search text, difficulty, and tag. Clicking a card opens practice-solve.html.
 */

document.addEventListener("DOMContentLoaded", () => {
    const api = window.CodingArenaAPI;

    let allProblems = [];

    /**
     * Guard: only run on the practice list page.
     * @returns {boolean}
     */
    function isPracticeListPage() {
        return Boolean(document.getElementById("problemListContainer"));
    }

    /**
     * Redirect to login if there's no auth token.
     * @returns {string|null}
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
     * Build one problem card link element.
     * @param {Object} problem
     * @returns {HTMLAnchorElement}
     */
    function createProblemCard(problem) {
        const card = document.createElement("a");
        card.className = "ca-problem-list-card";
        card.href = `practice-solve.html?id=${encodeURIComponent(problem.id)}`;

        const top = document.createElement("div");
        top.className = "ca-problem-list-card__top";

        const title = document.createElement("h3");
        title.className = "ca-problem-list-card__title";
        title.textContent = problem.title;

        const diffPill = document.createElement("span");
        diffPill.className = `ca-diff-pill ca-diff-pill--${difficultyClass(problem.difficulty)}`;
        diffPill.textContent =
            problem.difficulty?.charAt(0).toUpperCase() +
            problem.difficulty?.slice(1).toLowerCase();

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
     * Populate the tag filter dropdown from the loaded problem set.
     */
    function populateTagFilter() {
        const tagFilter = document.getElementById("tagFilter");

        if (!tagFilter) {
            return;
        }

        const uniqueTags = new Set();

        allProblems.forEach((problem) => {
            (problem.tags || []).forEach((tag) => uniqueTags.add(tag));
        });

        Array.from(uniqueTags)
            .sort()
            .forEach((tag) => {
                const option = document.createElement("option");
                option.value = tag;
                option.textContent = tag;
                tagFilter.appendChild(option);
            });
    }

    /**
     * Render the problem list based on current filter values.
     */
    function renderFilteredList() {
        const container = document.getElementById("problemListContainer");

        if (!container) {
            return;
        }

        const search = document.getElementById("searchInput")?.value?.trim().toLowerCase() || "";
        const difficulty = document.getElementById("difficultyFilter")?.value || "";
        const tag = document.getElementById("tagFilter")?.value || "";

        const filtered = allProblems.filter((problem) => {
            const matchesSearch =
                !search || problem.title.toLowerCase().includes(search);

            const matchesDifficulty =
                !difficulty || problem.difficulty === difficulty;

            const matchesTag =
                !tag || (problem.tags || []).includes(tag);

            return matchesSearch && matchesDifficulty && matchesTag;
        });

        container.innerHTML = "";

        if (filtered.length === 0) {
            const emptyEl = document.createElement("p");
            emptyEl.className = "ca-problem-list__empty";
            emptyEl.textContent = "No problems match your filters.";
            container.appendChild(emptyEl);
            return;
        }

        filtered.forEach((problem) => {
            container.appendChild(createProblemCard(problem));
        });
    }

    /**
     * Wire up filter control event listeners.
     */
    function setupFilterListeners() {
        const searchInput = document.getElementById("searchInput");
        const difficultyFilter = document.getElementById("difficultyFilter");
        const tagFilter = document.getElementById("tagFilter");

        if (searchInput) {
            searchInput.addEventListener("input", renderFilteredList);
        }

        if (difficultyFilter) {
            difficultyFilter.addEventListener("change", renderFilteredList);
        }

        if (tagFilter) {
            tagFilter.addEventListener("change", renderFilteredList);
        }
    }

    /**
     * Load all problems and render the initial list.
     */
    async function initializePracticeList() {
        if (!isPracticeListPage()) {
            return;
        }

        if (!api) {
            console.error("[Practice] api.js is not loaded.");
            return;
        }

        if (!requireAuth()) {
            return;
        }

        setupFilterListeners();

        try {
            allProblems = await api.getProblems();
            populateTagFilter();
            renderFilteredList();
        } catch (error) {
            console.error("[Practice] Failed to load problems:", error);

            const container = document.getElementById("problemListContainer");

            if (container) {
                container.innerHTML = "";

                const errorEl = document.createElement("p");
                errorEl.className = "ca-problem-list__empty";
                errorEl.textContent = error?.message || "Failed to load problems.";
                container.appendChild(errorEl);
            }
        }
    }

    initializePracticeList();
});