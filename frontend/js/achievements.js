/**
 * DevMetrics — página de conquistas.
 */
requireAuth();

async function loadAchievements() {
  const grid = document.getElementById("achievementGrid");
  grid.innerHTML = Array.from({ length: 8 }).map(() => `<div class="skeleton" style="height:140px"></div>`).join("");

  try {
    const achievements = await Api.get("/achievements");
    renderAchievements(achievements);
  } catch (error) {
    grid.innerHTML = `<div class="empty-state">Erro ao carregar conquistas: ${escapeHtml(error.message)}</div>`;
  }
}

function renderAchievements(achievements) {
  const grid = document.getElementById("achievementGrid");
  const unlocked = achievements.filter((item) => item.unlocked);
  animateCount(document.getElementById("unlockedCount"), unlocked.length);
  animateCount(document.getElementById("totalCount"), achievements.length);

  const sorted = [...achievements].sort((a, b) => Number(b.unlocked) - Number(a.unlocked));

  grid.innerHTML = sorted
    .map(
      (achievement) => `
      <div class="achievement-card tilt-card ${achievement.unlocked ? "unlocked" : "locked"}" data-tilt-max="8">
        <div class="tilt-layer">
          <div class="achievement-icon">${achievement.icon || "🏅"}</div>
          <div class="achievement-name">${escapeHtml(achievement.name)}</div>
          <div class="achievement-desc">${escapeHtml(achievement.description)}</div>
          ${
            achievement.unlocked
              ? `<div class="badge green" style="margin-top:10px">Desbloqueada</div>`
              : `<div class="achievement-progress">
                   <div class="progress-track"><div class="progress-fill thin" style="width:${achievement.progressRatio * 100}%"></div></div>
                   <div class="text-faint" style="font-size:11px;margin-top:6px">${achievement.progress} / ${achievement.threshold}</div>
                 </div>`
          }
        </div>
      </div>`
    )
    .join("");

  initTiltCards(grid);
}

loadAchievements();
