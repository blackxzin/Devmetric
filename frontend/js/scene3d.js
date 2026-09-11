/**
 * DevMetrics — fundo animado "constelação 3D".
 *
 * Canvas 2D com particulas em um cubo 3D projetadas em perspectiva (sem WebGL):
 * cada ponto tem x, y, z reais; a projecao divide por z para dar profundidade,
 * e o conjunto gira lentamente. E leve o suficiente pra correr em qualquer maquina.
 */
(function initScene3D() {
  const mount = document.querySelector(".scene-bg");
  if (!mount) return;

  const canvas = document.createElement("canvas");
  mount.appendChild(canvas);
  const ctx = canvas.getContext("2d");

  const prefersReducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  let width = 0;
  let height = 0;
  let dpr = Math.min(window.devicePixelRatio || 1, 2);
  let points = [];
  let angleY = 0;
  let angleX = 0;
  let mouseX = 0;
  let mouseY = 0;
  let targetMouseX = 0;
  let targetMouseY = 0;
  let rafId = null;

  const POINT_COUNT = 70;
  const RADIUS = 620;
  const CONNECT_DIST = 150;
  const FOCAL = 640;

  function resize() {
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    canvas.style.width = width + "px";
    canvas.style.height = height + "px";
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  function seedPoints() {
    points = [];
    for (let i = 0; i < POINT_COUNT; i++) {
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(Math.random() * 2 - 1);
      const r = RADIUS * (0.35 + Math.random() * 0.65);
      points.push({
        baseX: r * Math.sin(phi) * Math.cos(theta),
        baseY: r * Math.sin(phi) * Math.sin(theta),
        baseZ: r * Math.cos(phi),
        twinkleSeed: Math.random() * Math.PI * 2,
      });
    }
  }

  function project(point) {
    // Rotacao Y
    const cosY = Math.cos(angleY);
    const sinY = Math.sin(angleY);
    let x = point.baseX * cosY - point.baseZ * sinY;
    let z = point.baseX * sinY + point.baseZ * cosY;
    let y = point.baseY;

    // Rotacao X (leve, reage ao mouse)
    const cosX = Math.cos(angleX);
    const sinX = Math.sin(angleX);
    const y2 = y * cosX - z * sinX;
    const z2 = y * sinX + z * cosX;
    y = y2;
    z = z2;

    const depth = FOCAL + z;
    if (depth <= 1) return null;
    const scale = FOCAL / depth;
    return {
      x: width / 2 + x * scale,
      y: height / 2 + y * scale,
      scale,
      z,
    };
  }

  function draw(time) {
    ctx.clearRect(0, 0, width, height);

    const projected = points.map((point) => ({ point, proj: project(point) }))
      .filter((entry) => entry.proj !== null);

    // Conexoes entre pontos proximos (efeito constelacao)
    ctx.lineWidth = 1;
    for (let i = 0; i < projected.length; i++) {
      for (let j = i + 1; j < projected.length; j++) {
        const a = projected[i].proj;
        const b = projected[j].proj;
        const dx = a.x - b.x;
        const dy = a.y - b.y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < CONNECT_DIST) {
          const opacity = (1 - dist / CONNECT_DIST) * 0.16 * Math.min(a.scale, b.scale);
          ctx.strokeStyle = `rgba(124, 92, 255, ${opacity})`;
          ctx.beginPath();
          ctx.moveTo(a.x, a.y);
          ctx.lineTo(b.x, b.y);
          ctx.stroke();
        }
      }
    }

    // Pontos
    for (const { point, proj } of projected) {
      const twinkle = 0.55 + 0.45 * Math.sin(time / 900 + point.twinkleSeed);
      const size = Math.max(0.6, proj.scale * 1.8) * twinkle;
      const opacity = Math.min(1, proj.scale) * 0.85;
      const hue = proj.z > 0 ? "51, 214, 255" : "124, 92, 255";
      ctx.beginPath();
      ctx.fillStyle = `rgba(${hue}, ${opacity})`;
      ctx.arc(proj.x, proj.y, size, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  function tick(time) {
    angleY += 0.00035;
    mouseX += (targetMouseX - mouseX) * 0.04;
    mouseY += (targetMouseY - mouseY) * 0.04;
    angleX = mouseY * 0.25;
    draw(time);
    rafId = requestAnimationFrame(tick);
  }

  function onMouseMove(event) {
    targetMouseX = (event.clientX / width - 0.5) * 2;
    targetMouseY = (event.clientY / height - 0.5) * 2;
  }

  function start() {
    resize();
    seedPoints();
    if (prefersReducedMotion) {
      draw(0);
      return;
    }
    window.addEventListener("mousemove", onMouseMove, { passive: true });
    rafId = requestAnimationFrame(tick);
  }

  window.addEventListener("resize", () => {
    resize();
  });

  document.addEventListener("visibilitychange", () => {
    if (document.hidden && rafId) {
      cancelAnimationFrame(rafId);
      rafId = null;
    } else if (!document.hidden && !rafId && !prefersReducedMotion) {
      rafId = requestAnimationFrame(tick);
    }
  });

  start();
})();
