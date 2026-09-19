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

  try {
    const username = document.getElementById("username").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const result = await CodingArenaAPI.registerUser(username, email, password);

    CodingArenaAPI.setToken(result.token);
    localStorage.setItem("ca_user", JSON.stringify(result.user));

    window.location.href = "index.html";
  } catch (error) {
    const errorEl = document.getElementById("registerError");
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