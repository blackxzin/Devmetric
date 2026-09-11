/**
 * DevMetrics — orquestra a página de dashboard: busca os dados agregados
 * e desenha score, streak, calendário, desafio, próximo passo e resumo.
 */
requireAuth();

const TYPE_ICON = {
  COMMIT: "🔵", PULL_REQUEST: "🔀", ISSUE: "🗒️", FEATURE: "✨", BUG_FIX: "🐛",
  TEST: "🧪", DOCUMENTATION: "📘", STUDY: "📚", NEW_PROJECT: "🚀",
  REFACTOR: "🛠️", CODE_REVIEW: "👀", DEPLOY: "🐳",
};

let calendarYear = new Date().getFullYear();

async function loadDashboard() {
  const user = Api.currentUser();
  if (user) {
    document.getElementById("userFirstName").textContent = user.displayName.split(" ")[0];
  }

  await Promise.allSettled([
    loadSummary(),
    loadCalendar(calendarYear),
    loadScoreHistory(),
    loadChallenge(),
    loadNextStep(),
  ]);
}

async function loadSummary() {
  try {
    const summary = await Api.get("/dashboard/summary");
    renderScore(summary.score);
    renderStreak(summary.streak);
    renderWeek(summary.week);
    renderTechnologies(summary.topTechnologies);
    renderTotals(summary.totals);
    renderRecentActivities(summary.recentActivities);
  } catch (error) {
    Toast.error("Não foi possível carregar o resumo: " + error.message);
  }
}

function renderScore(score) {
  document.getElementById("scoreLevelBadge").textContent = score.level;
  animateCount(document.getElementById("scoreValue"), score.devScore, { duration: 1000 });

  const circumference = 2 * Math.PI * 70;
  const ratio = Math.max(0, Math.min(1, score.devScore / 1000));
  const ring = document.getElementById("scoreRingFill");
  requestAnimationFrame(() => {
    ring.setAttribute("stroke-dasharray", `${circumference * ratio} ${circumference}`);
  });

  const trend = score.trend;
  const trendEl = document.getElementById("scoreTrend");
  if (trend.direction === "UP") {
    trendEl.innerHTML = `<span style="color:var(--accent-green)">▲ +${trend.delta30d}</span> nos últimos 30 dias`;
  } else if (trend.direction === "DOWN") {
    trendEl.innerHTML = `<span style="color:var(--accent-red)">▼ ${trend.delta30d}</span> nos últimos 30 dias`;
  } else {
    trendEl.innerHTML = `<span class="text-faint">— estável</span> nos últimos 30 dias`;
  }

  const breakdown = document.getElementById("scoreBreakdown");
  breakdown.innerHTML = score.breakdown
    .map(
      (component, index) => `
      <div class="breakdown-row" data-index="${index}">
        <div class="breakdown-label">${component.label}</div>
        <div class="progress-track"><div class="progress-fill thin" style="width:${component.normalized * 100}%"></div></div>
        <div class="breakdown-points">${component.points}</div>
        <div class="breakdown-tooltip">${escapeHtml(component.explanation)}</div>
      </div>`
    )
    .join("");

  breakdown.querySelectorAll(".breakdown-row").forEach((row) => {
    row.addEventListener("click", () => row.classList.toggle("expanded"));
  });
}

function renderStreak(streak) {
  animateCount(document.getElementById("streakCurrent"), streak.current);
  document.getElementById("streakLongest").textContent = streak.longest;
  document.getElementById("streakActive30").textContent = streak.activeDaysLast30;
  const flame = document.getElementById("streakFlame");
  flame.style.opacity = streak.current > 0 ? "1" : "0.25";
  flame.style.filter = streak.current > 0 ? "" : "grayscale(1)";
}

function renderWeek(week) {
  animateCount(document.getElementById("weekActivities"), week.activities);
  animateCount(document.getElementById("weekPoints"), week.points, { decimals: 0 });
  document.getElementById("weekGoalLabel").textContent = `${week.points.toFixed(0)} / ${week.goalPoints}`;
  requestAnimationFrame(() => {
    document.getElementById("weekGoalFill").style.width = `${Math.min(100, week.goalProgress * 100)}%`;
  });
}

function renderTechnologies(technologies) {
  const list = document.getElementById("techPillList");
  if (!technologies.length) {
    list.innerHTML = `<span class="text-faint" style="font-size:12.5px">Nenhuma tecnologia registrada ainda.</span>`;
    return;
  }
  list.innerHTML = technologies
    .map(
      (tech) => `
      <span class="tech-pill ${tech.isNew ? "is-new" : ""}">
        <span class="dot"></span>${escapeHtml(tech.name)}
        ${tech.isNew ? " 🌱" : ""}
      </span>`
    )
    .join("");
}

function renderTotals(totals) {
  animateCount(document.getElementById("totalProjects"), totals.activeProjects);
  animateCount(document.getElementById("totalActivities"), totals.activities);
}

function renderRecentActivities(activities) {
  const container = document.getElementById("recentActivities");
  if (!activities.length) {
    container.innerHTML = `<div class="empty-state"><div class="empty-icon">🌱</div>Nenhuma atividade ainda. Que tal registrar a primeira?</div>`;
    return;
  }
  container.innerHTML = activities
    .map(
      (activity) => `
      <div class="list-row">
        <div class="list-icon">${TYPE_ICON[activity.type] || "•"}</div>
        <div class="list-main">
          <div class="list-title">${escapeHtml(activity.title)}</div>
          <div class="list-sub">${activity.typeLabel} · ${formatDateTime(activity.occurredAt)}${activity.projectName ? " · " + escapeHtml(activity.projectName) : ""}</div>
        </div>
        <div class="list-points">+${Number(activity.points).toFixed(1)}</div>
      </div>`
    )
    .join("");
}

async function loadCalendar(year) {
  try {
    const calendar = await Api.get("/dashboard/calendar", { year });
    document.getElementById("yearLabel").textContent = year;
    document.getElementById("calendarSummary").innerHTML = `
      <span class="text-muted" style="font-size:12.5px">${calendar.totalActivities} atividades · ${Number(calendar.totalPoints).toFixed(0)} pontos em ${calendar.activeDays} dias</span>
    `;
    renderCalendar(document.getElementById("calendarContainer"), calendar);
  } catch (error) {
    Toast.error("Erro ao carregar o calendário: " + error.message);
  }
}

async function loadScoreHistory() {
  try {
    const history = await Api.get("/score/history");
    const canvas = document.getElementById("scoreChart");
    drawLineChart(canvas, history.map((point) => point.devScore), { color: "#7c5cff" });
  } catch {
    // grafico e cosmetico: falha aqui nao deve travar o resto do dashboard
  }
}

async function loadChallenge() {
  const body = document.getElementById("challengeBody");
  const statusBadge = document.getElementById("challengeStatusBadge");
  try {
    const challenge = await Api.get("/challenges/today");
    renderChallenge(challenge);
  } catch (error) {
    body.innerHTML = `<div class="empty-state">Não foi possível carregar o desafio de hoje.</div>`;
    statusBadge.textContent = "—";
  }
}

function renderChallenge(challenge) {
  const body = document.getElementById("challengeBody");
  const statusBadge = document.getElementById("challengeStatusBadge");

  const statusMap = {
    PENDING: { label: "Pendente", cls: "yellow" },
    COMPLETED: { label: "Concluído", cls: "green" },
    SKIPPED: { label: "Pulado", cls: "muted" },
    EXPIRED: { label: "Expirado", cls: "muted" },
  };
  const status = statusMap[challenge.status] || statusMap.PENDING;
  statusBadge.className = `badge ${status.cls}`;
  statusBadge.textContent = status.label;

  body.innerHTML = `
    <div style="font-weight:700;font-size:14.5px;margin-bottom:6px">${escapeHtml(challenge.title)}</div>
    <p style="font-size:13.5px;color:var(--text-1);line-height:1.6">${escapeHtml(challenge.text)}</p>
    <div class="challenge-meta">
      <span class="badge brand">⏱ ${challenge.estimatedMinutes} minutos</span>
      <span class="badge muted">${challenge.difficulty === "FACIL" ? "Fácil" : "Médio"}</span>
    </div>
    ${
      challenge.status === "PENDING"
        ? `<div class="challenge-actions">
             <button class="btn btn-primary" id="completeChallengeBtn">✓ Concluí hoje</button>
             <button class="btn btn-ghost" id="skipChallengeBtn">Pular</button>
           </div>`
        : ""
    }
  `;

  document.getElementById("completeChallengeBtn")?.addEventListener("click", async () => {
    try {
      const updated = await Api.post(`/challenges/${challenge.id}/complete`);
      renderChallenge(updated);
      Toast.success("Desafio concluído! 🎉");
      loadSummary();
    } catch (error) {
      Toast.error(error.message);
    }
  });

  document.getElementById("skipChallengeBtn")?.addEventListener("click", async () => {
    try {
      const updated = await Api.post(`/challenges/${challenge.id}/skip`);
      renderChallenge(updated);
      Toast.info("Novo desafio sorteado.");
    } catch (error) {
      Toast.error(error.message);
    }
  });
}

async function loadNextStep() {
  const body = document.getElementById("nextStepBody");
  try {
    const nextStep = await Api.get("/insights/next-step");
    const suggestion = nextStep.suggestion;
    body.innerHTML = `
      <div class="next-step-tech">
        <div class="tech-badge-icon">🧭</div>
        <div>
          <div style="font-weight:700;font-size:15px">${escapeHtml(suggestion.technology)}</div>
          <span class="badge muted">${suggestion.category}</span>
        </div>
      </div>
      <p class="next-step-reason">${escapeHtml(suggestion.reason)}</p>
      <div class="next-step-first"><strong>Primeiro passo:</strong> ${escapeHtml(suggestion.firstStep)}</div>
      <div style="margin-top:10px"><span class="badge brand">⏱ ${suggestion.estimatedMinutes} min</span></div>
    `;
  } catch (error) {
    body.innerHTML = `<div class="empty-state">Sem sugestão por agora.</div>`;
  }
}

document.getElementById("yearPrev").addEventListener("click", () => {
  calendarYear--;
  loadCalendar(calendarYear);
});
document.getElementById("yearNext").addEventListener("click", () => {
  calendarYear++;
  loadCalendar(calendarYear);
});

document.getElementById("recalcBtn").addEventListener("click", async (event) => {
  const btn = event.currentTarget;
  btn.disabled = true;
  const original = btn.textContent;
  btn.innerHTML = '<span class="spinner"></span> Recalculando...';
  try {
    const result = await Api.post("/scoring/recalculate");
    Toast.success(`${result.processed} atividades reprocessadas.`);
    await loadDashboard();
  } catch (error) {
    Toast.error(error.message);
  } finally {
    btn.disabled = false;
    btn.textContent = original;
  }
});

loadDashboard();
