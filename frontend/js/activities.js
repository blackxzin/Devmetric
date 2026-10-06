/**
 * DevMetrics — página de atividades: listagem com filtros, paginação e CRUD manual.
 */
requireAuth();

const TYPE_ICON_MAP = {
  COMMIT: "🔵", PULL_REQUEST: "🔀", ISSUE: "🗒️", FEATURE: "✨", BUG_FIX: "🐛",
  TEST: "🧪", DOCUMENTATION: "📘", STUDY: "📚", NEW_PROJECT: "🚀",
  REFACTOR: "🛠️", CODE_REVIEW: "👀", DEPLOY: "🐳",
};

let currentPage = 0;
let activityTypes = [];

async function bootstrap() {
  try {
    activityTypes = await Api.get("/activities/types");
    fillTypeSelects(activityTypes);
  } catch (error) {
    Toast.error("Erro ao carregar tipos de atividade.");
  }

  try {
    const projects = await Api.get("/projects", { status: "active", size: 100 });
    const select = document.getElementById("activityProject");
    projects.forEach((project) => {
      const option = document.createElement("option");
      option.value = project.id;
      option.textContent = project.name;
      select.appendChild(option);
    });
  } catch {
    // formulario funciona sem projetos tambem
  }

  try {
    const technologies = await Api.get("/technologies");
    const select = document.getElementById("activityTechnology");
    technologies.forEach((tech) => {
      const option = document.createElement("option");
      option.value = tech.id;
      option.textContent = tech.name;
      select.appendChild(option);
    });
  } catch {
    // idem
  }

  loadActivities();
}

function fillTypeSelects(types) {
  const filterSelect = document.getElementById("filterType");
  const formSelect = document.getElementById("activityType");
  types.forEach((type) => {
    filterSelect.appendChild(new Option(type.label, type.value));
    formSelect.appendChild(new Option(type.label, type.value));
  });
}

async function loadActivities(page = 0) {
  currentPage = page;
  const list = document.getElementById("activityList");
  list.innerHTML = `<div class="skeleton" style="height:200px"></div>`;

  const params = {
    page,
    size: 15,
    type: document.getElementById("filterType").value || undefined,
    source: document.getElementById("filterSource").value || undefined,
    from: document.getElementById("filterFrom").value || undefined,
    to: document.getElementById("filterTo").value || undefined,
  };

  try {
    const { data, meta } = await Api.getPaged("/activities", params);
    renderActivityList(data);
    renderPagination(meta);
  } catch (error) {
    list.innerHTML = `<div class="empty-state">Erro ao carregar atividades: ${escapeHtml(error.message)}</div>`;
  }
}

function renderActivityList(activities) {
  const list = document.getElementById("activityList");
  if (!activities.length) {
    list.innerHTML = `<div class="empty-state"><div class="empty-icon">🌱</div>Nenhuma atividade encontrada com esse filtro.</div>`;
    return;
  }
  list.innerHTML = activities
    .map(
      (activity) => `
      <div class="list-row">
        <div class="list-icon">${TYPE_ICON_MAP[activity.type] || "•"}</div>
        <div class="list-main">
          <div class="list-title">${escapeHtml(activity.title)}</div>
          <div class="list-sub">
            ${activity.typeLabel} · ${SOURCE_LABELS[activity.source] || "Manual"} ·
            ${formatDateTime(activity.occurredAt)}
            ${activity.projectName ? " · " + escapeHtml(activity.projectName) : ""}
          </div>
        </div>
        <div class="list-points">+${Number(activity.points).toFixed(1)}</div>
        ${
          activity.readOnly
            ? ""
            : `<div style="display:flex;gap:4px">
                 <button class="btn btn-ghost" style="padding:6px 10px" data-edit="${activity.id}">✏️</button>
                 <button class="btn btn-ghost" style="padding:6px 10px" data-delete="${activity.id}">🗑️</button>
               </div>`
        }
      </div>`
    )
    .join("");

  list.querySelectorAll("[data-edit]").forEach((btn) => {
    btn.addEventListener("click", () => openEditModal(Number(btn.dataset.edit), activities));
  });
  list.querySelectorAll("[data-delete]").forEach((btn) => {
    btn.addEventListener("click", () => deleteActivity(Number(btn.dataset.delete)));
  });
}

function renderPagination(meta) {
  const container = document.getElementById("activityPagination");
  if (!meta || meta.totalPages <= 1) {
    container.innerHTML = "";
    return;
  }
  let html = "";
  for (let i = 0; i < meta.totalPages; i++) {
    html += `<button class="btn ${i === meta.page ? "btn-primary" : "btn-ghost"}" style="padding:8px 14px" data-page="${i}">${i + 1}</button>`;
  }
  container.innerHTML = html;
  container.querySelectorAll("[data-page]").forEach((btn) => {
    btn.addEventListener("click", () => loadActivities(Number(btn.dataset.page)));
  });
}

function openCreateModal() {
  document.getElementById("modalTitle").textContent = "Nova atividade";
  document.getElementById("activityId").value = "";
  document.getElementById("activityForm").reset();
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  document.getElementById("activityOccurredAt").value = now.toISOString().slice(0, 16);
  Modal.open("activityModal");
}

function openEditModal(id, activities) {
  const activity = activities.find((item) => item.id === id);
  if (!activity) return;
  document.getElementById("modalTitle").textContent = "Editar atividade";
  document.getElementById("activityId").value = activity.id;
  document.getElementById("activityType").value = activity.type;
  document.getElementById("activityTitle").value = activity.title;
  document.getElementById("activityDescription").value = activity.description || "";
  document.getElementById("activityProject").value = activity.projectId || "";
  document.getElementById("activityTechnology").value = activity.technologyId || "";
  const occurred = new Date(activity.occurredAt);
  occurred.setMinutes(occurred.getMinutes() - occurred.getTimezoneOffset());
  document.getElementById("activityOccurredAt").value = occurred.toISOString().slice(0, 16);
  Modal.open("activityModal");
}

async function deleteActivity(id) {
  if (!confirm("Remover esta atividade? Os pontos do dia serão recalculados.")) return;
  try {
    await Api.del(`/activities/${id}`);
    Toast.success("Atividade removida.");
    loadActivities(currentPage);
  } catch (error) {
    Toast.error(error.message);
  }
}

document.getElementById("newActivityBtn").addEventListener("click", openCreateModal);

document.getElementById("activityForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const id = document.getElementById("activityId").value;
  const payload = {
    type: document.getElementById("activityType").value,
    title: document.getElementById("activityTitle").value.trim(),
    description: document.getElementById("activityDescription").value.trim() || null,
    projectId: document.getElementById("activityProject").value || null,
    technologyId: document.getElementById("activityTechnology").value || null,
    occurredAt: new Date(document.getElementById("activityOccurredAt").value).toISOString(),
  };

  const submitBtn = document.getElementById("activitySubmitBtn");
  submitBtn.disabled = true;
  try {
    if (id) {
      await Api.put(`/activities/${id}`, payload);
      Toast.success("Atividade atualizada.");
    } else {
      await Api.post("/activities", payload);
      Toast.success("Atividade registrada! 🎉");
    }
    Modal.close("activityModal");
    loadActivities(currentPage);
  } catch (error) {
    Toast.error(error.message);
  } finally {
    submitBtn.disabled = false;
  }
});

["filterType", "filterSource", "filterFrom", "filterTo"].forEach((id) => {
  document.getElementById(id).addEventListener("change", () => loadActivities(0));
});

bootstrap();
