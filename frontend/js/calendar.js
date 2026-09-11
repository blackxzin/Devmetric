/**
 * DevMetrics — calendário de atividade (heatmap estilo GitHub).
 * Recebe os dias vindos de GET /dashboard/calendar e desenha semana a semana,
 * alinhando a primeira coluna ao domingo anterior ao primeiro dia do ano.
 */
function renderCalendar(container, calendarData) {
  container.innerHTML = "";

  const days = calendarData.days;
  const dayByDate = new Map(days.map((d) => [d.date, d]));
  const firstDate = new Date(days[0].date + "T00:00:00");
  const lastDate = new Date(days[days.length - 1].date + "T00:00:00");

  const gridStart = new Date(firstDate);
  gridStart.setDate(gridStart.getDate() - gridStart.getDay());

  const weekday = document.createElement("div");
  weekday.className = "calendar-weekdays";
  ["", "S", "", "T", "", "Q", ""].forEach((label) => {
    const span = document.createElement("span");
    span.textContent = label;
    weekday.appendChild(span);
  });

  const grid = document.createElement("div");
  grid.className = "calendar-grid";

  const todayIso = new Date().toISOString().slice(0, 10);
  const months = [];
  let cursor = new Date(gridStart);
  let dayIndex = 0;
  let currentWeek = null;
  let lastMonthLabeled = -1;

  while (cursor <= lastDate) {
    if (dayIndex % 7 === 0) {
      currentWeek = document.createElement("div");
      currentWeek.className = "calendar-week";
      grid.appendChild(currentWeek);

      const month = cursor.getMonth();
      if (month !== lastMonthLabeled) {
        months.push(cursor.toLocaleDateString("pt-BR", { month: "short" }));
        lastMonthLabeled = month;
      } else {
        months.push("");
      }
    }

    const iso = cursor.toISOString().slice(0, 10);
    const cell = document.createElement("div");
    cell.className = "calendar-day";

    const info = dayByDate.get(iso);
    if (cursor >= firstDate && cursor <= lastDate) {
      cell.dataset.level = info ? info.level : 0;
      cell.dataset.date = iso;
      cell.dataset.count = info ? info.count : 0;
      cell.dataset.points = info ? info.points : 0;
      cell.style.animationDelay = `${Math.min(dayIndex * 1.1, 500)}ms`;
      if (iso === todayIso) cell.classList.add("is-today");

      cell.addEventListener("mouseenter", (event) => showCalendarTooltip(event, cell));
      cell.addEventListener("mousemove", (event) => moveCalendarTooltip(event));
      cell.addEventListener("mouseleave", hideCalendarTooltip);
    } else {
      cell.style.visibility = "hidden";
    }

    currentWeek.appendChild(cell);
    cursor.setDate(cursor.getDate() + 1);
    dayIndex++;
  }

  const monthsRow = document.createElement("div");
  monthsRow.className = "calendar-months";
  months.forEach((label) => {
    const span = document.createElement("span");
    span.textContent = label;
    monthsRow.appendChild(span);
  });

  const scroller = document.createElement("div");
  scroller.className = "calendar-scroller";
  const inner = document.createElement("div");
  inner.style.display = "flex";
  inner.appendChild(weekday);
  inner.appendChild(grid);
  scroller.appendChild(monthsRow);
  scroller.appendChild(inner);

  container.appendChild(scroller);
}

let calendarTooltipEl = null;

function showCalendarTooltip(event, cell) {
  if (!calendarTooltipEl) {
    calendarTooltipEl = document.createElement("div");
    calendarTooltipEl.className = "calendar-tooltip";
    document.body.appendChild(calendarTooltipEl);
  }
  const date = new Date(cell.dataset.date + "T00:00:00");
  const label = date.toLocaleDateString("pt-BR", { day: "2-digit", month: "long", year: "numeric" });
  const count = Number(cell.dataset.count);
  const points = Number(cell.dataset.points);
  calendarTooltipEl.innerHTML = `
    <div class="tooltip-date">${label}</div>
    <div>${count} atividade${count === 1 ? "" : "s"} · <span class="tooltip-points">${points.toFixed(1)} pts</span></div>
  `;
  moveCalendarTooltip(event);
  requestAnimationFrame(() => calendarTooltipEl.classList.add("visible"));
}

function moveCalendarTooltip(event) {
  if (!calendarTooltipEl) return;
  calendarTooltipEl.style.left = `${event.clientX}px`;
  calendarTooltipEl.style.top = `${event.clientY}px`;
}

function hideCalendarTooltip() {
  calendarTooltipEl?.classList.remove("visible");
}
