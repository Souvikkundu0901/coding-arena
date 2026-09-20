/**
 * Coding Arena — Theme Loader
 * Reads `ca_preferences.theme` from localStorage and applies `data-theme`
 * to <html> immediately so there is no flash of unstyled content.
 *
 * Must be loaded BEFORE any other scripts (placed early in <head> or first
 * <script> in <body> without defer/async).
 */

(function () {
    /**
     * Apply the given theme to <html> and <body>.
     * @param {'dark'|'light'} theme
     */
    function applyTheme(theme) {
        var mode = theme === 'light' ? 'light' : 'dark';
        var root = document.documentElement;
        root.setAttribute('data-theme', mode);
        root.setAttribute('data-bs-theme', mode);
        root.classList.remove('theme-dark', 'theme-light');
        root.classList.add('theme-' + mode);

        if (document.body) {
            document.body.setAttribute('data-theme', mode);
            document.body.setAttribute('data-bs-theme', mode);
            document.body.classList.remove('theme-dark', 'theme-light');
            document.body.classList.add('theme-' + mode);
        }
    }

    function saveTheme(theme) {
        try {
            var prefs = JSON.parse(localStorage.getItem("ca_preferences") || "{}");
            prefs.theme = theme;
            localStorage.setItem("ca_preferences", JSON.stringify(prefs));
        } catch (e) {
            // Ignore localStorage errors.
        }
    }

    function updateThemeOrb(theme) {
        var orb = document.querySelector(".ca-theme-orb");
        if (!orb) return;

        var icon = orb.querySelector("i");
        if (!icon) return;

        if (theme === "light") {
            icon.classList.remove("bi-moon-fill");
            icon.classList.add("bi-sun-fill");

            orb.setAttribute("aria-label", "Switch to dark theme");
            orb.setAttribute("title", "Switch to dark theme");
        } else {
            icon.classList.remove("bi-sun-fill");
            icon.classList.add("bi-moon-fill");

            orb.setAttribute("aria-label", "Switch to light theme");
            orb.setAttribute("title", "Switch to light theme");
        }
    }

    function setupThemeOrb() {
        var orb = document.querySelector(".ca-theme-orb");
        if (!orb) return;

        orb.addEventListener("click", function () {
            var currentTheme =
                document.documentElement.getAttribute("data-theme") === "light"
                    ? "light"
                    : "dark";

            var nextTheme = currentTheme === "light" ? "dark" : "light";

            applyTheme(nextTheme);
            saveTheme(nextTheme);
            updateThemeOrb(nextTheme);
        });

        updateThemeOrb(
            document.documentElement.getAttribute("data-theme") === "light"
                ? "light"
                : "dark"
        );
    }

    // Expose for settings.js to call on live toggle
    window.CodingArenaTheme = { applyTheme: applyTheme };
    window.CodessTheme = window.CodingArenaTheme;

    function init() {
        try {
            var prefs = JSON.parse(localStorage.getItem('ca_preferences') || '{}');
            applyTheme(prefs.theme || 'dark');
        } catch (e) {
            applyTheme('dark');
        }
    }

    // Eagerly apply to <html>
    init();

    // Re-apply to <body> when DOM is ready
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () {
            init();
            setupThemeOrb();
        });
    } else {
        init();
        setupThemeOrb();
    }
})();
