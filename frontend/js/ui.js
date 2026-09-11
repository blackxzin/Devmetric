/**
 * DevMetrics — utilidades de interface compartilhadas entre paginas:
 * tilt 3D em cartoes, reveal-on-scroll, toasts e um helper simples de modal.
 */

const Toast = (() => {
  let stack = null;

  function ensureStack() {
    if (!stack) {
      stack = document.createElement("div");
      stack.className = "toast-stack";
      document.body.appendChild(stack);
    }
    return stack;
  }

  function show(message, type = "info", timeout = 4200) {
    const container = ensureStack();
    const el = document.createElement("div");
    el.className = `toast ${type}`;
    const icon = { success: "✅", error: "⚠️", info: "ℹ️" }[type] || "ℹ️";
    el.innerHTML = `<span>${icon}</span><span>${escapeHtml(message)}</span>`;
    container.appendChild(el);

    const remove = () => {
      el.classList.add("leaving");
      setTimeout(() => el.remove(), 260);
    };
    setTimeout(remove, timeout);
    el.addEventListener("click", remove);
  }

  return {
    success: (msg) => show(msg, "success"),
    error: (msg) => show(msg, "error"),
    info: (msg) => show(msg, "info"),
  };
})();

function escapeHtml(value) {
  const div = document.createElement("div");
  div.textContent = value == null ? "" : String(value);
  return div.innerHTML;
}

/** Ativa o tilt 3D (rotação sutil seguindo o mouse) em todo .tilt-card da página. */
function initTiltCards(root = document) {
  const cards = root.querySelectorAll(".tilt-card");
  cards.forEach((card) => {
    if (card.dataset.tiltBound) return;
    card.dataset.tiltBound = "true";

    if (!card.querySelector(".tilt-glare")) {
      const glare = document.createElement("div");
      glare.className = "tilt-glare";
      card.appendChild(glare);
    }

    const maxTilt = Number(card.dataset.tiltMax || 6);

    card.addEventListener("mousemove", (event) => {
      const rect = card.getBoundingClientRect();
      const px = (event.clientX - rect.left) / rect.width;
      const py = (event.clientY - rect.top) / rect.height;
      const rotateY = (px - 0.5) * maxTilt * 2;
      const rotateX = (0.5 - py) * maxTilt * 2;
      card.style.transform = `rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(0)`;
      card.style.setProperty("--glare-x", `${px * 100}%`);
      card.style.setProperty("--glare-y", `${py * 100}%`);
      card.dataset.tiltActive = "true";
    });

    card.addEventListener("mouseleave", () => {
      card.style.transform = "rotateX(0deg) rotateY(0deg)";
      card.dataset.tiltActive = "false";
    });
  });
}

/** Anima elementos .reveal / .reveal-stagger quando entram na viewport. */
function initRevealOnScroll(root = document) {
  const targets = root.querySelectorAll(".reveal, .reveal-stagger");
  if (!("IntersectionObserver" in window) || targets.length === 0) {
    targets.forEach((el) => el.classList.add("is-visible"));
    return;
  }
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.15 }
  );
  targets.forEach((el) => observer.observe(el));
}

/** Anima um número contando de 0 (ou do valor atual) até o valor final. */
function animateCount(el, to, options = {}) {
  const duration = options.duration || 900;
  const decimals = options.decimals || 0;
  const suffix = options.suffix || "";
  const from = Number(el.dataset.countFrom || 0);
  const start = performance.now();

  function step(now) {
    const progress = Math.min(1, (now - start) / duration);
    const eased = 1 - Math.pow(1 - progress, 3);
    const value = from + (to - from) * eased;
    el.textContent = value.toFixed(decimals) + suffix;
    if (progress < 1) {
      requestAnimationFrame(step);
    } else {
      el.dataset.countFrom = String(to);
      el.classList.add("count-up");
      setTimeout(() => el.classList.remove("count-up"), 1200);
    }
  }
  requestAnimationFrame(step);
}

const Modal = {
  open(id) {
    document.getElementById(id)?.classList.add("open");
    document.body.style.overflow = "hidden";
  },
  close(id) {
    document.getElementById(id)?.classList.remove("open");
    document.body.style.overflow = "";
  },
};

document.addEventListener("click", (event) => {
  const overlay = event.target.closest(".modal-overlay");
  if (overlay && event.target === overlay) {
    overlay.classList.remove("open");
    document.body.style.overflow = "";
  }
  const closeBtn = event.target.closest("[data-modal-close]");
  if (closeBtn) {
    closeBtn.closest(".modal-overlay")?.classList.remove("open");
    document.body.style.overflow = "";
  }
});

document.addEventListener("DOMContentLoaded", () => {
  initTiltCards();
  initRevealOnScroll();
});

function formatDate(isoDate) {
  if (!isoDate) return "";
  const date = new Date(isoDate);
  return date.toLocaleDateString("pt-BR", { day: "2-digit", month: "short" });
}

function formatDateTime(isoDate) {
  if (!isoDate) return "";
  const date = new Date(isoDate);
  return date.toLocaleString("pt-BR", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

function debounce(fn, delay = 300) {
  let timer = null;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

function initialsOf(name) {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  return parts.length === 1 ? parts[0].slice(0, 2).toUpperCase() : (parts[0][0] + parts[1][0]).toUpperCase();
}
