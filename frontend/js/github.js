/**
 * DevMetrics — conexão OAuth com GitHub e disparo de sincronização.
 */
requireAuth();

let pollTimer = null;

async function loadStatus() {
  const card = document.getElementById("statusCard").querySelector(".tilt-layer");
  try {
    const status = await Api.get("/github/status");
    renderStatus(card, status);
  } catch (error) {
    card.innerHTML = `<div class="empty-state">Erro ao consultar o status: ${escapeHtml(error.message)}</div>`;
  }
}

function renderStatus(card, status) {
  if (!status.serverConfigured) {
    card.innerHTML = `
      <div>
        <div class="github-status-line"><span class="live-dot" style="background:var(--accent-yellow)"></span> Integração não configurada</div>
        <p class="text-muted" style="margin-top:8px;font-size:13px">
          O servidor ainda não tem <span class="mono">GITHUB_CLIENT_ID</span> e
          <span class="mono">GITHUB_CLIENT_SECRET</span> configurados.
        </p>
      </div>`;
    return;
  }

  if (!status.connected) {
    card.innerHTML = `
      <div>
        <div class="github-status-line"><span class="live-dot" style="background:var(--text-3)"></span> Nenhuma conta conectada</div>
        <p class="text-muted" style="margin-top:8px;font-size:13px;max-width:480px">
          Conecte sua conta para importar commits, Pull Requests, issues e tecnologias automaticamente.
          O acesso é somente leitura e pode ser revogado quando quiser.
        </p>
      </div>
      <button class="btn btn-primary" id="connectBtn">🔗 Conectar GitHub</button>
    `;
    document.getElementById("connectBtn").addEventListener("click", connectGitHub);
    return;
  }

  card.innerHTML = `
    <div>
      <div class="github-status-line">
        <div class="avatar" style="width:26px;height:26px">
          ${status.avatarUrl ? `<img src="${escapeHtml(status.avatarUrl)}" alt="">` : "🐙"}
        </div>
        <strong>@${escapeHtml(status.login)}</strong>
        <span class="badge green">Conectado</span>
      </div>
      <p class="text-muted" style="margin-top:8px;font-size:12.5px">
        Última sincronização: ${status.lastSyncedAt ? formatDateTime(status.lastSyncedAt) : "nunca"}
        ${status.rateLimitRemaining != null ? ` · rate limit: ${status.rateLimitRemaining}` : ""}
      </p>
    </div>
    <div style="display:flex;gap:10px">
      <button class="btn btn-primary" id="syncBtn">🔄 Sincronizar agora</button>
      <button class="btn btn-danger" id="disconnectBtn">Desconectar</button>
    </div>
  `;

  document.getElementById("syncBtn").addEventListener("click", triggerSync);
  document.getElementById("disconnectBtn").addEventListener("click", disconnectGitHub);

  loadSyncHistory();
}

async function connectGitHub() {
  try {
    const { authorizeUrl } = await Api.get("/github/authorize-url");
    location.href = authorizeUrl;
  } catch (error) {
    Toast.error(error.message);
  }
}

async function disconnectGitHub() {
  if (!confirm("Desconectar o GitHub? As atividades já importadas permanecem no seu histórico.")) return;
  try {
    await Api.del("/github/disconnect");
    Toast.success("GitHub desconectado.");
    loadStatus();
  } catch (error) {
    Toast.error(error.message);
  }
}

async function triggerSync() {
  const btn = document.getElementById("syncBtn");
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span> Sincronizando...';
  try {
    const sync = await Api.post("/github/sync");
    Toast.info("Sincronização iniciada em segundo plano.");
    pollSync(sync.syncId);
  } catch (error) {
    Toast.error(error.message);
    btn.disabled = false;
    btn.textContent = "🔄 Sincronizar agora";
  }
}

function pollSync(syncId) {
  clearInterval(pollTimer);
  pollTimer = setInterval(async () => {
    try {
      const sync = await Api.get(`/github/sync/${syncId}`);
      if (sync.status !== "RUNNING") {
        clearInterval(pollTimer);
        const btn = document.getElementById("syncBtn");
        if (btn) {
          btn.disabled = false;
          btn.textContent = "🔄 Sincronizar agora";
        }
        if (sync.status === "SUCCESS" || sync.status === "PARTIAL") {
          Toast.success(`Sincronização concluída: ${sync.activitiesCreated} atividades importadas.`);
        } else {
          Toast.error("A sincronização falhou: " + (sync.errorMessage || ""));
        }
        loadStatus();
      }
    } catch {
      clearInterval(pollTimer);
    }
  }, 3000);
}

async function loadSyncHistory() {
  const container = document.getElementById("syncHistory");
  try {
    const syncs = await Api.get("/github/sync");
    if (!syncs.length) {
      container.innerHTML = `<div class="empty-state">Nenhuma sincronização ainda.</div>`;
      return;
    }
    const statusBadge = { SUCCESS: "green", PARTIAL: "yellow", FAILED: "muted", RUNNING: "brand" };
    container.innerHTML = syncs
      .map(
        (sync) => `
        <div class="list-row">
          <div class="list-icon">🐙</div>
          <div class="list-main">
            <div class="list-title">${sync.reposScanned} repositórios · ${sync.activitiesCreated} atividades</div>
            <div class="list-sub">${formatDateTime(sync.startedAt)}</div>
          </div>
          <span class="badge ${statusBadge[sync.status] || "muted"}">${sync.status}</span>
        </div>`
      )
      .join("");
  } catch {
    container.innerHTML = `<div class="empty-state">Erro ao carregar o histórico.</div>`;
  }
}

const params = new URLSearchParams(location.search);
if (params.get("github") === "connected") {
  Toast.success("GitHub conectado com sucesso! 🎉");
}

loadStatus();
