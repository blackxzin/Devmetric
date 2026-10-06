/**
 * DevMetrics — página de projetos: listagem, criação, arquivamento.
 */
requireAuth();

let showArchived = false;

async function loadProjects() {
  const grid = document.getElementById("projectGrid");
  grid.innerHTML = `<div class="skeleton" style="height:160px"></div><div class="skeleton" style="height:160px"></div>`;
  try {
    const projects = await Api.get("/projects", { status: showArchived ? "all" : "active", size: 100 });
    renderProjects(projects);
  } catch (error) {
    grid.innerHTML = `<div class="empty-state">Erro ao carregar projetos: ${escapeHtml(error.message)}</div>`;
  }
}

function renderProjects(projects) {
  const grid = document.getElementById("projectGrid");
  if (!projects.length) {
    grid.innerHTML = `<div class="empty-state"><div class="empty-icon">📦</div>Nenhum projeto ainda. Crie o primeiro!</div>`;
    return;
  }
  grid.innerHTML = projects
    .map(
      (project) => `
      <div class="card tilt-card project-card" data-tilt-max="4">
        <div class="tilt-layer">
          <div style="display:flex;justify-content:space-between;align-items:flex-start">
            <div class="project-name">${escapeHtml(project.name)}</div>
            <span class="badge ${project.source === "MANUAL" ? "muted" : "brand"}">${SOURCE_LABELS[project.source] || "Manual"}</span>
          </div>
          <div class="project-desc">${escapeHtml(project.description || "Sem descrição.")}</div>
          <div class="tech-pill-list">
            ${project.technologies.slice(0, 4).map((tech) => `<span class="tech-pill">${escapeHtml(tech.name)}</span>`).join("")}
          </div>
          <div class="project-footer">
            <span>Desde ${formatDate(project.startedAt)}</span>
            <div style="display:flex;gap:6px">
              ${
                project.readOnly
                  ? ""
                  : `<button class="btn btn-ghost" style="padding:5px 9px" data-archive="${project.id}" data-archived="${project.archived}">
                       ${project.archived ? "↩" : "🗄"}
                     </button>`
              }
              ${project.repoUrl ? `<a class="btn btn-ghost" style="padding:5px 9px" href="${escapeHtml(project.repoUrl)}" target="_blank" rel="noopener">↗</a>` : ""}
            </div>
          </div>
        </div>
      </div>`
    )
    .join("");

  grid.querySelectorAll("[data-archive]").forEach((btn) => {
    btn.addEventListener("click", () => toggleArchive(Number(btn.dataset.archive), btn.dataset.archived === "true"));
  });

  initTiltCards(grid);
}

async function toggleArchive(id, isArchived) {
  try {
    if (isArchived) {
      await Api.post(`/projects/${id}/unarchive`);
      Toast.success("Projeto reativado.");
    } else {
      await Api.post(`/projects/${id}/archive`);
      Toast.success("Projeto arquivado.");
    }
    loadProjects();
  } catch (error) {
    Toast.error(error.message);
  }
}

document.getElementById("toggleArchivedBtn").addEventListener("click", (event) => {
  showArchived = !showArchived;
  event.currentTarget.textContent = showArchived ? "Ocultar arquivados" : "Mostrar arquivados";
  loadProjects();
});

document.getElementById("newProjectBtn").addEventListener("click", () => {
  document.getElementById("projectForm").reset();
  document.getElementById("projectStartedAt").value = new Date().toISOString().slice(0, 10);
  Modal.open("projectModal");
});

document.getElementById("projectForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  const submitBtn = document.getElementById("projectSubmitBtn");
  submitBtn.disabled = true;
  try {
    await Api.post("/projects", {
      name: document.getElementById("projectName").value.trim(),
      description: document.getElementById("projectDescription").value.trim() || null,
      startedAt: document.getElementById("projectStartedAt").value,
      repoUrl: document.getElementById("projectRepoUrl").value.trim() || null,
    });
    Toast.success("Projeto criado! +25 pontos de projeto novo.");
    Modal.close("projectModal");
    loadProjects();
  } catch (error) {
    Toast.error(error.message);
  } finally {
    submitBtn.disabled = false;
  }
});

loadProjects();
