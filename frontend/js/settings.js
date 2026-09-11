/**
 * DevMetrics — página de configurações: perfil, senha e regras de pontuação.
 */
requireAuth();

async function loadProfile() {
  try {
    const profile = await Api.get("/users/me");
    document.getElementById("displayName").value = profile.user.displayName;
    document.getElementById("timezone").value = profile.user.timezone;
    document.getElementById("weeklyGoalPoints").value = profile.user.weeklyGoalPoints;
  } catch (error) {
    Toast.error(error.message);
  }
}

document.getElementById("profileForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await Api.patch("/users/me", {
      displayName: document.getElementById("displayName").value.trim(),
      timezone: document.getElementById("timezone").value.trim(),
      weeklyGoalPoints: Number(document.getElementById("weeklyGoalPoints").value),
    });
    const auth = Api.readAuth();
    if (auth) {
      auth.user.displayName = document.getElementById("displayName").value.trim();
      Api.writeAuth(auth);
    }
    Toast.success("Perfil atualizado.");
  } catch (error) {
    Toast.error(error.message);
  }
});

document.getElementById("passwordForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    await Api.put("/users/me/password", {
      currentPassword: document.getElementById("currentPassword").value,
      newPassword: document.getElementById("newPassword").value,
    });
    Toast.success("Senha atualizada.");
    event.target.reset();
  } catch (error) {
    Toast.error(error.message);
  }
});

async function loadRules() {
  const container = document.getElementById("rulesList");
  container.innerHTML = `<div class="skeleton" style="height:200px"></div>`;
  try {
    const rules = await Api.get("/scoring/rules");
    renderRules(rules);
  } catch (error) {
    container.innerHTML = `<div class="empty-state">Erro ao carregar regras: ${escapeHtml(error.message)}</div>`;
  }
}

function renderRules(rules) {
  const container = document.getElementById("rulesList");
  container.innerHTML = rules
    .map(
      (rule) => `
      <div class="list-row" data-type="${rule.activityType}">
        <div class="list-main">
          <div class="list-title">${rule.label} ${rule.customized ? '<span class="badge brand" style="margin-left:6px">personalizado</span>' : ""}</div>
          <div class="list-sub">Limite diário: ${rule.dailyCap ?? "sem limite"} · Rendimento decrescente: ${rule.diminishing ? "sim" : "não"}</div>
        </div>
        <input type="number" step="0.5" min="0" max="100" value="${rule.basePoints}" class="rule-points" style="width:80px;padding:8px;border-radius:8px;border:1px solid var(--border);background:var(--surface);color:var(--text-0)" />
        <button class="btn btn-ghost" style="padding:8px 12px" data-save="${rule.activityType}">Salvar</button>
      </div>`
    )
    .join("");

  container.querySelectorAll("[data-save]").forEach((btn) => {
    btn.addEventListener("click", async () => {
      const row = btn.closest(".list-row");
      const basePoints = Number(row.querySelector(".rule-points").value);
      try {
        await Api.put(`/scoring/rules/${btn.dataset.save}`, {
          basePoints,
          dailyCap: null,
          diminishing: true,
          active: true,
        });
        Toast.success("Regra atualizada. Use recalcular para aplicar em pontos antigos.");
        loadRules();
      } catch (error) {
        Toast.error(error.message);
      }
    });
  });
}

document.getElementById("recalcFromSettings").addEventListener("click", async (event) => {
  const btn = event.currentTarget;
  btn.disabled = true;
  try {
    const result = await Api.post("/scoring/recalculate");
    Toast.success(`${result.processed} atividades reprocessadas.`);
  } catch (error) {
    Toast.error(error.message);
  } finally {
    btn.disabled = false;
  }
});

document.querySelectorAll(".tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach((t) => t.classList.remove("active"));
    tab.classList.add("active");
    document.getElementById("tab-profile").style.display = tab.dataset.tab === "profile" ? "" : "none";
    document.getElementById("tab-scoring").style.display = tab.dataset.tab === "scoring" ? "" : "none";
    if (tab.dataset.tab === "scoring") {
      loadRules();
    }
  });
});

loadProfile();
