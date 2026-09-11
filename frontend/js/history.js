/**
 * DevMetrics — histórico mensal de evolução.
 */
requireAuth();

let historyYear = new Date().getFullYear();

async function loadHistory(year) {
  document.getElementById("historyYearLabel").textContent = year;
  const table = document.getElementById("historyTable");
  table.innerHTML = `<div class="skeleton" style="height:200px"></div>`;

  try {
    const history = await Api.get("/history/monthly", { year });
    drawLineChart(
      document.getElementById("historyChart"),
      history.months.map((m) => m.devScore),
      { color: "#33d6ff" }
    );
    renderHistoryTable(history);
  } catch (error) {
    table.innerHTML = `<div class="empty-state">Erro ao carregar histórico: ${escapeHtml(error.message)}</div>`;
  }
}

function renderHistoryTable(history) {
  const table = document.getElementById("historyTable");
  const rows = history.months
    .map(
      (month) => `
      <tr>
        <td style="font-weight:600">${month.label}</td>
        <td class="mono">${month.activities}</td>
        <td class="mono">${Number(month.points).toFixed(0)}</td>
        <td class="mono" style="color:${month.devScore > 0 ? "var(--accent-green)" : "var(--text-3)"}">${month.devScore}</td>
        <td class="mono">${month.newTechnologies}</td>
        <td class="mono">${month.newProjects}</td>
        <td class="mono">${month.challengesCompleted}</td>
        <td class="mono">${month.activeDays}</td>
      </tr>`
    )
    .join("");

  table.innerHTML = `
    <table style="width:100%;border-collapse:collapse;font-size:13px;min-width:640px">
      <thead>
        <tr style="text-align:left;color:var(--text-2);font-size:11.5px;text-transform:uppercase;letter-spacing:0.03em">
          <th style="padding:8px 6px">Mês</th>
          <th style="padding:8px 6px">Atividades</th>
          <th style="padding:8px 6px">Pontos</th>
          <th style="padding:8px 6px">Dev Score</th>
          <th style="padding:8px 6px">Techs novas</th>
          <th style="padding:8px 6px">Projetos</th>
          <th style="padding:8px 6px">Desafios</th>
          <th style="padding:8px 6px">Dias ativos</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
  `;
  table.querySelectorAll("tr").forEach((row, index) => {
    if (index === 0) return;
    row.style.borderBottom = "1px solid rgba(255,255,255,0.05)";
    row.querySelectorAll("td").forEach((cell) => (cell.style.padding = "10px 6px"));
  });
}

document.getElementById("historyYearPrev").addEventListener("click", () => {
  historyYear--;
  loadHistory(historyYear);
});
document.getElementById("historyYearNext").addEventListener("click", () => {
  historyYear++;
  loadHistory(historyYear);
});

loadHistory(historyYear);
