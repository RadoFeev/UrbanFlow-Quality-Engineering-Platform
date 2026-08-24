(() => {
  "use strict";
  const STORE = "urbanflow.operations.v1";
  const pageSize = 5;
  const state = load();
  const byId = (id) => document.getElementById(id);

  function load() {
    try {
      const saved = Object.assign({ session: null, page: 0 }, JSON.parse(localStorage.getItem(STORE) || "{}"));
      if (!saved.shipments) Object.assign(saved, seed());
      return saved;
    } catch {
      return Object.assign({ session: null, page: 0 }, seed());
    }
  }
  function seed() {
    const statuses = ["CONFIRMED", "COURIER_ASSIGNED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "DELIVERY_ATTEMPTED"];
    const cities = ["Sofia", "Plovdiv", "Varna", "Burgas"];
    const shipments = Array.from({ length: 14 }, (_, index) => ({ id: crypto.randomUUID(), tracking: `UF-OP${String(index + 1).padStart(6, "0")}`, status: statuses[index % statuses.length], route: `${cities[index % cities.length]} → ${cities[(index + 1) % cities.length]}`, createdAt: new Date(Date.now() - index * 3600000).toISOString(), updatedAt: new Date(Date.now() - index * 600000).toISOString(), version: index % 3 }));
    return {
      shipments,
      users: [
        { id: crypto.randomUUID(), name: "Customer 104", role: "CUSTOMER", active: true, version: 0 },
        { id: crypto.randomUUID(), name: "Courier 218", role: "COURIER", active: true, version: 1 },
        { id: crypto.randomUUID(), name: "Operator 07", role: "OPERATOR", active: true, version: 2 }
      ],
      couriers: [
        { id: "CR-0218", name: "Courier 218", status: "AVAILABLE", load: "2 / 8" },
        { id: "CR-0314", name: "Courier 314", status: "ASSIGNED", load: "6 / 8" },
        { id: "CR-0420", name: "Courier 420", status: "OFF DUTY", load: "0 / 8" }
      ],
      hubs: [
        { code: "SOF01", name: "Sofia North", active: true, capacity: "68%" },
        { code: "PDV01", name: "Plovdiv Central", active: true, capacity: "51%" },
        { code: "VAR01", name: "Varna Port", active: false, capacity: "0%" }
      ],
      incidents: [
        { id: crypto.randomUUID(), tracking: "UF-OP000006", type: "DELAY", description: "Destination hub processing delay", status: "OPEN", version: 0 },
        { id: crypto.randomUUID(), tracking: "UF-OP000012", type: "DELIVERY", description: "Repeated delivery attempt", status: "OPEN", version: 1 },
        { id: crypto.randomUUID(), tracking: "UF-OP000003", type: "ROUTE", description: "Route reassigned", status: "RESOLVED", version: 1 }
      ],
      flags: [
        { id: crypto.randomUUID(), key: "routing.v2", description: "Capacity-aware route selection", enabled: false, version: 0 },
        { id: crypto.randomUUID(), key: "customer.dark-mode", description: "Customer portal theme switch", enabled: true, version: 2 },
        { id: crypto.randomUUID(), key: "courier.offline-sync", description: "Offline delivery update queue", enabled: true, version: 1 }
      ],
      audit: []
    };
  }
  function save() { localStorage.setItem(STORE, JSON.stringify(state)); }
  function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[character]); }
  function message(element, text, ok = false) { element.textContent = text; element.classList.toggle("ok", ok); }
  function toast(text) { const element = byId("toast"); element.textContent = text; element.classList.add("show"); window.setTimeout(() => element.classList.remove("show"), 2200); }
  function audit(action, entity, details) { state.audit.unshift({ id: crypto.randomUUID(), at: new Date().toISOString(), actor: state.session.operatorId, action, entity, details }); }
  function mayChange() { return state.session && state.session.role !== "VIEWER"; }
  function isAdmin() { return state.session?.role === "ADMIN"; }

  byId("ops-login-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const operatorId = byId("operator-id").value.trim().toUpperCase();
    const code = byId("operator-code").value;
    const role = byId("workspace-role").value;
    if (!/^[A-Z]{2}-\d{4}$/.test(operatorId) || code.length < 10) return message(byId("ops-login-message"), "Enter a valid operator ID and access code.");
    state.session = { operatorId, role, signedInAt: Date.now() };
    audit("SESSION_STARTED", "Operator", `role=${role}`);
    save(); openDashboard();
  });

  byId("ops-logout").addEventListener("click", () => {
    if (state.session) audit("SESSION_ENDED", "Operator", "operator logout");
    state.session = null; save();
    document.querySelectorAll(".panel").forEach((panel) => panel.classList.remove("active"));
    byId("login").classList.add("active");
    document.querySelector(".sidebar").classList.remove("open");
  });

  function openDashboard() {
    byId("login").classList.remove("active");
    byId("operator-name").textContent = state.session.operatorId;
    byId("operator-role").textContent = state.session.role;
    byId("operator-badge").textContent = state.session.operatorId.slice(0, 2);
    document.querySelectorAll("[data-permission='ADMIN']").forEach((button) => {
      button.setAttribute("aria-disabled", String(!isAdmin()));
    });
    showPanel("overview");
  }

  document.querySelectorAll("[data-panel]").forEach((button) => button.addEventListener("click", () => {
    if (button.dataset.permission === "ADMIN" && !isAdmin()) return toast("Administrator permission is required.");
    showPanel(button.dataset.panel);
  }));
  function showPanel(id) {
    document.querySelectorAll(".panel").forEach((panel) => panel.classList.toggle("active", panel.id === id));
    document.querySelectorAll("[data-panel]").forEach((button) => button.classList.toggle("active", button.dataset.panel === id));
    document.querySelector(".sidebar").classList.remove("open");
    byId("menu-toggle").setAttribute("aria-expanded", "false");
    render(id);
    byId("main").focus();
  }
  byId("menu-toggle").addEventListener("click", () => {
    const open = document.querySelector(".sidebar").classList.toggle("open");
    byId("menu-toggle").setAttribute("aria-expanded", String(open));
  });

  function render(id) {
    if (id === "overview") renderOverview();
    if (id === "shipments") renderShipments();
    if (id === "users") renderUsers();
    if (id === "network") renderNetwork();
    if (id === "incidents") renderIncidents();
    if (id === "flags") renderFlags();
    if (id === "audit") renderAudit();
  }

  function renderOverview() {
    const active = state.shipments.filter((shipment) => shipment.status !== "DELIVERED").length;
    byId("metric-active").textContent = active;
    byId("metric-ontime").textContent = `${Math.round(100 * state.shipments.filter((shipment) => shipment.status !== "DELIVERY_ATTEMPTED").length / state.shipments.length)}%`;
    byId("metric-incidents").textContent = state.incidents.filter((incident) => incident.status === "OPEN").length;
    byId("metric-couriers").textContent = state.couriers.filter((courier) => courier.status !== "OFF DUTY").length;
    byId("dashboard-time").textContent = new Date().toLocaleString();
    const counts = Object.groupBy(state.shipments, (shipment) => shipment.status);
    const maximum = Math.max(...Object.values(counts).map((items) => items.length));
    byId("status-chart").innerHTML = Object.entries(counts).map(([status, items]) => `<div class="bar"><strong>${items.length}</strong><i style="height:${Math.max(8, 150 * items.length / maximum)}px"></i><span>${escapeHtml(status.replaceAll("_", " "))}</span></div>`).join("");
    const problems = state.incidents.filter((incident) => incident.status === "OPEN");
    byId("problem-list").innerHTML = problems.map((incident) => `<article class="problem"><strong>${escapeHtml(incident.tracking)}</strong><p>${escapeHtml(incident.description)}</p><span class="pill alert">${escapeHtml(incident.type)}</span></article>`).join("");
  }

  ["shipment-search", "ops-status-filter", "shipment-sort"].forEach((id) => byId(id).addEventListener(id === "shipment-search" ? "input" : "change", () => { state.page = 0; renderShipments(); }));
  function filteredShipments() {
    const query = byId("shipment-search").value.trim().toLowerCase();
    const status = byId("ops-status-filter").value;
    const [property, direction] = byId("shipment-sort").value.split("-");
    const key = property === "created" ? "createdAt" : property;
    return state.shipments.filter((shipment) => (!query || shipment.tracking.toLowerCase().includes(query) || shipment.route.toLowerCase().includes(query)) && (status === "ALL" || shipment.status === status)).toSorted((left, right) => String(left[key]).localeCompare(String(right[key])) * (direction === "asc" ? 1 : -1));
  }
  function renderShipments() {
    const shipments = filteredShipments();
    const pages = Math.max(1, Math.ceil(shipments.length / pageSize));
    state.page = Math.min(state.page, pages - 1);
    const visible = shipments.slice(state.page * pageSize, (state.page + 1) * pageSize);
    byId("shipments-body").innerHTML = visible.length ? visible.map((shipment) => `<tr data-id="${shipment.id}"><td><strong>${escapeHtml(shipment.tracking)}</strong></td><td><span class="pill ${shipment.status === "DELIVERY_ATTEMPTED" ? "alert" : ""}">${escapeHtml(shipment.status)}</span></td><td>${escapeHtml(shipment.route)}</td><td>${new Date(shipment.updatedAt).toLocaleString()}</td><td>v${shipment.version}</td><td><button data-action="manual-status" type="button" ${!mayChange() ? "disabled" : ""}>Change</button></td></tr>`).join("") : '<tr><td colspan="6" class="empty">No shipments match these filters.</td></tr>';
    byId("page-status").textContent = `Page ${state.page + 1} of ${pages}`;
    byId("previous-page").disabled = state.page === 0;
    byId("next-page").disabled = state.page >= pages - 1;
  }
  byId("previous-page").addEventListener("click", () => { state.page -= 1; renderShipments(); });
  byId("next-page").addEventListener("click", () => { state.page += 1; renderShipments(); });

  byId("shipments-body").addEventListener("click", (event) => {
    const button = event.target.closest("[data-action='manual-status']"); if (!button) return;
    if (!mayChange()) return toast("Your role cannot change shipment state.");
    const shipment = state.shipments.find((item) => item.id === button.closest("[data-id]").dataset.id);
    byId("manual-shipment-id").value = shipment.id; byId("manual-version").value = shipment.version; byId("manual-status").value = shipment.status === "DELIVERED" ? "DELIVERED" : "IN_TRANSIT"; byId("manual-reason").value = ""; byId("simulate-conflict").checked = false; byId("manual-message").textContent = ""; byId("status-dialog").showModal();
  });
  byId("manual-status-form").addEventListener("submit", (event) => {
    event.preventDefault(); if (event.submitter?.value === "cancel") return byId("status-dialog").close();
    const shipment = state.shipments.find((item) => item.id === byId("manual-shipment-id").value);
    const expected = Number(byId("manual-version").value);
    const reason = byId("manual-reason").value.trim();
    if (!reason) return message(byId("manual-message"), "A reason is required for a manual change.");
    if (byId("simulate-conflict").checked) shipment.version += 1;
    if (shipment.version !== expected) return message(byId("manual-message"), `Version conflict: expected v${expected}, current v${shipment.version}.`);
    if (byId("manual-status").value === "CANCELLED" && !isAdmin()) return message(byId("manual-message"), "Administrator permission is required to cancel a shipment.");
    shipment.status = byId("manual-status").value; shipment.version += 1; shipment.updatedAt = new Date().toISOString();
    audit("SHIPMENT_STATUS_CHANGED", shipment.tracking, `${shipment.status}; reason=${reason}`); save(); byId("status-dialog").close(); renderShipments(); toast("Shipment status updated.");
  });

  byId("export-csv").addEventListener("click", () => {
    const rows = [["tracking", "status", "route", "updatedAt", "version"], ...filteredShipments().map((shipment) => [shipment.tracking, shipment.status, shipment.route, shipment.updatedAt, shipment.version])];
    const csv = rows.map((row) => row.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(",")).join("\n");
    const link = document.createElement("a"); link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv" })); link.download = "urbanflow-shipments.csv"; link.click(); URL.revokeObjectURL(link.href);
    audit("SHIPMENTS_EXPORTED", "Shipment", `${rows.length - 1} records`); save(); toast("CSV export prepared.");
  });

  function renderUsers() {
    byId("users-body").innerHTML = state.users.map((user) => `<tr data-id="${user.id}"><td>${escapeHtml(user.name)}</td><td><select data-role ${!isAdmin() ? "disabled" : ""}><option ${user.role === "CUSTOMER" ? "selected" : ""}>CUSTOMER</option><option ${user.role === "COURIER" ? "selected" : ""}>COURIER</option><option ${user.role === "OPERATOR" ? "selected" : ""}>OPERATOR</option><option ${user.role === "ADMIN" ? "selected" : ""}>ADMIN</option></select></td><td>${user.active ? "ACTIVE" : "SUSPENDED"}</td><td>v${user.version}</td><td><button data-action="save-role" ${!isAdmin() ? "disabled" : ""} type="button">Save</button></td></tr>`).join("");
  }
  byId("users-body").addEventListener("click", (event) => {
    const button = event.target.closest("[data-action='save-role']"); if (!button) return;
    if (!isAdmin()) return message(byId("users-message"), "Administrator permission is required.");
    const row = button.closest("[data-id]"); const user = state.users.find((item) => item.id === row.dataset.id); const role = row.querySelector("[data-role]").value;
    if (user.role === role) return message(byId("users-message"), "No role change to save.");
    user.role = role; user.version += 1; audit("USER_ROLE_CHANGED", user.name, `role=${role}`); save(); renderUsers(); message(byId("users-message"), "Role updated.", true);
  });

  function renderNetwork() {
    byId("courier-list").innerHTML = state.couriers.map((courier) => `<div class="resource"><span><strong>${escapeHtml(courier.name)}</strong><small>${escapeHtml(courier.id)} · ${escapeHtml(courier.status)}</small></span><span class="pill">${escapeHtml(courier.load)}</span></div>`).join("");
    byId("hub-list").innerHTML = state.hubs.map((hub) => `<div class="resource"><span><strong>${escapeHtml(hub.name)}</strong><small>${escapeHtml(hub.code)} · ${hub.active ? "ACTIVE" : "INACTIVE"}</small></span><span class="pill">${escapeHtml(hub.capacity)}</span></div>`).join("");
  }

  byId("incident-filter").addEventListener("change", renderIncidents);
  function renderIncidents() {
    const filter = byId("incident-filter").value; const items = state.incidents.filter((incident) => filter === "ALL" || incident.status === filter);
    byId("incident-list").innerHTML = items.length ? items.map((incident) => `<article class="incident" data-id="${incident.id}"><div><strong>${escapeHtml(incident.tracking)} · ${escapeHtml(incident.type)}</strong><p>${escapeHtml(incident.description)}</p><span class="pill ${incident.status === "OPEN" ? "alert" : ""}">${incident.status} · v${incident.version}</span></div>${incident.status === "OPEN" ? `<button data-action="resolve" ${!mayChange() ? "disabled" : ""} type="button">Resolve</button>` : ""}</article>`).join("") : '<p class="empty">No incidents in this state.</p>';
  }
  byId("incident-list").addEventListener("click", (event) => { const button = event.target.closest("[data-action='resolve']"); if (!button) return; const incident = state.incidents.find((item) => item.id === button.closest("[data-id]").dataset.id); incident.status = "RESOLVED"; incident.version += 1; audit("INCIDENT_RESOLVED", incident.tracking, incident.type); save(); renderIncidents(); toast("Incident resolved."); });

  function renderFlags() {
    byId("flag-list").innerHTML = state.flags.map((flag) => `<article class="flag" data-id="${flag.id}"><div><strong>${escapeHtml(flag.key)}</strong><p>${escapeHtml(flag.description)}</p><span class="pill">v${flag.version}</span></div><label class="toggle"><input data-action="toggle-flag" type="checkbox" ${flag.enabled ? "checked" : ""} ${!isAdmin() ? "disabled" : ""}><span>${flag.enabled ? "Enabled" : "Disabled"}</span></label></article>`).join("");
  }
  byId("flag-list").addEventListener("change", (event) => { if (!event.target.matches("[data-action='toggle-flag']")) return; if (!isAdmin()) return; const flag = state.flags.find((item) => item.id === event.target.closest("[data-id]").dataset.id); flag.enabled = event.target.checked; flag.version += 1; audit("FEATURE_FLAG_CHANGED", flag.key, `enabled=${flag.enabled}`); save(); renderFlags(); toast("Feature flag updated."); });

  byId("audit-search").addEventListener("input", renderAudit);
  function renderAudit() {
    const query = byId("audit-search").value.trim().toLowerCase(); const records = state.audit.filter((record) => !query || `${record.action} ${record.entity}`.toLowerCase().includes(query));
    byId("audit-body").innerHTML = records.length ? records.map((record) => `<tr><td>${new Date(record.at).toLocaleString()}</td><td>${escapeHtml(record.actor)}</td><td>${escapeHtml(record.action)}</td><td>${escapeHtml(record.entity)}</td><td>${escapeHtml(record.details)}</td></tr>`).join("") : '<tr><td colspan="5" class="empty">No audit records match.</td></tr>';
  }

  if (state.session) openDashboard();
})();
