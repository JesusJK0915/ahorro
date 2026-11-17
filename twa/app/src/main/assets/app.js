/* =========================================================
   Ahorros · PWA sin dependencias
   Material 3 Expressive: movimiento con spring, formas suaves
   ========================================================= */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

const STORE = "ahorros.v1";
let _nf = null, _nfCur = null;
function getNf() {
  const c = (state && state.currency) || "COP";
  if (_nf && _nfCur === c) return _nf;
  _nfCur = c;
  _nf = new Intl.NumberFormat("es-CO", { style: "currency", currency: c, maximumFractionDigits: 0 });
  return _nf;
}
const money = (n) => getNf().format(Math.round(Number(n) || 0));
const group = (n) => Math.round(Number(n) || 0).toLocaleString("es-CO");
const digits = (v) => String(v ?? "").replace(/\D/g, "");
const clamp = (n, a, b) => Math.min(b, Math.max(a, n));
const uid = () => Math.random().toString(36).slice(2, 9);
const reduceMotion = matchMedia("(prefers-reduced-motion: reduce)");
const TINTS = ["#00696b", "#8a5a00", "#7a4e9c", "#a03a2e", "#00658e"];
// Versionado: cuando el parche llega a 9, sube el número del medio y reinicia en 0.
// Ej: 1.0.9 -> 1.1.0 -> 1.1.1 ... -> 1.1.9 -> 1.2.0
const APP_VERSION = "1.1.1";
// Cambia esta URL por la de tu version.json publicado (GitHub Pages, etc.)
const UPDATE_URL = "https://JesusJK0915.github.io/ahorro/version.json";
const pctOf = (g) => (g.target > 0 ? clamp((g.saved / g.target) * 100, 0, 100) : 0);
const buzz = (ms = 12) => { try { navigator.vibrate?.(ms); } catch {} };

/* ------------------------- iconos ------------------------- */
const SVG = {
  target: '<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="3.5"/>',
  check: '<path d="M5 12.5l4.5 4.5L19 7.5"/>',
  trash: '<path d="M4 7h16"/><path d="M9.5 7V5.2A1.2 1.2 0 0 1 10.7 4h2.6a1.2 1.2 0 0 1 1.2 1.2V7"/><path d="M6.5 7l.8 11.2A2 2 0 0 0 9.3 20h5.4a2 2 0 0 0 2-1.8L17.5 7"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  up: '<path d="M12 19V5"/><path d="M6 11l6-6 6 6"/>',
  down: '<path d="M12 5v14"/><path d="M6 13l6 6 6-6"/>',
  coin: '<ellipse cx="12" cy="7" rx="7" ry="3"/><path d="M5 7v5c0 1.7 3.1 3 7 3s7-1.3 7-3V7"/><path d="M5 12v5c0 1.7 3.1 3 7 3s7-1.3 7-3v-5"/>',
};
const icon = (name, w = 20) =>
  `<svg viewBox="0 0 24 24" width="${w}" height="${w}" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${SVG[name] || ""}</svg>`;

/* ------------------------- estado ------------------------- */
function defaults() {
  const ts = Date.now();
  return {
    goals: [],
    moves: [],
    theme: "dark",
    ringMode: false,
    dynamicColor: true,
    currency: "COP",
    themeMode: "dark",
  };
}

let state = load();

function load() {
  try {
    const raw = localStorage.getItem(STORE);
    if (!raw) return defaults();
    const p = JSON.parse(raw);
    if (!p || !Array.isArray(p.goals)) return defaults();
    p.moves = Array.isArray(p.moves) ? p.moves : [];
    p.goals = p.goals.filter((g) => g && typeof g.name === "string" && Number(g.target) > 0);
    if (typeof p.ringMode !== "boolean") p.ringMode = false;
    if (typeof p.dynamicColor !== "boolean") p.dynamicColor = true;
    if (!p.currency) p.currency = "COP";
    p.themeMode = "dark";
    return p;
  } catch {
    return defaults();
  }
}
function save() {
  try { localStorage.setItem(STORE, JSON.stringify(state)); } catch {}
}

/* ------------------------- tema (siempre oscuro) ------------------------- */
function applyTheme(t) {
  document.documentElement.dataset.theme = "dark";
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.content = "#0d1313";
}
function applyThemeMode(mode) {
  state.themeMode = "dark";
  state.theme = "dark";
  save();
  applyTheme("dark");
}
function applyDynamic(on) {
  document.documentElement.dataset.dynamic = on ? "true" : "false";
}
function toggleDynamic() {
  state.dynamicColor = !state.dynamicColor;
  save();
  buzz();
  applyDynamic(state.dynamicColor);
  const dt = $("#dynamicToggle");
  if (dt) dt.setAttribute("aria-pressed", state.dynamicColor ? "true" : "false");
}

/* ------------------------- animaciones ------------------------- */
function tween(el, to) {
  const from = Number(el.dataset.v || 0);
  el.dataset.v = to;
  if (from === to) { el.textContent = money(to); return; }
  cancelAnimationFrame(el._raf || 0);
  if (reduceMotion.matches) { el.textContent = money(to); return; }
  const t0 = performance.now();
  const dur = 760;
  const ease = (k) => 1 - Math.pow(1 - k, 3);
  const step = (now) => {
    const k = clamp((now - t0) / dur, 0, 1);
    el.textContent = money(from + (to - from) * ease(k));
    if (k < 1) el._raf = requestAnimationFrame(step);
    else el.textContent = money(to);
  };
  el._raf = requestAnimationFrame(step);
}

function bump(el, cls = "bump") {
  if (reduceMotion.matches) return;
  el.classList.remove(cls);
  void el.offsetWidth;
  el.classList.add(cls);
}

function burst(x, y) {
  if (reduceMotion.matches) return;
  const fx = $("#fx");
  const colors = ["#00696b", "#7fd4d6", "#ffb59d", "#e8b931", "#9d8df1"];
  for (let i = 0; i < 18; i++) {
    const p = document.createElement("i");
    p.className = "fx-piece";
    const ang = Math.random() * Math.PI * 2;
    const dist = 40 + Math.random() * 95;
    p.style.left = x + "px";
    p.style.top = y + "px";
    p.style.background = colors[i % colors.length];
    p.style.setProperty("--dx", Math.cos(ang) * dist + "px");
    p.style.setProperty("--dy", Math.sin(ang) * dist - 20 + "px");
    p.style.setProperty("--rot", Math.random() * 540 - 270 + "deg");
    if (i % 3 === 0) p.style.borderRadius = "50%";
    fx.appendChild(p);
    p.addEventListener("animationend", () => p.remove());
  }
}

function floatAmount(x, y, text) {
  if (reduceMotion.matches) return;
  const el = document.createElement("span");
  el.className = "fx-float";
  el.textContent = text;
  el.style.left = x + "px";
  el.style.top = y + "px";
  $("#fx").appendChild(el);
  el.addEventListener("animationend", () => el.remove());
}

/* ------------------------- ripple ------------------------- */
document.addEventListener("pointerdown", (e) => {
  const host = e.target.closest?.(".press");
  if (!host) return;
  const r = host.getBoundingClientRect();
  const size = Math.max(r.width, r.height) || 40;
  const s = document.createElement("span");
  s.className = "ripple";
  s.style.width = s.style.height = size + "px";
  s.style.left = e.clientX - r.left - size / 2 + "px";
  s.style.top = e.clientY - r.top - size / 2 + "px";
  host.appendChild(s);
  s.addEventListener("animationend", () => s.remove());
}, { passive: true });

/* ------------------------- render ------------------------- */
function render() {
  renderHero();
  renderGoals();
  renderStats();
  renderHistory();
  renderSettings();
}

function renderHero() {
  const total = state.goals.reduce((s, g) => s + g.saved, 0);
  const target = state.goals.reduce((s, g) => s + g.target, 0);
  const p = target > 0 ? clamp((total / target) * 100, 0, 100) : 0;

  tween($("#totalAmount"), total);
  $("#heroPct").textContent = Math.round(p) + "%";
  const prog = $("#heroProg");
  const ring = $("#heroRing");
  if (state.ringMode) { prog.hidden = true; ring.hidden = false; } else { prog.hidden = false; ring.hidden = true; }
  prog.style.setProperty("--p", p);
  prog.dataset.on = p > 0 ? "true" : "false";
  ring.style.setProperty("--p", p);
  ring.dataset.on = p > 0 ? "true" : "false";
  ring.dataset.targetP = p;
  $("#heroRingPct").textContent = Math.round(p) + "%";
  const n = state.goals.length;
  $("#heroGoals").textContent = `${n} ${n === 1 ? "meta" : "metas"}`;
  $("#heroTarget").textContent = `de ${money(target)}`;
}

/* ------------------------- estadísticas ------------------------- */
let statsReady = false;
const monthKey = (ts) => {
  const d = new Date(ts);
  return d.getFullYear() + "-" + d.getMonth();
};

function renderStats() {
  const body = $("#statsBody");
  const loading = $("#statsLoading");
  if (!body || !loading) return;
  if (!statsReady) {
    // Primera carga: mostrar el "gusanito" de Material 3.
    loading.hidden = false;
    body.hidden = true;
    setTimeout(() => {
      statsReady = true;
      loading.hidden = true;
      body.hidden = false;
      paintStats();
    }, 720);
  } else {
    loading.hidden = true;
    body.hidden = false;
    paintStats();
  }
}

function paintStats() {
  const key = monthKey(Date.now());
  const sum = (arr) => arr.reduce((s, m) => s + (Number(m.amount) || 0), 0);
  const inMes = state.moves.filter((m) => m.type !== "withdraw" && monthKey(m.ts) === key);
  const outMes = state.moves.filter((m) => m.type === "withdraw" && monthKey(m.ts) === key);

  tween($("#statIn"), sum(inMes));
  tween($("#statOut"), sum(outMes));
  $("#statCountIn").textContent = inMes.length;
  $("#statCountOut").textContent = outMes.length;

  const days = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() - i);
    days.push(d);
  }
  const data = days.map((d) => {
    const s = d.toDateString();
    const inc = sum(state.moves.filter((m) => m.type !== "withdraw" && new Date(m.ts).toDateString() === s));
    const out = sum(state.moves.filter((m) => m.type === "withdraw" && new Date(m.ts).toDateString() === s));
    return { d, net: inc - out };
  });
  const max = Math.max(1, ...data.map((x) => Math.abs(x.net)));
  $("#chart").innerHTML = data.map((x, i) => {
    const h = Math.max(5, Math.round((Math.abs(x.net) / max) * 100));
    const lbl = x.d.toLocaleDateString("es-CO", { weekday: "short" }).replace(".", "");
    return `<div class="chart-bar ${x.net < 0 ? "out" : ""}" style="--i:${i}" title="${money(x.net)}">
      <i style="height:${h}%"></i><small>${lbl}</small>
    </div>`;
  }).join("");
  $("#chartTotal").textContent = money(data.reduce((s, x) => s + x.net, 0));
}

function renderGoals() {
  const list = $("#goalList");
  const ids = new Set(state.goals.map((g) => g.id));
  [...list.children].forEach((el) => { if (!ids.has(el.dataset.id)) el.remove(); });

  state.goals.forEach((g, i) => {
    let el = list.querySelector(`[data-id="${g.id}"]`);
    if (!el) {
      el = buildGoal(g, i);
      list.appendChild(el);
    }
    updateGoal(el, g);
    list.appendChild(el); // mantiene el orden
  });

  $("#emptyGoals").hidden = state.goals.length > 0;
  $("#goalList").hidden = state.goals.length === 0;
}

function buildGoal(g, i) {
  const li = document.createElement("li");
  li.className = "goal";
  li.dataset.id = g.id;
  li.style.setProperty("--i", Math.min(i, 6));
  li.innerHTML = `
    <div class="goal-head">
      <span class="goal-icon">${icon("target")}</span>
      <div class="goal-titles">
        <h3></h3>
        <p class="goal-sub"><b class="saved"></b> <span class="of">de</span> <span class="tgt"></span></p>
      </div>
      <span class="goal-pct">0%</span>
    </div>
    <div class="prog" style="--p:0">
      <span class="prog-track"></span>
      <span class="prog-fill"></span>
      <span class="prog-stop"></span>
    </div>
    <div class="ring ring-sm" style="--p:0" hidden aria-hidden="true">
      <svg viewBox="0 0 48 48">
        <circle class="ring-track" cx="24" cy="24" r="20" />
        <path class="ring-track" d="" />
        <path class="ring-fill" d="" />
      </svg>
    </div>
    <div class="goal-foot">
      <span class="rest"></span>
      <span class="goal-actions">
        <button class="mini-btn mini-btn-out press" data-withdraw aria-label="Retirar dinero">${icon("down", 15)}</button>
        <button class="mini-btn press" data-add>${icon("plus", 15)}<span style="vertical-align:middle;margin-left:4px">Añadir</span></button>
      </span>
    </div>`;
  li.addEventListener("click", (e) => {
    if (e.target.closest("[data-add]") || e.target.closest("[data-withdraw]")) return;
    openDeposit(g.id);
  });
  li.querySelector("[data-add]").addEventListener("click", (e) => {
    e.stopPropagation();
    openDeposit(g.id);
  });
  li.querySelector("[data-withdraw]").addEventListener("click", (e) => {
    e.stopPropagation();
    openWithdraw(g.id);
  });
  let timer = 0;
  li.addEventListener("pointerdown", () => {
    timer = setTimeout(() => { buzz(18); openGoalOptions(g.id); }, 550);
  });
  ["pointerup", "pointerleave", "pointercancel", "pointermove"].forEach((ev) =>
    li.addEventListener(ev, () => clearTimeout(timer))
  );
  li.addEventListener("contextmenu", (e) => { e.preventDefault(); openGoalOptions(g.id); });
  return li;
}

function updateGoal(el, g) {
  const p = pctOf(g);
  const done = p >= 100;
  el.style.setProperty("--tint-raw", g.tint || TINTS[0]);
  el.classList.toggle("is-done", done);
  el.querySelector(".goal-titles h3").textContent = g.name;
  el.querySelector(".saved").textContent = money(g.saved);
  el.querySelector(".tgt").textContent = money(g.target);
  el.querySelector(".goal-pct").textContent = Math.round(p) + "%";
  const iconEl = el.querySelector(".goal-icon");
  iconEl.innerHTML = icon(done ? "check" : "target");
  const prog = el.querySelector(".prog");
  const ring = el.querySelector(".ring");
  if (state.ringMode) { prog.hidden = true; ring.hidden = false; } else { prog.hidden = false; ring.hidden = true; }
  prog.style.setProperty("--p", p);
  prog.dataset.on = p > 0 ? "true" : "false";
  ring.style.setProperty("--p", p);
  ring.dataset.on = p > 0 ? "true" : "false";
  ring.dataset.targetP = p;
  el.querySelector(".rest").textContent = done
    ? "¡Meta cumplida!"
    : `Faltan ${money(g.target - g.saved)}`;
}

function renderHistory() {
  const list = $("#historyList");
  const moves = state.moves.slice(0, 60);
  $("#historyCount").textContent = moves.length ? `${state.moves.length} mov.` : "";
  list.innerHTML = moves.map((m, i) => {
    const out = m.type === "withdraw";
    return `
    <li class="move ${out ? "move-out" : ""}" style="--i:${Math.min(i, 8)};--tint-raw:${m.tint || TINTS[0]}">
      <span class="move-dot">${icon(out ? "down" : "up", 18)}</span>
      <div class="move-body">
        <strong>${escapeHTML(m.name || (out ? "Retiro" : "Aporte"))}</strong>
        <span>${fmtDate(m.ts)}</span>
      </div>
      <span class="move-amt">${out ? "-" : "+"}${money(m.amount)}</span>
    </li>`;
  }).join("");
  $("#emptyHistory").hidden = state.moves.length > 0;
  list.hidden = state.moves.length === 0;
}

/* ------------------------- actualizaciones ------------------------- */
function openUpdatesSheet() {
  openSheet("Actualizaciones", `Versión instalada: ${APP_VERSION}`, `
    <div class="update-status" id="updStatus">
      <div class="empty-art" style="width:64px;height:64px;margin:0 auto 10px">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
          <polyline points="7 10 12 15 17 10"/>
          <line x1="12" y1="15" x2="12" y2="3"/>
        </svg>
      </div>
      <p class="muted" style="text-align:center">Pulsa para buscar nuevas versiones.</p>
    </div>
    <div class="sheet-actions">
      <button class="btn-tonal press" data-close>Cancelar</button>
      <button class="btn-filled press" id="checkUpdates">Buscar actualizaciones</button>
    </div>`, (root, close) => {
    const status = $("#updStatus", root);
    const btn = $("#checkUpdates", root);
    const setStatus = (html) => { status.innerHTML = html; };
    btn.addEventListener("click", async () => {
      btn.disabled = true;
      btn.textContent = "Buscando…";
      buzz();
      try {
        const res = await fetch(UPDATE_URL, { cache: "no-store" });
        if (!res.ok) throw new Error("HTTP " + res.status);
        const data = await res.json();
        const remote = String(data.version || "").trim();
        if (remote && compareVersions(remote, APP_VERSION) > 0) {
          setStatus(`
            <div style="text-align:center">
              <h3 style="margin:0 0 6px">Nueva versión ${escapeHTML(remote)}</h3>
              <p class="muted" style="margin:0 0 14px">${escapeHTML(data.notes || "Hay una actualización disponible.")}</p>
              <button class="btn-filled press" id="installUpdate" data-url="${escapeHTML(data.url || "")}" style="display:inline-flex">Descargar e instalar</button>
            </div>`);
          const inst = $("#installUpdate", root);
          if (inst) inst.addEventListener("click", () => {
            const u = inst.dataset.url;
            if (!u) return;
            inst.disabled = true; inst.textContent = "Descargando…";
            // El Activity intercepta este esquema, descarga y abre el instalador.
            location.href = "ahorro://install?url=" + encodeURIComponent(u);
            setTimeout(() => { inst.disabled = false; inst.textContent = "Descargar e instalar"; }, 3000);
          });
          buzz([10, 40, 10]);
        } else {
          setStatus(`<p style="text-align:center;margin:0">Ya tienes la última versión (${APP_VERSION}).</p>`);
        }
      } catch (e) {
        setStatus(`<p style="text-align:center;margin:0;color:var(--error)">No se pudo comprobar. Verifica la URL de actualización en app.js (UPDATE_URL).<br><small>${escapeHTML(e.message)}</small></p>`);
      } finally {
        btn.disabled = false;
        btn.textContent = "Buscar de nuevo";
      }
    });
  });
}
function compareVersions(a, b) {
  const pa = String(a).split(".").map((x) => parseInt(x, 10) || 0);
  const pb = String(b).split(".").map((x) => parseInt(x, 10) || 0);
  const n = Math.max(pa.length, pb.length);
  for (let i = 0; i < n; i++) {
    const x = pa[i] || 0, y = pb[i] || 0;
    if (x !== y) return x > y ? 1 : -1;
  }
  return 0;
}

/* ------------------------- ajustes ------------------------- */
function renderSettings() {
  const list = $("#settingsList");
  if (!list) return;
  const cur = state.currency || "COP";
  const currencies = ["COP", "USD", "EUR", "MXN", "ARS", "CLP"];
  list.innerHTML = `
    <li class="setting">
      <span class="setting-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/></svg>
      </span>
      <div class="setting-body">
        <strong>Color dinámico</strong>
        <span>Toma el color de acento del sistema (puede venir del fondo de pantalla).</span>
      </div>
      <div class="setting-control">
        <button class="toggle ${state.dynamicColor ? "is-on" : ""}" id="setDynamic" aria-pressed="${state.dynamicColor}"></button>
      </div>
    </li>
    <li class="setting">
      <span class="setting-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
      </span>
      <div class="setting-body">
        <strong>Progreso circular</strong>
        <span>Usa el anillo en lugar de la barra ondulada.</span>
      </div>
      <div class="setting-control">
        <button class="toggle ${state.ringMode ? "is-on" : ""}" id="setRing" aria-pressed="${state.ringMode}"></button>
      </div>
    </li>
    <li class="setting">
      <span class="setting-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
      </span>
      <div class="setting-body">
        <strong>Moneda</strong>
        <span>Formato de los montos.</span>
      </div>
      <div class="setting-control">
        <select id="setCurrency" class="field input-sm">
          ${currencies.map((c) => `<option value="${c}" ${c === cur ? "selected" : ""}>${c}</option>`).join("")}
        </select>
      </div>
    </li>
    <li>
      <button class="setting-btn" id="clearData">
        <span class="setting-icon">${icon("trash")}</span>
        <span>Borrar todos los datos</span>
      </button>
    </li>
    <li class="setting" id="openUpdates">
      <span class="setting-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
      </span>
      <div class="setting-body">
        <strong>Actualizaciones</strong>
        <span>Versión actual ${APP_VERSION}. Buscar e instalar nuevas versiones.</span>
      </div>
      <div class="setting-control">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
      </div>
    </li>
    <li class="setting">
      <span class="setting-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v4M12 16h.01"/></svg>
      </span>
      <div class="setting-body">
        <strong>Acerca de</strong>
        <span>Ahorros · Material 3 Expressive · v${APP_VERSION}</span>
      </div>
    </li>
  `;
  // wire
  $("#setDynamic", list).addEventListener("click", () => {
    toggleDynamic();
    renderSettings();
  });
  $("#setRing", list).addEventListener("click", () => {
    state.ringMode = !state.ringMode;
    save();
    const rt = $("#ringToggle");
    if (rt) rt.setAttribute("aria-pressed", state.ringMode ? "true" : "false");
    render();
    renderSettings();
    buzz();
  });
  $("#setCurrency", list).addEventListener("change", (e) => {
    state.currency = e.target.value;
    _nf = null;
    save();
    render();
    buzz();
  });
  $("#openUpdates", list).addEventListener("click", () => openUpdatesSheet());
  $("#clearData", list).addEventListener("click", () => {
    openConfirm("¿Borrar todos los datos?", "Se eliminarán todas las metas y el historial. Esta acción no se puede deshacer.", () => {
      localStorage.removeItem(STORE);
      state = defaults();
      save();
      render();
      renderSettings();
      buzz(30);
    });
  });
}

const escapeHTML = (s) => String(s).replace(/[&<>"']/g, (c) =>
  ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

function fmtDate(ts) {
  const d = new Date(ts);
  const hoy = new Date();
  const ayer = new Date(Date.now() - 864e5);
  const same = (a, b) => a.toDateString() === b.toDateString();
  const hora = d.toLocaleTimeString("es-CO", { hour: "numeric", minute: "2-digit" });
  if (same(d, hoy)) return `Hoy · ${hora}`;
  if (same(d, ayer)) return `Ayer · ${hora}`;
  return d.toLocaleDateString("es-CO", { day: "numeric", month: "short" }) + ` · ${hora}`;
}

/* ------------------------- acciones ------------------------- */
function addSaving(id, amount) {
  const g = state.goals.find((x) => x.id === id);
  if (!g || !(amount > 0)) return 0;
  const before = pctOf(g);
  g.saved += amount;
  state.moves.unshift({ id: uid(), goalId: g.id, name: g.name, amount, ts: Date.now(), tint: g.tint });
  if (state.moves.length > 200) state.moves.length = 200;
  save();
  render();
  const el = $(`#goalList [data-id="${id}"]`);
  bump($("#totalAmount"));
  if (el) {
    const r = el.getBoundingClientRect();
    floatAmount(r.left + r.width / 2, r.top + 34, "+" + money(amount));
    if (before < 100 && pctOf(g) >= 100) {
      burst(r.left + r.width / 2, r.top + r.height / 2);
      buzz([14, 60, 24]);
    } else buzz();
  }
  return pctOf(g) - before;
}

function withdrawSaving(id, amount) {
  const g = state.goals.find((x) => x.id === id);
  if (!g || !(amount > 0)) return 0;
  const v = Math.min(amount, g.saved);
  if (!(v > 0)) return 0;
  const before = pctOf(g);
  g.saved -= v;
  state.moves.unshift({ id: uid(), goalId: g.id, name: g.name, amount: v, ts: Date.now(), tint: g.tint, type: "withdraw" });
  if (state.moves.length > 200) state.moves.length = 200;
  save();
  render();
  const el = $(`#goalList [data-id="${id}"]`);
  bump($("#totalAmount"));
  if (el) {
    const r = el.getBoundingClientRect();
    floatAmount(r.left + r.width / 2, r.top + 34, "-" + money(v));
    buzz(15);
  }
  return before - pctOf(g);
}

function createGoal(name, target) {
  const tint = TINTS[state.goals.length % TINTS.length];
  const g = { id: uid(), name: name.trim().slice(0, 42), target, saved: 0, tint, ts: Date.now() };
  state.goals.unshift(g);
  save();
  render();
  return g;
}

function deleteGoal(id) {
  state.goals = state.goals.filter((g) => g.id !== id);
  save();
  render();
  buzz(20);
}

/* ------------------------- hoja (bottom sheet) ------------------------- */
const sheetEl = $("#sheet");
const scrimEl = $("#scrim");
const sheetBody = $("#sheetBody");
let sheetCloseHandler = null;

function openSheet(title, sub, html, mount) {
  sheetBody.innerHTML = `
    <h2 class="sheet-title" id="sheetTitle">${title}</h2>
    ${sub ? `<p class="sheet-sub">${sub}</p>` : ""}
    ${html}`;
  const t = $("#sheetTitle");
  if (t) t.id = "sheetTitle";
  sheetEl.setAttribute("aria-label", t ? t.textContent : "Diálogo");
  scrimEl.classList.add("open");
  sheetEl.classList.add("open");
  document.body.style.overflow = "hidden";
  sheetEl.scrollTop = 0;
  const closer = () => closeSheet();
  sheetCloseHandler = closer;
  sheetBody.querySelectorAll("[data-close]").forEach((b) => b.addEventListener("click", closer));
  mount?.(sheetBody, closer);
  setTimeout(() => sheetBody.querySelector("input")?.focus({ preventScroll: true }), 320);
}

function closeSheet() {
  scrimEl.classList.remove("open");
  sheetEl.classList.remove("open");
  document.body.style.overflow = "";
  sheetCloseHandler = null;
  setTimeout(() => { if (!sheetEl.classList.contains("open")) sheetBody.innerHTML = ""; }, 420);
}

scrimEl.addEventListener("click", closeSheet);

/* ------------------------- gestos: deslizar para cambiar de apartado ------------------------- */
const VIEWS = ["goals", "history", "settings"];
let touchX = 0, touchY = 0, swiping = false;

document.addEventListener("touchstart", (e) => {
  swiping = false;
  if (e.touches.length !== 1) return;
  // No cambiar de apartado si hay un diálogo o la bienvenida abierta.
  if (sheetEl.classList.contains("open") || $("#welcome")) return;
  touchX = e.touches[0].clientX;
  touchY = e.touches[0].clientY;
  swiping = true;
}, { passive: true });

document.addEventListener("touchend", (e) => {
  if (!swiping || !e.changedTouches.length) return;
  swiping = false;
  const dx = e.changedTouches[0].clientX - touchX;
  const dy = e.changedTouches[0].clientY - touchY;
  // Solo si el gesto fue claramente horizontal (no un scroll vertical).
  if (Math.abs(dx) < 55 || Math.abs(dx) < Math.abs(dy) * 1.4) return;
  const current = VIEWS.indexOf(document.body.dataset.view || "goals");
  if (current < 0) return;
  // Deslizar a la derecha → apartado de la derecha; a la izquierda → el de la izquierda.
  const next = dx > 0 ? current + 1 : current - 1;
  if (next < 0 || next >= VIEWS.length) return;
  switchView(VIEWS[next]);
}, { passive: true });
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && sheetEl.classList.contains("open")) closeSheet();
});

/* ------------------------- nueva meta ------------------------- */
function openNewGoal() {
  openSheet("Nueva meta", "Ponle nombre y define cuánto quieres alcanzar.", `
    <label class="field"><span>Nombre</span>
      <input id="f-name" maxlength="42" autocomplete="off" placeholder="Ej. Viaje a la playa" />
    </label>
    <label class="field"><span>Monto objetivo</span>
      <div class="amount-wrap">
        <span class="cur">$</span>
        <input id="f-target" inputmode="numeric" autocomplete="off" placeholder="0" />
      </div>
    </label>
    <div class="chips">
      ${[500000, 1000000, 5000000].map((v) => `<button class="chip-btn press" data-target="${v}">${group(v)}</button>`).join("")}
    </div>
    <div class="sheet-actions">
      <button class="btn-tonal press" data-close>Cancelar</button>
      <button class="btn-filled press" id="f-save">Crear meta</button>
    </div>`, (root, close) => {
    const name = $("#f-name", root);
    const target = $("#f-target", root);
    root.querySelectorAll("[data-target]").forEach((b) =>
      b.addEventListener("click", () => {
        target.value = group(b.dataset.target);
        root.querySelectorAll("[data-target]").forEach((x) => x.classList.toggle("sel", x === b));
        buzz(8);
      }));
    target.addEventListener("input", () => {
      target.value = digits(target.value) ? group(digits(target.value)) : "";
    });
    const submit = () => {
      const t = Number(digits(target.value));
      if (!name.value.trim()) { shake(name); name.focus(); return; }
      if (!(t > 0)) { shake(target); target.focus(); return; }
      createGoal(name.value, t);
      close();
      buzz();
    };
    $("#f-save", root).addEventListener("click", submit);
    root.addEventListener("keydown", (e) => { if (e.key === "Enter") submit(); });
  });
}

/* ------------------------- aportar ------------------------- */
function openDeposit(id) {
  const g = state.goals.find((x) => x.id === id);
  if (!g) return;
  const p = pctOf(g);
  const rest = Math.max(0, g.target - g.saved);
  openSheet(`Añadir a <em>${escapeHTML(g.name)}</em>`,
    `Llevas ${money(g.saved)} de ${money(g.target)}`, `
    <div class="amount-wrap">
      <span class="cur">$</span>
      <input id="d-amt" inputmode="numeric" autocomplete="off" placeholder="0" aria-label="Monto a ahorrar" />
    </div>
    <div class="chips">
      ${[50000, 100000, 500000].map((v) => `<button class="chip-btn press" data-add="${v}">+${group(v)}</button>`).join("")}
      ${rest > 0 && rest <= 100000000 ? `<button class="chip-btn press" data-set="${rest}">Completar</button>` : ""}
    </div>
    <div class="goal-preview">
      <div class="goal-preview-head"><span>Progreso nuevo</span><b id="d-pct">${Math.round(p)}%</b></div>
      <div class="prog" id="d-prog" style="--p:${p};--tint-raw:${g.tint}">
        <span class="prog-track"></span><span class="prog-fill"></span><span class="prog-stop"></span>
      </div>
    </div>
    <div class="sheet-actions">
      <button class="btn-tonal press" data-close>Cancelar</button>
      <button class="btn-filled press" id="d-save">Ahorrar</button>
    </div>`, (root, close) => {
    const amt = $("#d-amt", root);
    const prog = $("#d-prog", root);
    const pctEl = $("#d-pct", root);
    const preview = () => {
      const v = Number(digits(amt.value)) || 0;
      const np = g.target > 0 ? clamp(((g.saved + v) / g.target) * 100, 0, 100) : 0;
      prog.style.setProperty("--p", np);
      prog.dataset.on = np > 0 ? "true" : "false";
      pctEl.textContent = Math.round(np) + "%";
    };
    amt.addEventListener("input", () => {
      amt.value = digits(amt.value) ? group(digits(amt.value)) : "";
      preview();
    });
    root.querySelectorAll("[data-add]").forEach((b) =>
      b.addEventListener("click", () => {
        amt.value = group((Number(digits(amt.value)) || 0) + Number(b.dataset.add));
        preview(); buzz(8);
      }));
    root.querySelectorAll("[data-set]").forEach((b) =>
      b.addEventListener("click", () => {
        amt.value = group(Number(b.dataset.set));
        preview(); buzz(8);
      }));
    const submit = () => {
      const v = Number(digits(amt.value));
      if (!(v > 0)) { shake(amt); amt.focus(); return; }
      close();
      setTimeout(() => addSaving(id, v), 120);
    };
    $("#d-save", root).addEventListener("click", submit);
    root.addEventListener("keydown", (e) => { if (e.key === "Enter") submit(); });
    preview();
  });
}

/* ------------------------- retirar ------------------------- */
function openWithdraw(id) {
  const g = state.goals.find((x) => x.id === id);
  if (!g) return;
  if (!(g.saved > 0)) {
    openSheet("Retirar dinero", "Esta meta todavía no tiene ahorros.", `
      <div class="sheet-actions">
        <button class="btn-filled press" data-close>Entendido</button>
      </div>`);
    return;
  }
  const p = pctOf(g);
  openSheet(`Retirar de <em>${escapeHTML(g.name)}</em>`,
    `Disponible: ${money(g.saved)}`, `
    <div class="amount-wrap">
      <span class="cur">$</span>
      <input id="w-amt" inputmode="numeric" autocomplete="off" placeholder="0" aria-label="Monto a retirar" />
    </div>
    <div class="chips">
      ${[50000, 100000, 500000].filter((v) => v <= g.saved).map((v) => `<button class="chip-btn press" data-add="${v}">${group(v)}</button>`).join("")}
      <button class="chip-btn press" data-all="1">Todo</button>
    </div>
    <div class="goal-preview">
      <div class="goal-preview-head"><span>Progreso nuevo</span><b id="w-pct">${Math.round(p)}%</b></div>
      <div class="prog" id="w-prog" style="--p:${p};--tint-raw:${g.tint}">
        <span class="prog-track"></span><span class="prog-fill"></span><span class="prog-stop"></span>
      </div>
    </div>
    <div class="sheet-actions">
      <button class="btn-tonal press" data-close>Cancelar</button>
      <button class="btn-tonal press btn-danger" id="w-save">Retirar</button>
    </div>`, (root, close) => {
    const amt = $("#w-amt", root);
    const prog = $("#w-prog", root);
    const pctEl = $("#w-pct", root);
    const preview = () => {
      const v = Math.min(Number(digits(amt.value)) || 0, g.saved);
      const np = g.target > 0 ? clamp(((g.saved - v) / g.target) * 100, 0, 100) : 0;
      prog.style.setProperty("--p", np);
      prog.dataset.on = np > 0 ? "true" : "false";
      pctEl.textContent = Math.round(np) + "%";
    };
    const setVal = (v) => { amt.value = v > 0 ? group(v) : ""; preview(); };
    amt.addEventListener("input", () => {
      amt.value = digits(amt.value) ? group(digits(amt.value)) : "";
      preview();
    });
    root.querySelectorAll("[data-add]").forEach((b) =>
      b.addEventListener("click", () => {
        setVal((Number(digits(amt.value)) || 0) + Number(b.dataset.add));
        buzz(8);
      }));
    const all = $('[data-all="1"]', root);
    if (all) all.addEventListener("click", () => { setVal(g.saved); buzz(8); });
    const submit = () => {
      const v = Number(digits(amt.value));
      if (!(v > 0) || v > g.saved) { shake(amt); amt.focus(); return; }
      close();
      setTimeout(() => withdrawSaving(id, v), 120);
    };
    $("#w-save", root).addEventListener("click", submit);
    root.addEventListener("keydown", (e) => { if (e.key === "Enter") submit(); });
    preview();
  });
}

/* ------------------------- opciones de meta ------------------------- */
function openGoalOptions(id) {
  const g = state.goals.find((x) => x.id === id);
  if (!g) return;
  openSheet(escapeHTML(g.name), `${money(g.saved)} de ${money(g.target)} · ${Math.round(pctOf(g))}%`, `
    <div class="sheet-actions" style="flex-direction:column;margin-top:6px">
      <button class="btn-filled press" id="o-add" style="width:100%">Añadir dinero</button>
      <button class="btn-tonal press" id="o-withdraw" style="width:100%">Retirar dinero</button>
      <button class="btn-tonal press btn-danger" id="o-del" style="width:100%">Eliminar meta</button>
    </div>`, (root, close) => {
    $("#o-add", root).addEventListener("click", () => { close(); setTimeout(() => openDeposit(id), 180); });
    $("#o-withdraw", root).addEventListener("click", () => { close(); setTimeout(() => openWithdraw(id), 180); });
    $("#o-del", root).addEventListener("click", () => {
      close();
      setTimeout(() => openConfirm(`¿Eliminar "${g.name}"?`, "Se borrará la meta. Los aportes ya registrados quedan en el historial.", () => deleteGoal(id)), 180);
    });
  });
}

function openConfirm(title, text, onYes) {
  openSheet(title, text, `
    <div class="sheet-actions">
      <button class="btn-tonal press" data-close>Cancelar</button>
      <button class="btn-filled press" id="c-yes" style="background:var(--error);color:#fff;box-shadow:none">Eliminar</button>
    </div>`, (root, close) => {
    $("#c-yes", root).addEventListener("click", () => { close(); setTimeout(onYes, 150); });
  });
}

function shake(el) {
  if (reduceMotion.matches) return;
  el.animate?.(
    [{ transform: "translateX(0)" }, { transform: "translateX(-7px)" }, { transform: "translateX(7px)" },
     { transform: "translateX(-4px)" }, { transform: "translateX(0)" }],
    { duration: 340, easing: "ease-in-out" }
  );
}

/* ------------------------- navegación ------------------------- */
function switchView(name) {
  $$(".view").forEach((v) => { v.hidden = v.id !== `view-${name}`; });
  $$(".nav-item").forEach((b) => b.classList.toggle("is-active", b.dataset.view === name));
  document.body.dataset.view = name;
  if (name === "settings") renderSettings();
  placeIndicator();
  window.scrollTo({ top: 0, behavior: reduceMotion.matches ? "auto" : "smooth" });
  buzz(8);
}

function placeIndicator() {
  const active = $(".nav-item.is-active");
  const ind = $("#navIndicator");
  if (!active || !ind) return;
  ind.style.left = active.offsetLeft + "px";
  ind.style.width = active.offsetWidth + "px";
}

$$(".nav-item").forEach((b) => b.addEventListener("click", () => switchView(b.dataset.view)));
window.addEventListener("resize", placeIndicator);

/* ------------------------- onda que arrastra la bolita ------------------------- */
let rideRaf = 0;
function rideWaves() {
  if (reduceMotion.matches) return;
  document.querySelectorAll(".prog").forEach((prog) => {
    const stop = prog.querySelector(".prog-stop");
    if (!stop) return;
    const p = Number(prog.style.getPropertyValue("--p")) || 0;
    if (p <= 0.5 || p >= 99.5) { stop.style.setProperty("--dy", "0px"); return; }
    const W = prog.clientWidth || 1;
    const H = prog.clientHeight || 14;
    const amp = (4 / 14) * H;
    const dotX = W * (p / 100);
    const period = parseFloat(getComputedStyle(prog).getPropertyValue("--wave")) || 38;
    const t = (performance.now() / 1000) % 2.0;
    const scroll = (t / 2.0) * period;
    const phase = ((dotX - scroll) / period) * Math.PI * 2;
    // signo invertido para que la bolita cabalgue sobre la onda (cresta arriba)
    const dy = -Math.sin(phase) * amp;
    stop.style.setProperty("--dy", dy.toFixed(2) + "px");
  });
  rideRaf = requestAnimationFrame(rideWaves);
}
function startRide() { if (!rideRaf && !reduceMotion.matches) rideRaf = requestAnimationFrame(rideWaves); }
function stopRide() { cancelAnimationFrame(rideRaf); rideRaf = 0; }
reduceMotion.addEventListener("change", () => { if (reduceMotion.matches) stopRide(); else startRide(); });

/* ------------------------- anillo ondulado que fluye ------------------------- */
let waveRaf = 0;
function drawWavyRings() {
  const t = performance.now() / 1000;
  const phase = (t / 4) * Math.PI * 2; // la onda viaja alrededor en 4 s
  document.querySelectorAll(".ring").forEach((ring) => {
    const svg = ring.querySelector("svg");
    if (!svg) return;
    const vb = svg.viewBox.baseVal;
    const size = vb.width || 120;
    const cx = size / 2, cy = size / 2;
    const R = size * 0.43;
    const A = size * 0.045;
    const n = size >= 100 ? 9 : 6;
    const steps = 200;
    let d = "";
    for (let i = 0; i <= steps; i++) {
      const theta = (i / steps) * Math.PI * 2;
      const r = R + A * Math.sin(n * theta + phase);
      const x = cx + r * Math.cos(theta);
      const y = cy + r * Math.sin(theta);
      d += (i === 0 ? "M" : " L") + " " + x.toFixed(2) + " " + y.toFixed(2);
    }
    d += " Z";
    const track = svg.querySelector(".ring-track");
    const fill = svg.querySelector(".ring-fill");
    if (track) track.setAttribute("d", d);
    if (fill) {
      fill.setAttribute("d", d);
      const target = parseFloat(ring.dataset.targetP) || 0;
      if (ring._dispP == null) ring._dispP = target;
      ring._dispP += (target - ring._dispP) * 0.12;
      const L = fill.getTotalLength();
      fill.style.strokeDasharray = L;
      fill.style.strokeDashoffset = (L * (1 - ring._dispP / 100)).toFixed(2);
    }
  });
  if (!reduceMotion.matches) waveRaf = requestAnimationFrame(drawWavyRings);
}
function startWave() { if (!waveRaf && !reduceMotion.matches) waveRaf = requestAnimationFrame(drawWavyRings); }
function stopWave() { cancelAnimationFrame(waveRaf); waveRaf = 0; }
reduceMotion.addEventListener("change", () => { if (reduceMotion.matches) stopWave(); else startWave(); });

/* ------------------------- arranque ------------------------- */
$("#dynamicToggle")?.addEventListener("click", toggleDynamic);
$("#ringToggle")?.addEventListener("click", () => {
  state.ringMode = !state.ringMode;
  save();
  buzz();
  const rt = $("#ringToggle");
  if (rt) {
    rt.setAttribute("aria-pressed", state.ringMode ? "true" : "false");
    rt.setAttribute("aria-label", state.ringMode ? "Cambiar a progreso lineal" : "Cambiar a progreso circular");
  }
  render();
  if (state.ringMode) {
    document.querySelectorAll(".ring").forEach((r) => {
      r.classList.remove("ring-enter");
      void r.offsetWidth;
      r.classList.add("ring-enter");
      setTimeout(() => r.classList.remove("ring-enter"), 700);
    });
  }
});
$("#newGoalBtn")?.addEventListener("click", () => openNewGoal());
$("#emptyCreate")?.addEventListener("click", () => openNewGoal());

applyThemeMode(state.themeMode || "auto");
applyDynamic(state.dynamicColor !== false);
if ($("#ringToggle")) $("#ringToggle").setAttribute("aria-pressed", state.ringMode ? "true" : "false");
if ($("#dynamicToggle")) $("#dynamicToggle").setAttribute("aria-pressed", state.dynamicColor !== false ? "true" : "false");
document.body.dataset.view = "goals";
render();
startWave();
requestAnimationFrame(placeIndicator);
document.fonts?.ready.then(placeIndicator);

/* ------------------------- Material You (color del fondo de pantalla) ------------------------- */
// El Activity nativo inyecta los colores del esquema dinámico del sistema.
window.__applyWallpaperColors = (c) => {
  if (!c) return;
  const r = document.documentElement;
  const set = (k, v) => { if (v) r.style.setProperty(k, v); };
  set("--primary", c.primary);
  set("--on-primary", c.onPrimary);
  set("--primary-container", c.primaryContainer);
  set("--on-primary-container", c.onPrimaryContainer);
  set("--secondary-container", c.secondaryContainer);
  set("--on-secondary-container", c.onSecondaryContainer);
  set("--tertiary", c.tertiary);
  set("--surface", c.surface);
  set("--surface-container-low", c.surfaceLow);
  set("--on-surface", c.onSurface);
  set("--on-surface-variant", c.onSurfaceVariant);
  const meta = document.querySelector('meta[name="theme-color"]:not([media])');
  if (meta && c.surface) meta.content = c.surface;
};

/* ------------------------- pantalla de bienvenida ------------------------- */
const WELCOME_KEY = "ahorros.welcome.v1";
function maybeWelcome() {
  let seen = false;
  try { seen = localStorage.getItem(WELCOME_KEY) === "1"; } catch {}
  if (seen) return;
  const el = document.createElement("div");
  el.className = "welcome";
  el.id = "welcome";
  el.innerHTML = `
    <div class="welcome-card">
      <div class="welcome-art" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 8.5A2.5 2.5 0 0 1 5.5 6H18a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3z"/>
          <path d="M16 12.5h2.5"/><path d="M12 3v3M6 3v3M18 3v3"/>
        </svg>
      </div>
      <h2>Tu dinero, con rumbo</h2>
      <p>Crea metas, registra aportes y mira cómo crece tu ahorro. Todo se guarda en tu celular, sin cuentas ni internet.</p>
      <button class="btn-filled press" id="welcomeStart">Empezar a ahorrar</button>
    </div>`;
  document.body.appendChild(el);
  $("#welcomeStart", el).addEventListener("click", () => {
    try { localStorage.setItem(WELCOME_KEY, "1"); } catch {}
    el.classList.add("welcome-out");
    setTimeout(() => el.remove(), 480);
    buzz(20);
  });
}
maybeWelcome();

// En la APK (file://) no registramos service worker: podría cachear una versión vieja.
