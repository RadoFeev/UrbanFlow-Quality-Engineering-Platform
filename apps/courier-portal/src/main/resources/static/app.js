(() => {
  "use strict";
  const STORE = "urbanflow.courier.v1";
  const transitions = {
    COURIER_ASSIGNED: ["PICKUP_IN_PROGRESS"],
    PICKUP_IN_PROGRESS: ["PICKED_UP"],
    PICKED_UP: ["AT_ORIGIN_HUB"],
    AT_ORIGIN_HUB: ["OUT_FOR_DELIVERY"],
    OUT_FOR_DELIVERY: ["DELIVERED", "DELIVERY_ATTEMPTED"],
    DELIVERY_ATTEMPTED: ["OUT_FOR_DELIVERY"],
    DELIVERED: []
  };
  const state = load();
  const byId = (id) => document.getElementById(id);

  function load() {
    try {
      return Object.assign({ session: null, routeStarted: false, offline: false, assignments: [], queue: [] }, JSON.parse(localStorage.getItem(STORE) || "{}"));
    } catch {
      return { session: null, routeStarted: false, offline: false, assignments: [], queue: [] };
    }
  }
  function save() { localStorage.setItem(STORE, JSON.stringify(state)); }
  function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[character]); }
  function message(element, text, ok = false) { element.textContent = text; element.classList.toggle("ok", ok); }
  function toast(text) { const element = byId("toast"); element.textContent = text; element.classList.add("show"); window.setTimeout(() => element.classList.remove("show"), 2200); }

  function seedAssignments(courierId) {
    if (state.assignments.length) return;
    state.assignments = [
      ["UF-A1B2C3D4", "Hub North · Dock 3", "Zone 12 · Locker 8", "EXPRESS"],
      ["UF-E5F6G7H8", "Hub North · Dock 1", "Zone 4 · Service desk", "STANDARD"],
      ["UF-J9K0L1M2", "Partner point · Central", "Zone 9 · Parcel station", "STANDARD"]
    ].map((entry, index) => ({ id: crypto.randomUUID(), courierId, stop: index + 1, tracking: entry[0], pickup: entry[1], delivery: entry[2], service: entry[3], status: "COURIER_ASSIGNED", queued: false, history: [] }));
  }

  byId("login-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const courierId = byId("courier-id").value.trim();
    const accessCode = byId("access-code").value;
    if (!/^\d{6}$/.test(courierId) || accessCode.length < 8) return message(byId("login-message"), "Enter a six-digit courier ID and a valid shift code.");
    state.session = { courierId, startedAt: Date.now() };
    seedAssignments(courierId);
    save();
    openWorkspace();
  });

  byId("logout").addEventListener("click", () => {
    state.session = null;
    save();
    byId("workspace").classList.add("hidden");
    byId("login-panel").classList.remove("hidden");
  });

  function openWorkspace() {
    byId("login-panel").classList.add("hidden");
    byId("workspace").classList.remove("hidden");
    byId("route-code").textContent = `R-${state.session.courierId}-${new Date().toISOString().slice(5, 10).replace("-", "")}`;
    updateRoute();
    updateNetwork();
    render();
  }

  byId("route-toggle").addEventListener("click", () => {
    if (!state.routeStarted) {
      state.routeStarted = true;
      toast("Route started.");
    } else if (state.assignments.some((assignment) => assignment.status !== "DELIVERED")) {
      return toast("Complete all assignments before ending the route.");
    } else {
      state.routeStarted = false;
      toast("Route completed.");
    }
    save(); updateRoute(); render();
  });

  function updateRoute() {
    byId("route-toggle").textContent = state.routeStarted ? "Complete route" : "Start route";
    byId("route-toggle").dataset.testid = state.routeStarted ? "complete-route" : "start-route";
    const delivered = state.assignments.filter((assignment) => assignment.status === "DELIVERED").length;
    byId("route-summary").textContent = `${delivered} of ${state.assignments.length} stops completed.`;
  }

  byId("offline-toggle").checked = state.offline;
  byId("offline-toggle").addEventListener("change", (event) => {
    state.offline = event.target.checked;
    save(); updateNetwork();
    if (!state.offline) syncQueue();
  });

  function updateNetwork() {
    document.querySelector(".network").classList.toggle("offline", state.offline);
    byId("network-label").textContent = state.offline ? "Offline" : "Online";
    byId("queue-count").textContent = `${state.queue.length} queued`;
    byId("sync").disabled = state.offline || !state.queue.length;
  }

  byId("sync").addEventListener("click", syncQueue);
  function syncQueue() {
    if (state.offline) return toast("Reconnect before synchronizing.");
    const count = state.queue.length;
    state.queue = [];
    state.assignments.forEach((assignment) => { assignment.queued = false; });
    save(); updateNetwork(); render();
    toast(count ? `${count} updates synchronized.` : "Everything is synchronized.");
  }

  byId("status-filter").addEventListener("change", render);
  function render() {
    const filter = byId("status-filter").value;
    const items = state.assignments.filter((assignment) => filter === "ALL" || assignment.status === filter);
    byId("visible-count").textContent = `${items.length} ${items.length === 1 ? "stop" : "stops"}`;
    byId("assignments").innerHTML = items.length ? items.map((assignment) => `<article class="assignment" data-id="${assignment.id}" data-testid="assignment-card">
      <div class="stop">${assignment.stop}</div><div><h2>${escapeHtml(assignment.tracking)}</h2><p><strong>${escapeHtml(assignment.pickup)}</strong> → ${escapeHtml(assignment.delivery)}</p><div class="assignment-meta"><span class="pill">${escapeHtml(assignment.service)}</span><span class="pill pending">${escapeHtml(assignment.status.replaceAll("_", " "))}</span>${assignment.queued ? '<span class="pill">Queued offline</span>' : ""}</div></div>
      <button class="primary" data-action="update" type="button" ${!state.routeStarted || !transitions[assignment.status].length ? "disabled" : ""}>Update status</button></article>`).join("") : '<p class="empty">No assignments match this filter.</p>';
    updateRoute(); updateNetwork();
  }

  byId("assignments").addEventListener("click", (event) => {
    const button = event.target.closest("[data-action='update']");
    if (!button) return;
    const assignment = state.assignments.find((item) => item.id === button.closest("[data-id]").dataset.id);
    byId("dialog-assignment-id").value = assignment.id;
    byId("dialog-title").textContent = assignment.tracking;
    byId("next-status").innerHTML = transitions[assignment.status].map((status) => `<option>${status}</option>`).join("");
    byId("update-message").textContent = "";
    byId("failure-reason").value = "";
    byId("delivery-proof").value = "";
    updateConditionalFields();
    byId("update-dialog").showModal();
  });

  byId("next-status").addEventListener("change", updateConditionalFields);
  function updateConditionalFields() {
    const next = byId("next-status").value;
    byId("reason-field").classList.toggle("hidden", next !== "DELIVERY_ATTEMPTED");
    byId("proof-field").classList.toggle("hidden", next !== "DELIVERED");
  }

  byId("update-form").addEventListener("submit", (event) => {
    event.preventDefault();
    if (event.submitter?.value === "cancel") return byId("update-dialog").close();
    const assignment = state.assignments.find((item) => item.id === byId("dialog-assignment-id").value);
    const next = byId("next-status").value;
    if (!transitions[assignment.status].includes(next)) return message(byId("update-message"), "This status transition is no longer valid.");
    const reason = byId("failure-reason").value.trim();
    if (next === "DELIVERY_ATTEMPTED" && !reason) return message(byId("update-message"), "A reason is required for an unsuccessful attempt.");
    const proof = byId("delivery-proof").files[0];
    if (next === "DELIVERED" && !proof) return message(byId("update-message"), "Delivery proof is required.");
    if (proof && (!['image/jpeg', 'image/png', 'application/pdf'].includes(proof.type) || proof.size > 5 * 1024 * 1024)) return message(byId("update-message"), "Proof must be a JPEG, PNG, or PDF up to 5 MB.");
    assignment.status = next;
    assignment.history.push({ status: next, at: new Date().toISOString(), reason: reason || null, proofName: proof?.name || null });
    if (state.offline) {
      assignment.queued = true;
      state.queue.push({ assignmentId: assignment.id, status: next, createdAt: new Date().toISOString() });
    }
    save();
    byId("update-dialog").close();
    render();
    toast(state.offline ? "Update saved to the offline queue." : "Delivery status updated.");
  });

  if (state.session) openWorkspace();
})();
