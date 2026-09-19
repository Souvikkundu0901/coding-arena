/**
 * Coding Arena — practice-solve.html
 * CodeMirror 5 practice editor + problem loading + submission.
 */

document.addEventListener("DOMContentLoaded", () => {
    const api = window.CodingArenaAPI;

    let problemId = null;
    let codeEditor = null;

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    function isSolvePage() {
        return document.getElementById("solveCodeInput") !== null;
    }

    function requireAuth() {
        const token = api?.getToken?.();

        if (!token) {
            window.location.href = "login.html";
            return false;
        }

        return true;
    }

    function difficultyClass(difficulty) {
        const value = String(difficulty || "").toUpperCase();

        switch (value) {
            case "EASY":
                return "easy";

            case "MEDIUM":
                return "medium";

            case "HARD":
                return "hard";

            default:
                return "medium";
        }
    }

    function editorMode(language) {
        const value = String(language || "python").toLowerCase();

        switch (value) {
            case "python":
                return "python";

            case "java":
                return "text/x-java";

            case "cpp":
                return "text/x-c++src";

            case "c":
                return "text/x-csrc";

            case "javascript":
                return {
                    name: "javascript",
                    json: false
                };

            default:
                return "python";
        }
    }

    // ------------------------------------------------------------
    // CodeMirror 5
    // ------------------------------------------------------------

    function initializeCodeEditor() {
        const textarea = document.getElementById("solveCodeInput");

        if (!textarea) {
            console.error(
                "[PracticeSolve] #solveCodeInput was not found."
            );
            return false;
        }

        if (typeof window.CodeMirror === "undefined") {
            console.error(
                "[PracticeSolve] CodeMirror 5 is not loaded."
            );
            return false;
        }

        // Prevent creating the editor twice.
        if (codeEditor) {
            return true;
        }

        const languageSelect =
            document.getElementById("solveLanguageSelect");

        const selectedLanguage =
            languageSelect?.value || "python";

        codeEditor = window.CodeMirror.fromTextArea(
            textarea,
            {
                mode: editorMode(selectedLanguage),

                theme: "base16-dark",

                lineNumbers: true,

                // IMPORTANT:
                // Requires closebrackets.min.js in HTML.
                autoCloseBrackets: true,

                lineWrapping: false,

                indentUnit: 4,

                tabSize: 4,

                indentWithTabs: false,

                autofocus: false,

                matchBrackets: true,

                styleActiveLine: true,

                viewportMargin: Infinity
            }
        );

        codeEditor.setSize("100%", "100%");

        // Make sure CodeMirror renders correctly
        // after the page layout has been calculated.
        setTimeout(() => {
            if (codeEditor) {
                codeEditor.refresh();
            }
        }, 0);

        // Keep backing textarea synchronized.
        codeEditor.on("change", () => {
            codeEditor.save();
        });

        return true;
    }

    function updateEditorLanguage(language) {
        if (!codeEditor) {
            return;
        }

        codeEditor.setOption(
            "mode",
            editorMode(language)
        );

        codeEditor.refresh();
    }

    // ------------------------------------------------------------
    // Problem rendering
    // ------------------------------------------------------------

    function renderProblem(problem) {
        if (!problem) {
            return;
        }

        const titleElement =
            document.getElementById(
                "solveProblemTitle"
            );

        const difficultyElement =
            document.getElementById(
                "solveProblemDifficulty"
            );

        const tagsElement =
            document.getElementById(
                "solveProblemTags"
            );

        const descriptionElement =
            document.getElementById(
                "solveProblemDescription"
            );

        const examplesElement =
            document.getElementById(
                "solveProblemExamples"
            );

        // Title
        if (titleElement) {
            titleElement.textContent =
                problem.title ||
                "Untitled Problem";
        }

        // Difficulty
        if (difficultyElement) {
            const difficulty =
                String(
                    problem.difficulty ||
                    "MEDIUM"
                ).toUpperCase();

            difficultyElement.textContent =
                difficulty.charAt(0) +
                difficulty
                    .slice(1)
                    .toLowerCase();

            difficultyElement.className =
                "ca-diff-pill " +
                `ca-diff-pill--${difficultyClass(
                    difficulty
                )}`;
        }

        // Tags
        if (tagsElement) {
            tagsElement.innerHTML = "";

            const tags = Array.isArray(problem.tags)
                ? problem.tags
                : [];

            tags.forEach((tag) => {
                const tagElement =
                    document.createElement(
                        "span"
                    );

                tagElement.className =
                    "ca-tag-pill";

                tagElement.textContent = tag;

                tagsElement.appendChild(
                    tagElement
                );
            });
        }

        // Description
        if (descriptionElement) {
            descriptionElement.textContent =
                problem.description ||
                "No description available.";
        }

        // Examples
        if (examplesElement) {
            examplesElement.innerHTML = "";

            const examples =
                Array.isArray(problem.examples)
                    ? problem.examples
                    : [];

            if (examples.length === 0) {
                examplesElement.textContent =
                    "No examples available.";

                return;
            }

            examples.forEach((example) => {
                const exampleElement =
                    document.createElement(
                        "div"
                    );

                exampleElement.className =
                    "solve-example";

                const input =
                    example?.input ?? "";

                const output =
                    example?.output ?? "";

                exampleElement.textContent =
                    `Input: ${input}\n` +
                    `Output: ${output}`;

                examplesElement.appendChild(
                    exampleElement
                );
            });
        }
    }

    // ------------------------------------------------------------
    // Verdict
    // ------------------------------------------------------------

    function renderVerdict(verdict) {
        const verdictElement =
            document.getElementById(
                "solveVerdictDisplay"
            );

        if (!verdictElement) {
            return;
        }

        const normalized =
            String(
                verdict || "PENDING"
            ).toUpperCase();

        verdictElement.style.display =
            "block";

        verdictElement.textContent =
            normalized.replace(
                /_/g,
                " "
            );

        verdictElement.classList.remove(
            "ca-solve-verdict--accepted",
            "ca-solve-verdict--wrong",
            "ca-solve-verdict--pending"
        );

        switch (normalized) {
            case "AC":
            case "ACCEPTED":
                verdictElement.classList.add(
                    "ca-solve-verdict--accepted"
                );
                break;

            case "PENDING":
                verdictElement.classList.add(
                    "ca-solve-verdict--pending"
                );
                break;

            default:
                verdictElement.classList.add(
                    "ca-solve-verdict--wrong"
                );
                break;
        }
    }

    // ------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------

    async function handleSubmit() {
        const submitButton =
            document.getElementById(
                "solveSubmitBtn"
            );

        const textarea =
            document.getElementById(
                "solveCodeInput"
            );

        const languageSelect =
            document.getElementById(
                "solveLanguageSelect"
            );

        // Always get the current text from CodeMirror.
        const code = codeEditor
            ? codeEditor.getValue()
            : textarea?.value || "";

        const language =
            languageSelect?.value ||
            "python";

        // Keep textarea updated.
        if (textarea) {
            textarea.value = code;
        }

        // Don't submit empty code.
        if (!code.trim()) {
            renderVerdict("PENDING");
            return;
        }

        if (!api) {
            console.error(
                "[PracticeSolve] CodingArenaAPI is not available."
            );
            renderVerdict("ERROR");
            return;
        }

        if (
            typeof api.submitPracticeCode !==
            "function"
        ) {
            console.error(
                "[PracticeSolve] submitPracticeCode() is not available in api.js."
            );
            renderVerdict("ERROR");
            return;
        }

        if (submitButton) {
            submitButton.disabled = true;

            submitButton.innerHTML =
                `<span>Submitting...</span>`;
        }

        renderVerdict("PENDING");

        try {
            const result =
                await api.submitPracticeCode(
                    problemId,
                    code,
                    language
                );

            renderVerdict(
                result?.verdict ||
                "PENDING"
            );
        } catch (error) {
            console.error(
                "[PracticeSolve] Submission failed:",
                error
            );

            const verdictElement =
                document.getElementById(
                    "solveVerdictDisplay"
                );

            if (verdictElement) {
                verdictElement.style.display =
                    "block";

                verdictElement.textContent =
                    error?.message ||
                    "Submission failed. Please try again.";

                verdictElement.classList.remove(
                    "ca-solve-verdict--accepted",
                    "ca-solve-verdict--pending"
                );

                verdictElement.classList.add(
                    "ca-solve-verdict--wrong"
                );
            }
        } finally {
            if (submitButton) {
                submitButton.disabled = false;

                submitButton.innerHTML =
                    `<i class="bi bi-send-fill"></i>
                     <span>Submit Solution</span>`;
            }
        }
    }

    // ------------------------------------------------------------
    // Load problem
    // ------------------------------------------------------------

    async function loadProblem() {
        try {
            if (
                !api ||
                typeof api.getProblem !==
                "function"
            ) {
                console.error(
                    "[PracticeSolve] getProblem() is not available in api.js."
                );
                return;
            }

            const problem =
                await api.getProblem(
                    problemId
                );

            renderProblem(problem);
        } catch (error) {
            console.error(
                "[PracticeSolve] Failed to load problem:",
                error
            );

            const titleElement =
                document.getElementById(
                    "solveProblemTitle"
                );

            const descriptionElement =
                document.getElementById(
                    "solveProblemDescription"
                );

            if (titleElement) {
                titleElement.textContent =
                    "Unable to load problem";
            }

            if (descriptionElement) {
                descriptionElement.textContent =
                    error?.message ||
                    "Something went wrong while loading the problem.";
            }
        }
    }

    // ------------------------------------------------------------
    // Initialize page
    // ------------------------------------------------------------

    async function initializeSolvePage() {
        if (!isSolvePage()) {
            return;
        }

        if (!api) {
            console.error(
                "[PracticeSolve] api.js is not loaded."
            );
            return;
        }

        if (!requireAuth()) {
            return;
        }

        const urlParams =
            new URLSearchParams(
                window.location.search
            );

        problemId =
            urlParams.get("id");

        if (!problemId) {
            window.location.href =
                "practice.html";
            return;
        }

        const languageSelect =
            document.getElementById(
                "solveLanguageSelect"
            );

        const submitButton =
            document.getElementById(
                "solveSubmitBtn"
            );

        // Initialize CodeMirror BEFORE attaching
        // language/submission events.
        initializeCodeEditor();

        // Language switching
        if (languageSelect) {
            languageSelect.addEventListener(
                "change",
                (event) => {
                    updateEditorLanguage(
                        event.target.value
                    );
                }
            );
        }

        // Submit
        if (submitButton) {
            submitButton.addEventListener(
                "click",
                handleSubmit
            );
        }

        // Load problem
        await loadProblem();
    }

    initializeSolvePage();
});