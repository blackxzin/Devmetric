/**
 * DevMetrics — barra de navegação compartilhada.
 * Cada página só precisa de <div id="navbar" data-active="dashboard"></div>.
 */
(function renderNavbar() {
  const mount = document.getElementById("navbar");
  if (!mount) return;

  const active = mount.dataset.active || "";
  const user = Api.currentUser();

  const links = [
    { key: "dashboard", href: "dashboard.html", label: "Dashboard" },
    { key: "activities", href: "activities.html", label: "Atividades" },
    { key: "projects", href: "projects.html", label: "Projetos" },
    { key: "achievements", href: "achievements.html", label: "Conquistas" },
    { key: "history", href: "history.html", label: "Histórico" },
    { key: "github", href: "github.html", label: "Integrações" },
    { key: "settings", href: "settings.html", label: "Configurações" },
  ];

  const navLinks = links
    .map(
      (link) =>
        `<a href="${link.href}" class="${link.key === active ? "active" : ""}">${link.label}</a>`
    )
    .join("");

  const initials = escapeHtml(initialsOf(user?.displayName));
  const avatar = user?.avatarUrl
    ? `<img src="${escapeHtml(user.avatarUrl)}" alt="">`
    : initials;

  mount.innerHTML = `
    <div class="container navbar">
      <a href="dashboard.html" class="brand">
        <span class="brand-mark floating-icon">D</span>
        DevMetrics
      </a>
      <nav>${navLinks}</nav>
      <div class="user-chip" id="userChip">
        <div class="avatar">${avatar}</div>
        <span class="mono" style="font-size:12.5px;color:var(--text-1)">${escapeHtml(user?.displayName || "")}</span>
      </div>
    </div>
  `;

  document.getElementById("userChip")?.addEventListener("click", async () => {
    if (confirm("Sair da sua conta DevMetrics?")) {
      await Api.logout();
      location.href = "login.html";
    }
  });
})();
