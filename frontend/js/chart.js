/**
 * DevMetrics — mini gráfico de linha em canvas, sem dependências externas.
 * Usado para a evolução do Dev Score e para o histórico mensal.
 */
function drawLineChart(canvas, values, options = {}) {
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const rect = canvas.parentElement.getBoundingClientRect();
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  canvas.style.width = rect.width + "px";
  canvas.style.height = rect.height + "px";
  const ctx = canvas.getContext("2d");
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

  const width = rect.width;
  const height = rect.height;
  const padding = { top: 14, right: 10, bottom: 20, left: 10 };
  const color = options.color || "#7c5cff";
  const labels = options.labels || [];

  ctx.clearRect(0, 0, width, height);

  if (!values.length) {
    ctx.fillStyle = "rgba(255,255,255,0.35)";
    ctx.font = "12px Inter, sans-serif";
    ctx.textAlign = "center";
    ctx.fillText("Sem dados ainda", width / 2, height / 2);
    return;
  }

  const max = Math.max(...values, options.min || 0) || 1;
  const min = Math.min(...values, 0);
  const chartW = width - padding.left - padding.right;
  const chartH = height - padding.top - padding.bottom;

  const points = values.map((value, index) => {
    const x = padding.left + (values.length === 1 ? chartW / 2 : (index / (values.length - 1)) * chartW);
    const y = padding.top + chartH - ((value - min) / (max - min || 1)) * chartH;
    return { x, y, value };
  });

  const gradient = ctx.createLinearGradient(0, 0, 0, height);
  gradient.addColorStop(0, hexToRgba(color, 0.28));
  gradient.addColorStop(1, hexToRgba(color, 0));

  const progress = { t: 0 };
  const duration = 900;
  const start = performance.now();

  function frame(now) {
    const t = Math.min(1, (now - start) / duration);
    const eased = 1 - Math.pow(1 - t, 3);
    const visiblePoints = points.slice(0, Math.max(2, Math.ceil(points.length * eased)));

    ctx.clearRect(0, 0, width, height);

    // grade horizontal discreta
    ctx.strokeStyle = "rgba(255,255,255,0.05)";
    ctx.lineWidth = 1;
    for (let i = 1; i < 3; i++) {
      const y = padding.top + (chartH / 3) * i;
      ctx.beginPath();
      ctx.moveTo(padding.left, y);
      ctx.lineTo(width - padding.right, y);
      ctx.stroke();
    }

    if (visiblePoints.length > 1) {
      ctx.beginPath();
      ctx.moveTo(visiblePoints[0].x, visiblePoints[0].y);
      for (let i = 1; i < visiblePoints.length; i++) {
        const prev = visiblePoints[i - 1];
        const curr = visiblePoints[i];
        const midX = (prev.x + curr.x) / 2;
        ctx.quadraticCurveTo(prev.x, prev.y, midX, (prev.y + curr.y) / 2);
      }
      ctx.lineTo(visiblePoints[visiblePoints.length - 1].x, visiblePoints[visiblePoints.length - 1].y);
      ctx.strokeStyle = color;
      ctx.lineWidth = 2.4;
      ctx.lineJoin = "round";
      ctx.stroke();

      ctx.lineTo(visiblePoints[visiblePoints.length - 1].x, height - padding.bottom);
      ctx.lineTo(visiblePoints[0].x, height - padding.bottom);
      ctx.closePath();
      ctx.fillStyle = gradient;
      ctx.fill();
    }

    const last = visiblePoints[visiblePoints.length - 1];
    if (last) {
      ctx.beginPath();
      ctx.arc(last.x, last.y, 4, 0, Math.PI * 2);
      ctx.fillStyle = color;
      ctx.fill();
      ctx.beginPath();
      ctx.arc(last.x, last.y, 8, 0, Math.PI * 2);
      ctx.strokeStyle = hexToRgba(color, 0.4);
      ctx.lineWidth = 2;
      ctx.stroke();
    }

    if (t < 1) {
      requestAnimationFrame(frame);
    }
  }
  requestAnimationFrame(frame);
}

function hexToRgba(hex, alpha) {
  const value = hex.replace("#", "");
  const bigint = parseInt(value, 16);
  const r = (bigint >> 16) & 255;
  const g = (bigint >> 8) & 255;
  const b = bigint & 255;
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}
