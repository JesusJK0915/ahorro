/* ============================================================
   Bloqueo de app: PIN (4 / 6 / personalizado), patrón (3x3 a 9x9)
   y huella del dispositivo. Todo se guarda en state.lock.
   El hash es disuasorio (evita acceso casual), no criptografía.
   ============================================================ */

const LOCK_METHODS = ["none", "pin", "pattern", "biometric"];
let lockUnlocked = false;
let lockFails = 0;
let lockBlockedUntil = 0;
let lockView = "unlock";

const lockCfg = () => state.lock || (state.lock = { method: "none" });

function lockRand(n) {
  const a = new Uint32Array(n);
  if (window.crypto?.getRandomValues) window.crypto.getRandomValues(a);
  else for (let i = 0; i < n; i++) a[i] = Math.floor(Math.random() * 0xffffffff);
  return Array.from(a, (x) => x.toString(36)).join("").slice(0, n);
}

function lockHash(value, salt) {
  const s = String(salt) + "|" + String(value);
  let h1 = 0x811c9dc5, h2 = 0x9e3779b9;
  for (let i = 0; i < s.length; i++) {
    const c = s.charCodeAt(i);
    h1 = Math.imul(h1 ^ c, 0x01000193) >>> 0;
    h2 = Math.imul(h2 + c + i * 131, 0x85ebca6b) >>> 0;
    h2 = ((h2 << 13) | (h2 >>> 19)) >>> 0;
  }
  return h1.toString(36) + "-" + h2.toString(36);
}

function lockRecoveryCode() {
  const abc = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 6; i++) s += abc[Math.floor(Math.random() * abc.length)];
  return s.slice(0, 3) + "-" + s.slice(3);
}

/* ---------------- teclado numérico ---------------- */
function buildPinPad(host, len, onComplete) {
  host.innerHTML = `
    <div class="pin-dots">${Array.from({ length: len }, () => "<i></i>").join("")}</div>
    <div class="pin-keys">
      ${[1, 2, 3, 4, 5, 6, 7, 8, 9].map((n) => `<button type="button" class="pin-key" data-k="${n}">${n}</button>`).join("")}
      <button type="button" class="pin-key blank" aria-hidden="true" tabindex="-1"></button>
      <button type="button" class="pin-key" data-k="0">0</button>
      <button type="button" class="pin-key del" data-k="del" aria-label="Borrar">⌫</button>
    </div>`;
  const dots = [...host.querySelectorAll(".pin-dots i")];
  let val = "";
  host.querySelectorAll(".pin-key[data-k]").forEach((b) =>
    b.addEventListener("click", () => {
      const k = b.dataset.k;
      if (k === "del") val = val.slice(0, -1);
      else if (val.length < len) val += k;
      dots.forEach((d, i) => d.classList.toggle("on", i < val.length));
      if (val.length === len) onComplete(val);
    })
  );
  return { clear() { val = ""; dots.forEach((d) => d.classList.remove("on")); } };
}

/* ---------------- patrón 3x3 ... 9x9 ---------------- */
function buildPattern(host, n, onComplete) {
  host.innerHTML = `
    <div class="pat-wrap">
      <svg class="pat-lines" aria-hidden="true"></svg>
      <div class="pat-grid" style="--n:${n}">
        ${Array.from({ length: n * n }, (_, i) => `<span class="pat-dot" data-i="${i}"></span>`).join("")}
      </div>
    </div>`;
  const wrap = host.querySelector(".pat-wrap");
  const grid = host.querySelector(".pat-grid");
  const svg = host.querySelector(".pat-lines");
  const dots = [...host.querySelectorAll(".pat-dot")];
  let seq = [], drawing = false, rect = null;

  const measure = () => (rect = grid.getBoundingClientRect());
  const center = (i) => {
    const r = rect || measure();
    const cell = r.width / n;
    return { x: (i % n) * cell + cell / 2, y: Math.floor(i / n) * cell + cell / 2 };
  };
  const nearest = (x, y) => {
    const r = rect || measure();
    const cell = r.width / n;
    let best = -1, bd = Infinity;
    for (let i = 0; i < n * n; i++) {
      const p = center(i);
      const d = Math.hypot(p.x - (x - r.left), p.y - (y - r.top));
      if (d < bd) { bd = d; best = i; }
    }
    return bd <= cell * 0.62 ? best : -1;
  };
  const paint = (pt) => {
    const r = rect || measure();
    if (!r.width) return;
    svg.setAttribute("viewBox", `0 0 ${r.width} ${r.height}`);
    let d = "";
    seq.forEach((i, k) => { const p = center(i); d += (k ? " L " : "M ") + p.x + " " + p.y; });
    if (drawing && pt && seq.length) {
      const p = center(seq[seq.length - 1]);
      d += " L " + (pt.x - r.left) + " " + (pt.y - r.top);
    }
    svg.innerHTML = d ? `<path d="${d}"/>` : "";
    dots.forEach((el, i) => el.classList.toggle("on", seq.indexOf(i) >= 0));
  };
  const add = (x, y) => {
    const i = nearest(x, y);
    if (i >= 0 && seq[seq.length - 1] !== i) { seq.push(i); paint({ x, y }); }
  };

  wrap.addEventListener("pointerdown", (e) => {
    e.preventDefault();
    if (Date.now() < lockBlockedUntil) return;
    drawing = true;
    seq = [];
    measure();
    try { wrap.setPointerCapture(e.pointerId); } catch (_) {}
    add(e.clientX, e.clientY);
  });
  wrap.addEventListener("pointermove", (e) => {
    if (!drawing) return;
    e.preventDefault();
    add(e.clientX, e.clientY);
    paint({ x: e.clientX, y: e.clientY });
  });
  const end = () => {
    if (!drawing) return;
    drawing = false;
    paint();
    onComplete(seq.slice());
  };
  wrap.addEventListener("pointerup", end);
  wrap.addEventListener("pointercancel", end);
  return { clear() { seq = []; paint(); } };
}

/* ---------------- huella ---------------- */
function bioAvailable() {
  try { return !!(window.AndroidAuth && window.AndroidAuth.isAvailable()); }
  catch (_) { return false; }
}
function bioAsk() {
  if (!bioAvailable()) return false;
  try { window.AndroidAuth.authenticate(); return true; }
  catch (_) { return false; }
}
window.onBiometricResult = (ok) => {
  if (ok) unlockApp();
  else lockFail();
};

/* ---------------- configuración desde Ajustes ---------------- */
function finishLock(patch, announce) {
  const code = lockRecoveryCode();
  state.lock = Object.assign({ method: "none" }, patch, { recovery: code });
  save();
  lockUnlocked = true;
  renderSettings();
  if (announce) {
    openSheet("Bloqueo activado", "Guarda este código de rescate: sirve para entrar si olvidas tu clave.", `
      <div class="recovery-code">${code}</div>
      <p class="lock-note">Anótalo o compártelo con alguien de confianza. No se puede recuperar de otra forma.</p>
      <button class="btn-filled press" data-close>Entendido</button>`, () => {});
  }
  buzz(25);
}

function chooseLockMethod(m) {
  if (!LOCK_METHODS.includes(m)) return;
  if (m === "none") {
    state.lock = { method: "none" };
    lockUnlocked = true;
    save();
    renderSettings();
    buzz();
    return;
  }
  if (m === "biometric") {
    if (!bioAvailable()) {
      renderSettings();
      openSheet("Huella no disponible", "Tu celular no tiene una huella registrada o el sensor no responde. Puedes usar PIN o patrón en su lugar.", "", () => {});
      return;
    }
    finishLock({ method: "biometric" }, true);
    return;
  }
  if (m === "pin") setupPin();
  else setupPattern();
}

function setupPin() {
  const salt = lockRand(8);
  openSheet("Crear PIN", "Elige la longitud y escríbelo dos veces para confirmar.", `
    <div class="lock-opts">
      ${[4, 6].map((n) => `<button type="button" class="chip-btn ${n === 4 ? "sel" : ""}" data-len="${n}">${n} dígitos</button>`).join("")}
      <button type="button" class="chip-btn" data-len="custom">Otro</button>
    </div>
    <div class="lock-custom" id="pinCustomWrap" hidden>
      <span>Dígitos (7 a 12)</span>
      <input id="pinCustom" type="number" min="7" max="12" value="8" inputmode="numeric" />
    </div>
    <div class="lock-step" id="pinStep">Paso 1 de 2 · escribe tu PIN</div>
    <div class="pin-host" id="pinHost"></div>
    <button class="text-btn" data-close>Cancelar</button>
  `, (body, close) => {
    let len = 4, first = null;
    const step = body.querySelector("#pinStep");
    const host = body.querySelector("#pinHost");
    const customWrap = body.querySelector("#pinCustomWrap");
    let pad;
    const onPin = (v) => {
      if (first === null) {
        first = v;
        step.textContent = "Paso 2 de 2 · repite tu PIN";
        pad.clear();
      } else if (v === first) {
        close();
        finishLock({ method: "pin", pinLen: len, salt, hash: lockHash(v, salt) }, true);
      } else {
        first = null;
        step.textContent = "No coinciden · escribe el PIN otra vez";
        pad.clear();
        buzz(30);
      }
    };
    pad = buildPinPad(host, len, onPin);
    body.querySelectorAll("[data-len]").forEach((b) => b.addEventListener("click", () => {
      body.querySelectorAll("[data-len]").forEach((x) => x.classList.remove("sel"));
      b.classList.add("sel");
      const v = b.dataset.len;
      customWrap.hidden = v !== "custom";
      len = v === "custom"
        ? clamp(Number(body.querySelector("#pinCustom").value) || 8, 7, 12)
        : Number(v);
      first = null;
      step.textContent = "Paso 1 de 2 · escribe tu PIN";
      pad = buildPinPad(host, len, onPin);
    }));
    body.querySelector("#pinCustom").addEventListener("change", (e) => {
      if (len !== 8 || body.querySelector('[data-len="custom"]').classList.contains("sel"))
        len = clamp(Number(e.target.value) || 8, 7, 12);
    });
  });
}

function setupPattern() {
  const salt = lockRand(8);
  openSheet("Crear patrón", "Elige el tamaño de la cuadrícula (hasta 9×9) y dibújalo dos veces.", `
    <div class="lock-opts">
      ${[3, 4, 5, 6, 7, 8, 9].map((n) => `<button type="button" class="chip-btn ${n === 3 ? "sel" : ""}" data-grid="${n}">${n}×${n}</button>`).join("")}
    </div>
    <div class="lock-step" id="patStep">Paso 1 de 2 · dibuja tu patrón</div>
    <div class="pat-host" id="patHost"></div>
    <button class="text-btn" data-close>Cancelar</button>
  `, (body, close) => {
    let n = 3, first = null;
    const step = body.querySelector("#patStep");
    const host = body.querySelector("#patHost");
    const make = () => buildPattern(host, n, (seq) => {
      if (seq.length < 4) {
        step.textContent = "Mínimo 4 puntos · inténtalo otra vez";
        buzz(30);
        return;
      }
      const code = seq.join(",");
      if (first === null) {
        first = code;
        step.textContent = "Paso 2 de 2 · repite tu patrón";
        make();
      } else if (code === first) {
        close();
        finishLock({ method: "pattern", grid: n, salt, hash: lockHash(code, salt) }, true);
      } else {
        first = null;
        step.textContent = "No coinciden · dibújalo otra vez";
        make();
        buzz(30);
      }
    });
    make();
    body.querySelectorAll("[data-grid]").forEach((b) => b.addEventListener("click", () => {
      body.querySelectorAll("[data-grid]").forEach((x) => x.classList.remove("sel"));
      b.classList.add("sel");
      n = Number(b.dataset.grid);
      first = null;
      step.textContent = "Paso 1 de 2 · dibuja tu patrón";
      make();
    }));
  });
}

function openLockManager() {
  const cfg = lockCfg();
  if (cfg.method === "none") { chooseLockMethod("pin"); return; }
  const detail = cfg.method === "pin" ? `PIN de ${cfg.pinLen} dígitos`
    : cfg.method === "pattern" ? `Patrón ${cfg.grid}×${cfg.grid}`
    : "Huella del dispositivo";
  openSheet("Seguridad", `Método actual: ${detail}.`, `
    <div class="lock-detail">${detail}</div>
    <button class="btn-filled press" id="lockReconfig">Cambiar método</button>
    <div class="recovery-box">
      <span>Código de rescate</span>
      <strong>${cfg.recovery || "—"}</strong>
      <small>Guárdalo para entrar si olvidas tu clave.</small>
    </div>
    <button class="text-btn" id="lockOff">Desactivar bloqueo</button>
  `, (body, close) => {
    body.querySelector("#lockReconfig").addEventListener("click", () => {
      close();
      if (cfg.method === "pin") setupPin();
      else if (cfg.method === "pattern") setupPattern();
      else chooseLockMethod("biometric");
    });
    body.querySelector("#lockOff").addEventListener("click", () => { close(); chooseLockMethod("none"); });
  });
}

/* ---------------- pantalla de bloqueo ---------------- */
function lockFail() {
  lockFails++;
  const msg = $("#lockMsg");
  if (lockFails >= 5) {
    lockBlockedUntil = Date.now() + 30000;
    lockFails = 0;
    if (msg) msg.textContent = "Demasiados intentos. Espera 30 segundos.";
    setTimeout(() => { lockBlockedUntil = 0; if (!lockUnlocked) showLock(); }, 30000);
  } else if (msg) {
    msg.textContent = `Incorrecto. Te quedan ${5 - lockFails} intentos.`;
  }
  buzz(40);
}

function verifySecret(v) {
  const cfg = lockCfg();
  if (lockHash(v, cfg.salt) === cfg.hash) unlockApp();
  else lockFail();
}

function unlockApp() {
  lockUnlocked = true;
  lockFails = 0;
  const el = $("#lock");
  if (el) { el.hidden = true; el.innerHTML = ""; }
  buzz(20);
}

function showLock(mode) {
  lockView = mode || "unlock";
  const cfg = lockCfg();
  const el = $("#lock");
  if (!el) return;
  if (cfg.method === "none") { el.hidden = true; return; }
  el.hidden = false;

  const detail = cfg.method === "pin" ? `Ingresa tu PIN de ${cfg.pinLen} dígitos`
    : cfg.method === "pattern" ? `Dibuja tu patrón de ${cfg.grid}×${cfg.grid}`
    : "Confirma tu huella para continuar";
  const remaining = Math.max(0, lockBlockedUntil - Date.now());

  el.innerHTML = `
    <div class="lock-card">
      <div class="lock-badge" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/><path d="M12 15v2"/>
        </svg>
      </div>
      <h2>Ahorros está protegido</h2>
      <p class="lock-sub">${lockView === "recovery" ? "Escribe tu código de rescate" : detail}</p>
      ${lockView === "recovery" ? `
        <input id="recInput" class="lock-input" autocomplete="off" placeholder="ABC-123" />
        <button class="btn-filled press" id="recOk">Entrar</button>
        <button class="text-btn" id="recBack">Volver</button>
      ` : `
        <div class="lock-input-host" id="lockInputHost"></div>
        ${bioAvailable() ? `<button class="bio-btn" id="bioBtn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 4a8 8 0 0 0-8 8M12 20a8 8 0 0 0 8-8"/><path d="M12 8a4 4 0 0 0-4 4M12 16a4 4 0 0 0 4-4"/><path d="M12 12h.01"/></svg>
          <span>Usar huella</span>
        </button>` : ""}
        <p class="lock-msg" id="lockMsg">${remaining > 0 ? `Espera ${Math.ceil(remaining / 1000)} s` : ""}</p>
        <button class="text-btn" id="lockForgot">¿Olvidaste tu código?</button>
      `}
    </div>`;

  if (lockView === "recovery") {
    const inp = el.querySelector("#recInput");
    const ok = () => {
      if ((inp.value || "").trim().toUpperCase() === String(cfg.recovery || "").toUpperCase()) unlockApp();
      else { lockFail(); inp.value = ""; }
    };
    el.querySelector("#recOk").addEventListener("click", ok);
    inp.addEventListener("keydown", (e) => { if (e.key === "Enter") ok(); });
    el.querySelector("#recBack").addEventListener("click", () => showLock("unlock"));
    setTimeout(() => inp.focus({ preventScroll: true }), 160);
    return;
  }

  const host = el.querySelector("#lockInputHost");
  if (remaining <= 0 && cfg.method === "pin") buildPinPad(host, cfg.pinLen, verifySecret);
  if (remaining <= 0 && cfg.method === "pattern") {
    buildPattern(host, cfg.grid, (seq) => {
      if (seq.length < 4) { const m = $("#lockMsg"); if (m) m.textContent = "Mínimo 4 puntos"; return; }
      verifySecret(seq.join(","));
    });
  }
  if (remaining > 0) setTimeout(() => { if (!lockUnlocked) showLock(); }, remaining + 100);

  el.querySelector("#bioBtn")?.addEventListener("click", bioAsk);
  el.querySelector("#lockForgot").addEventListener("click", () => showLock("recovery"));
}

function initLock() {
  const cfg = lockCfg();
  if (!cfg.method || cfg.method === "none") { lockUnlocked = true; return; }
  lockUnlocked = false;
  showLock();

  document.addEventListener("visibilitychange", () => {
    if (lockCfg().method === "none") return;
    if (document.hidden) lockUnlocked = false;
    else if (!lockUnlocked) showLock();
  });
}

initLock();
