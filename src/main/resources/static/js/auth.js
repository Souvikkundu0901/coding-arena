document.addEventListener("DOMContentLoaded", () => {

  // ---- Guard: if already logged in, skip login/register pages ----
  const onAuthPage = document.getElementById("loginBtn") || document.getElementById("registerBtn");
  if (onAuthPage && CodingArenaAPI.getToken()) {
    window.location.href = "index.html";
    return;
  }

  // ---- LOGIN ----
  const loginBtn = document.getElementById("loginBtn");
  if (loginBtn) {
    const loginForm = loginBtn.closest("form");
    if (loginForm) {
      loginForm.addEventListener("submit", handleLogin);
    } else {
      loginBtn.addEventListener("click", handleLogin);
    }
  }

  // ---- REGISTER ----
  const registerBtn = document.getElementById("registerBtn");
  if (registerBtn) {
    const registerForm = registerBtn.closest("form");
    if (registerForm) {
      registerForm.addEventListener("submit", handleRegister);
    } else {
      registerBtn.addEventListener("click", handleRegister);
    }
  }

  // ---- LOGOUT ----
  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", handleLogout);
  }

  // ---- Password show/hide toggle ----
  document.querySelectorAll(".ca-toggle-password").forEach((icon) => {
    icon.addEventListener("click", () => {
      const input = icon.parentElement.querySelector("input");
      if (input) {
        input.type = input.type === "password" ? "text" : "password";
      }
    });
  });

  // ---- Terms checkbox live validation reset ----
  const agreeTerms = document.getElementById("agreeTerms");
  if (agreeTerms) {
    agreeTerms.addEventListener("change", () => {
      const errorEl = document.getElementById("registerError");
      if (errorEl && agreeTerms.checked && errorEl.textContent.includes("Terms")) {
        errorEl.textContent = "";
        errorEl.style.display = "none";
      }
    });
  }

});

async function handleLogin(e) {
  e.preventDefault();

  try {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const result = await CodingArenaAPI.loginUser(email, password);

    CodingArenaAPI.setToken(result.token);
    localStorage.setItem("ca_user", JSON.stringify(result.user));

    window.location.href = "index.html";
  } catch (error) {
    const errorEl = document.getElementById("loginError");
    if (errorEl) {
      errorEl.textContent = error.message;
      errorEl.style.display = "block";
      errorEl.classList.remove("d-none");
    }
  }
}

async function handleRegister(e) {
  e.preventDefault();

  const errorEl = document.getElementById("registerError");
  if (errorEl) {
    errorEl.textContent = "";
    errorEl.style.display = "none";
  }

  const agreeTerms = document.getElementById("agreeTerms");
  if (agreeTerms && !agreeTerms.checked) {
    if (errorEl) {
      errorEl.textContent = "You must agree to the Terms & Conditions and Privacy Policy to register.";
      errorEl.style.display = "block";
      errorEl.classList.remove("d-none");
    }
    agreeTerms.focus();
    return;
  }

  const captchaWidget = document.querySelector(".g-recaptcha");
  let captchaToken = "";
  if (captchaWidget && typeof grecaptcha !== "undefined") {
    captchaToken = grecaptcha.getResponse();
    const siteKey = captchaWidget.getAttribute("data-sitekey");
    if (siteKey && siteKey !== "YOUR_RECAPTCHA_SITE_KEY_HERE" && !captchaToken) {
      if (errorEl) {
        errorEl.textContent = "Please complete the CAPTCHA.";
        errorEl.style.display = "block";
        errorEl.classList.remove("d-none");
      }
      return;
    }
  }

  try {
    const username = document.getElementById("username").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const result = await CodingArenaAPI.registerUser(username, email, password, captchaToken);

    CodingArenaAPI.setToken(result.token);
    localStorage.setItem("ca_user", JSON.stringify(result.user));

    window.location.href = "index.html";
  } catch (error) {
    if (typeof grecaptcha !== "undefined" && typeof grecaptcha.reset === "function") {
      try {
        grecaptcha.reset();
      } catch (e) {
        // ignore
      }
    }
    if (errorEl) {
      errorEl.textContent = error.message;
      errorEl.style.display = "block";
      errorEl.classList.remove("d-none");
    }
  }
}

function handleLogout(e) {
  e.preventDefault();

  CodingArenaAPI.clearToken();
  localStorage.removeItem("ca_user");

  window.location.href = "login.html";
}