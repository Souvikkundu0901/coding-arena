/**
 * Coding Arena — settings.html initializer.
 * Handles account (email/password), preferences (language/theme/
 * notifications), and danger zone (logout/delete account) actions.
 *
 * Note: "Log Out" on this page reuses the same #logoutBtn id/pattern
 * that auth.js already listens for, so no duplicate logic is needed
 * here for that button.
 */

document.addEventListener("DOMContentLoaded", () => {
    const api = window.CodingArenaAPI;

    /**
     * Guard: only run on the settings page.
     * @returns {boolean}
     */
    function isSettingsPage() {
        return Boolean(document.getElementById("changeEmailForm"));
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
     * Build a DiceBear avatar URL from a seed.
     * @param {string} seed
     * @returns {string}
     */
    function getCharacterNumberFromSeed(seed) {
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

    /**
     * Render the header avatar/username pill and pre-fill the email field.
     * @param {Object} user
     */
    function renderHeaderAndForm(user) {
        const settingsAvatar = document.getElementById("settingsAvatar");
        const settingsUsername = document.getElementById("settingsUsername");
        const emailInput = document.getElementById("emailInput");

        if (settingsAvatar) {
            const characterNumber =
                getCharacterNumberFromSeed(user?.avatarSeed);

            settingsAvatar.src =
                `characters/character-${String(characterNumber).padStart(2, "0")}.png`;

            settingsAvatar.alt =
                `${user?.username || "Player"}'s character`;
        }

        if (settingsUsername) {
            settingsUsername.textContent = user?.username || "Player";
        }

        if (emailInput) {
            emailInput.placeholder = user?.email || "Enter new email";
        }
    }

    /**
     * Apply saved preferences to the form controls.
     * @param {Object} preferences
     */
    function renderPreferences(preferences = {}) {
        const darkThemeToggle = document.getElementById("darkThemeToggle");
        const notificationToggle = document.getElementById("notificationToggle");

        if (darkThemeToggle) {
            darkThemeToggle.checked = preferences.theme === "dark";
        }

        if (notificationToggle) {
            notificationToggle.checked = preferences.emailNotifications !== false;
        }
    }

    /**
     * Show a temporary inline status message next to a form.
     * @param {HTMLElement} formEl
     * @param {string} message
     * @param {boolean} [isError=false]
     */
    function showFormStatus(formEl, message, isError = false) {
        if (!formEl) {
            return;
        }

        let statusEl = formEl.querySelector(".ca-form-status");

        if (!statusEl) {
            statusEl = document.createElement("div");
            statusEl.className = "ca-form-status";
            formEl.appendChild(statusEl);
        }

        statusEl.textContent = message;
        statusEl.style.color = isError ? "var(--ca-red, #FF5D5D)" : "var(--ca-green, #2FBF71)";
        statusEl.style.fontWeight = "700";
        statusEl.style.marginTop = "8px";

        setTimeout(() => {
            statusEl.remove();
        }, 4000);
    }

    /**
     * Handle the change-email form submission.
     * @param {SubmitEvent} e
     */
    async function handleChangeEmail(e) {
        e.preventDefault();

        const form = e.target;
        const btn = document.getElementById("changeEmailBtn");
        const emailInput = document.getElementById("emailInput");
        const passwordInput = document.getElementById("emailChangePasswordInput");
        const newEmail = emailInput?.value?.trim();
        const password = passwordInput?.value || "";

        if (!newEmail) {
            showFormStatus(form, "Please enter an email address.", true);
            return;
        }

        if (!password) {
            showFormStatus(form, "Please enter your password to confirm.", true);
            return;
        }

        if (btn) {
            btn.disabled = true;
            btn.textContent = "Updating...";
        }

        try {
            await api.changeEmail(newEmail, password);

            const storedUser = JSON.parse(localStorage.getItem("ca_user") || "{}");
            storedUser.email = newEmail;
            localStorage.setItem("ca_user", JSON.stringify(storedUser));

            emailInput.value = "";
            passwordInput.value = "";
            emailInput.placeholder = newEmail;

            showFormStatus(form, "Email updated successfully.");
        } catch (error) {
            console.error("[Settings] Failed to change email:", error);
            showFormStatus(form, error?.message || "Failed to update email.", true);
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.textContent = "Update Email";
            }
        }
    }

    /**
     * Handle the change-password form submission.
     * @param {SubmitEvent} e
     */
    async function handleChangePassword(e) {
        e.preventDefault();

        const form = e.target;
        const btn = document.getElementById("changePasswordBtn");
        const currentPasswordInput = document.getElementById("currentPasswordInput");
        const newPasswordInput = document.getElementById("newPasswordInput");
        const confirmPasswordInput = document.getElementById("confirmPasswordInput");

        const currentPassword = currentPasswordInput?.value || "";
        const newPassword = newPasswordInput?.value || "";
        const confirmPassword = confirmPasswordInput?.value || "";

        if (newPassword !== confirmPassword) {
            showFormStatus(form, "New password and confirmation do not match.", true);
            return;
        }

        if (btn) {
            btn.disabled = true;
            btn.textContent = "Updating...";
        }

        try {
            await api.changePassword(currentPassword, newPassword);

            currentPasswordInput.value = "";
            newPasswordInput.value = "";
            confirmPasswordInput.value = "";

            showFormStatus(form, "Password changed successfully.");
        } catch (error) {
            console.error("[Settings] Failed to change password:", error);
            showFormStatus(form, error?.message || "Failed to change password.", true);
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.textContent = "Change Password";
            }
        }
    }

    /**
     * Persist a single preference field immediately on change.
     * @param {string} key
     * @param {*} value
     */
    async function persistPreference(key, value) {
        try {
            await api.updatePreferences({ [key]: value });
        } catch (error) {
            console.error(`[Settings] Failed to save preference "${key}":`, error);
        }
    }

    /**
     * Wire up all page event listeners.
     */
    function setupEventListeners() {
        const changeEmailForm = document.getElementById("changeEmailForm");
        const changePasswordForm = document.getElementById("changePasswordForm");
        const darkThemeToggle = document.getElementById("darkThemeToggle");
        const notificationToggle = document.getElementById("notificationToggle");
        const deleteAccountBtn = document.getElementById("deleteAccountBtn");

        if (changeEmailForm) {
            changeEmailForm.addEventListener("submit", handleChangeEmail);
        }

        if (changePasswordForm) {
            changePasswordForm.addEventListener("submit", handleChangePassword);
        }

        if (darkThemeToggle) {
            darkThemeToggle.addEventListener("change", () => {
                const theme = darkThemeToggle.checked ? "dark" : "light";

                CodingArenaTheme?.applyTheme(theme);

                persistPreference("theme", theme);
            });
        }

        if (notificationToggle) {
            notificationToggle.addEventListener("change", () => {
                persistPreference("emailNotifications", notificationToggle.checked);
            });
        }

        if (deleteAccountBtn) {
            deleteAccountBtn.addEventListener("click", handleDeleteAccount);
        }
    }

    /**
     * Handle the "Delete Account" danger-zone action.
     */
    async function handleDeleteAccount() {
        const confirmed = window.confirm(
            "This will permanently delete your account and all match history. " +
            "This cannot be undone. Are you sure?"
        );

        if (!confirmed) {
            return;
        }

        const password = window.prompt("Enter your password to confirm account deletion:");

        if (!password) {
            return;
        }

        const btn = document.getElementById("deleteAccountBtn");

        if (btn) {
            btn.disabled = true;
            btn.textContent = "Deleting...";
        }

        try {
            await api.deleteAccount(password);

            api.clearToken?.();
            localStorage.removeItem("ca_user");
            localStorage.removeItem("ca_preferences");

            window.location.href = "login.html";
        } catch (error) {
            console.error("[Settings] Failed to delete account:", error);
            alert(error?.message || "Failed to delete account. Please try again.");

            if (btn) {
                btn.disabled = false;
                btn.innerHTML = '<i class="bi bi-trash-fill"></i> Delete Account';
            }
        }
    }

    /**
     * Load and render everything the settings page needs.
     */
    async function initializeSettings() {
        if (!isSettingsPage()) {
            return;
        }

        if (!api) {
            console.error("[Settings] api.js is not loaded.");
            return;
        }

        if (!requireAuth()) {
            return;
        }

        setupEventListeners();

        try {
            const user = await api.getCurrentUser();
            localStorage.setItem("ca_user", JSON.stringify(user));
            renderHeaderAndForm(user);
        } catch (error) {
            console.error("[Settings] Failed to load current user:", error);
            api.clearToken?.();
            window.location.href = "login.html";
            return;
        }

        try {
            const preferences = await api.getPreferences();
            renderPreferences(preferences);
        } catch (error) {
            console.error("[Settings] Failed to load preferences:", error);
        }
    }

    initializeSettings();
});