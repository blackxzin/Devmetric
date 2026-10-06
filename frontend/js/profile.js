/**
 * DevMetrics — perfil público (/profile.html?u=username). Não exige login.
 */
(async function loadPublicProfile() {
  const root = document.getElementById("profileRoot");
  const username = new URLSearchParams(location.search).get("u");
  if (!username) {
    root.innerHTML = `<div class="empty-state">Informe um usuário: profile.html?u=seu-usuario</div>`;
    return;
  }

  let profile;
  try {
    profile = await Api.get(`/public/u/${encodeURIComponent(username)}`);
  } catch (error) {
    root.innerHTML = `<div class="empty-state">Perfil não encontrado ou privado.</div>`;
    return;
  }

  document.title = `${profile.displayName} — DevMetrics`;
  const avatar = profile.avatarUrl
    ? `<img src="${escapeHtml(profile.avatarUrl)}" alt="">`
    : escapeHtml(initialsOf(profile.displayName));
  const badgeUrl = `${Api.baseUrl}/public/u/${encodeURIComponent(profile.username)}/badge.svg`;

  root.innerHTML = `
    <div class="card reveal">
      <div class="profile-head">
        <div class="avatar">${avatar}</div>
        <div style="flex:1;min-width:200px">
          <h1 style="margin:0">${escapeHtml(profile.displayName)}</h1>
          <div class="text-muted mono">@${escapeHtml(profile.username)} · no DevMetrics desde ${formatDate(profile.memberSince)}</div>
        </div>
        <div style="text-align:right">
          <div class="profile-score">${profile.score.devScore}</div>
          <div class="text-muted">Dev Score · <span class="badge brand">${escapeHtml(profile.score.level)}</span></div>
        </div>
      </div>
      <div class="profile-stats">
        <div class="card"><strong>🔥 ${profile.streak.current}</strong><span class="text-muted">dias seguidos</span></div>
        <div class="card"><strong>${profile.streak.longest}</strong><span class="text-muted">recorde</span></div>
        <div class="card"><strong>${profile.totalActivities}</strong><span class="text-muted">atividades</span></div>
        <div class="card"><strong>${profile.totalProjects}</strong><span class="text-muted">projetos</span></div>
      </div>
    </div>

    <div class="card reveal" style="margin-top:18px">
      <div class="card-header"><div class="card-title">📅 Último ano</div></div>
      <div class="calendar-summary">${profile.calendar.totalActivities} atividades em ${profile.calendar.activeDays} dias ativos</div>
      <div class="calendar-wrap"><div id="calendarContainer"></div></div>
    </div>

    <div class="card reveal" style="margin-top:18px">
      <div class="card-header"><div class="card-title">🧰 Tecnologias</div></div>
      <div class="chip-list">
        ${profile.topTechnologies.map((tech) =>
          `<span class="badge ${tech.isNew ? "green" : "muted"}">${escapeHtml(tech.name)} · ${tech.usageCount}</span>`).join("")
          || `<span class="text-muted">Nenhuma ainda.</span>`}
      </div>
    </div>

    <div class="card reveal" style="margin-top:18px">
      <div class="card-header"><div class="card-title">🏆 Conquistas (${profile.achievements.length})</div></div>
      <div class="chip-list">
        ${profile.achievements.map((achievement) =>
          `<span class="badge brand" title="${escapeHtml(achievement.description)}">${escapeHtml(achievement.icon || "🏅")} ${escapeHtml(achievement.name)}</span>`).join("")
          || `<span class="text-muted">Nenhuma ainda.</span>`}
      </div>
    </div>

    <div class="card reveal" style="margin-top:18px">
      <div class="card-header"><div class="card-title">📎 Card para o README</div></div>
      <img src="${escapeHtml(badgeUrl)}" alt="Card DevMetrics" style="max-width:100%;border-radius:12px" />
      <pre class="mono" style="white-space:pre-wrap;word-break:break-all;font-size:12px;margin-top:12px">![DevMetrics](${escapeHtml(badgeUrl)})</pre>
    </div>
  `;

  renderCalendar(document.getElementById("calendarContainer"), profile.calendar);
  initRevealOnScroll(root);
})();
