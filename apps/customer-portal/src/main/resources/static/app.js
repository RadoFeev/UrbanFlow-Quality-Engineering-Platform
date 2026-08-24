(() => {
  "use strict";

  const STORE = "urbanflow.customer.v1";
  const state = load();
  const byId = (id) => document.getElementById(id);
  const testId = (id) => document.querySelector(`[data-testid="${id}"]`);

  function load() {
    try {
      return Object.assign(
        { users: [], session: null, addresses: [], shipments: [], incidents: [], language: "en" },
        JSON.parse(localStorage.getItem(STORE) || "{}")
      );
    } catch {
      return { users: [], session: null, addresses: [], shipments: [], incidents: [], language: "en" };
    }
  }

  function save() {
    localStorage.setItem(STORE, JSON.stringify(state));
  }

  async function digest(value) {
    const bytes = new TextEncoder().encode(value);
    const hash = await crypto.subtle.digest("SHA-256", bytes);
    return Array.from(new Uint8Array(hash), (byte) => byte.toString(16).padStart(2, "0")).join("");
  }

  function escapeHtml(value) {
    return String(value).replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[character]);
  }

  function message(element, text, success = false) {
    element.textContent = text;
    element.classList.toggle("success", success);
  }

  function toast(text) {
    const element = byId("toast");
    element.textContent = text;
    element.classList.add("show");
    window.setTimeout(() => element.classList.remove("show"), 2400);
  }

  function sessionValid() {
    if (!state.session) return false;
    if (Date.now() - state.session.touchedAt > 30 * 60 * 1000) {
      state.session = null;
      save();
      toast("Your session expired. Sign in again.");
      return false;
    }
    state.session.touchedAt = Date.now();
    save();
    return true;
  }

  function currentUser() {
    return state.session ? state.users.find((user) => user.id === state.session.userId) : null;
  }

  function showView(id) {
    const target = byId(id);
    if (!target) return;
    const protectedView = document.querySelector(`[data-view="${id}"][data-protected="true"]`);
    if (protectedView && !sessionValid()) {
      id = "auth";
      message(byId("login-message"), "Sign in to continue.");
    }
    document.querySelectorAll(".view").forEach((view) => view.classList.toggle("active", view.id === id));
    document.querySelectorAll("[data-view]").forEach((button) => button.classList.toggle("active", button.dataset.view === id));
    if (id === "shipments") renderShipments();
    if (id === "profile") renderProfile();
    if (id === "support") renderIncidentOptions();
    history.replaceState(null, "", `#${id}`);
    byId("main-content").focus();
  }

  function updateSessionControls() {
    byId("logout").classList.toggle("hidden", !state.session);
  }

  document.querySelectorAll("[data-view]").forEach((button) => button.addEventListener("click", () => showView(button.dataset.view)));
  document.querySelectorAll("[data-protected]").forEach((button) => button.addEventListener("click", updateSessionControls));

  byId("theme-toggle").addEventListener("click", () => {
    const dark = document.documentElement.dataset.theme !== "dark";
    document.documentElement.dataset.theme = dark ? "dark" : "light";
    byId("theme-toggle").textContent = dark ? "Light mode" : "Dark mode";
    byId("theme-toggle").setAttribute("aria-pressed", String(dark));
  });

  byId("language").value = state.language;
  byId("language").addEventListener("change", (event) => {
    state.language = event.target.value;
    document.documentElement.lang = state.language;
    save();
    toast(state.language === "bg" ? "Езикът е променен." : "Language updated.");
  });

  byId("registration-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = byId("register-name").value.trim();
    const email = byId("register-email").value.trim().toLowerCase();
    const password = byId("register-password").value;
    const output = byId("registration-message");
    if (!name || !/^\S+@\S+\.\S+$/.test(email)) return message(output, "Enter a valid name and email.");
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{12,}$/.test(password)) return message(output, "Password does not meet the security policy.");
    if (state.users.some((user) => user.email === email)) return message(output, "An account with this email already exists.");
    state.users.push({ id: crypto.randomUUID(), name, email, passwordHash: await digest(password), failures: 0, locked: false, role: "CUSTOMER" });
    save();
    event.target.reset();
    message(output, "Account created. You can sign in now.", true);
  });

  byId("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = byId("login-email").value.trim().toLowerCase();
    const password = byId("login-password").value;
    const output = byId("login-message");
    if (!email || !password) return message(output, "Email and password are required.");
    const user = state.users.find((candidate) => candidate.email === email);
    if (!user || user.passwordHash !== await digest(password)) {
      if (user) {
        user.failures += 1;
        user.locked = user.failures >= 5;
        save();
      }
      return message(output, "The email or password is incorrect.");
    }
    if (user.locked) return message(output, "This account is temporarily locked.");
    user.failures = 0;
    state.session = { userId: user.id, touchedAt: Date.now(), role: user.role };
    save();
    updateSessionControls();
    message(output, "Signed in successfully.", true);
    showView("create");
  });

  byId("logout").addEventListener("click", () => {
    state.session = null;
    save();
    updateSessionControls();
    showView("auth");
    toast("Signed out.");
  });

  byId("reset-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const email = byId("reset-email").value.trim();
    if (!/^\S+@\S+\.\S+$/.test(email)) return message(byId("reset-message"), "Enter a valid email.");
    message(byId("reset-message"), "If the account exists, reset instructions have been sent.", true);
  });

  const deliveryDays = { STANDARD: 3, EXPRESS: 1, SAME_DAY: 0 };
  testId("delivery-type").addEventListener("change", updateEstimate);
  function updateEstimate() {
    const type = testId("delivery-type").value;
    const date = new Date();
    date.setDate(date.getDate() + deliveryDays[type]);
    byId("estimate").textContent = `Estimated delivery: ${date.toLocaleDateString(state.language)}`;
  }
  updateEstimate();

  let submitting = false;
  byId("shipment-form").addEventListener("submit", (event) => {
    event.preventDefault();
    if (submitting) return;
    const pickup = testId("pickup-address").value.trim();
    const delivery = testId("delivery-address").value.trim();
    const phone = testId("recipient-phone").value.trim();
    const weight = Number(testId("parcel-weight").value);
    const dimensions = ["parcel-length", "parcel-width", "parcel-height"].map((id) => Number(testId(id).value));
    const description = testId("parcel-description").value.trim();
    const output = byId("shipment-message");
    if (!pickup || !delivery) return message(output, "Pickup and delivery addresses are required.");
    if (!/^\+?[1-9]\d{7,14}$/.test(phone)) return message(output, "Enter a valid international phone number.");
    if (!(weight >= 0.1 && weight <= 50)) return message(output, "Weight must be between 0.1 and 50 kg.");
    if (dimensions.some((value) => !(value >= 1 && value <= 200))) return message(output, "Every dimension must be between 1 and 200 cm.");
    if (!description) return message(output, "Contents description is required.");
    submitting = true;
    const button = testId("create-shipment-submit");
    button.disabled = true;
    const id = crypto.randomUUID();
    const trackingNumber = `UF-${id.replaceAll("-", "").slice(0, 8).toUpperCase()}`;
    const shipment = {
      id, ownerId: state.session.userId, trackingNumber, pickup, delivery, phone, weight,
      dimensions, description, type: testId("delivery-type").value, status: "CREATED", version: 0,
      createdAt: new Date().toISOString(), history: [{ status: "CREATED", at: new Date().toISOString() }]
    };
    state.shipments.push(shipment);
    save();
    event.target.reset();
    updateEstimate();
    message(output, `Shipment ${trackingNumber} was created.`, true);
    submitting = false;
    button.disabled = false;
  });

  byId("shipment-filter").addEventListener("change", renderShipments);
  function ownShipments() {
    const user = currentUser();
    return user ? state.shipments.filter((shipment) => shipment.ownerId === user.id) : [];
  }

  function renderShipments() {
    const filter = byId("shipment-filter").value;
    const shipments = ownShipments().filter((shipment) => filter === "ALL" || shipment.status === filter);
    const list = byId("shipment-list");
    if (!shipments.length) {
      list.innerHTML = '<p class="empty">No shipments match this view.</p>';
      return;
    }
    list.innerHTML = shipments.map((shipment) => `<article class="shipment-card" data-testid="shipment-card" data-id="${shipment.id}">
      <header><div><small>${new Date(shipment.createdAt).toLocaleString(state.language)}</small><h2>${escapeHtml(shipment.trackingNumber)}</h2></div><span class="status-pill">${escapeHtml(shipment.status)}</span></header>
      <dl><div><dt>From</dt><dd>${escapeHtml(shipment.pickup)}</dd></div><div><dt>To</dt><dd>${escapeHtml(shipment.delivery)}</dd></div><div><dt>Service</dt><dd>${escapeHtml(shipment.type)}</dd></div></dl>
      <div class="actions"><button class="ghost" data-action="track" type="button">View timeline</button><button class="ghost" data-action="change-address" type="button" ${!["CREATED", "CONFIRMED"].includes(shipment.status) ? "disabled" : ""}>Change address</button><button class="ghost" data-action="cancel" type="button" ${!["CREATED", "CONFIRMED"].includes(shipment.status) ? "disabled" : ""}>Cancel</button></div>
    </article>`).join("");
  }

  byId("shipment-list").addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    const shipment = state.shipments.find((item) => item.id === button.closest("[data-id]").dataset.id);
    if (button.dataset.action === "track") {
      byId("tracking-number").value = shipment.trackingNumber;
      showTracking(shipment);
      showView("tracking");
    } else if (button.dataset.action === "cancel") {
      shipment.status = "CANCELLED";
      shipment.version += 1;
      shipment.history.push({ status: "CANCELLED", at: new Date().toISOString() });
      save(); renderShipments(); toast("Shipment cancelled.");
    } else {
      const address = window.prompt("New delivery address", shipment.delivery);
      if (address && address.trim()) {
        shipment.delivery = address.trim().slice(0, 200);
        shipment.version += 1;
        save(); renderShipments(); toast("Delivery address updated.");
      }
    }
  });

  byId("tracking-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const trackingNumber = byId("tracking-number").value.trim().toUpperCase();
    if (!/^UF-[A-Z0-9]{8}$/.test(trackingNumber)) return message(byId("tracking-message"), "Tracking number format is invalid.");
    const shipment = state.shipments.find((item) => item.trackingNumber === trackingNumber);
    if (!shipment) return message(byId("tracking-message"), "No shipment was found for this tracking number.");
    message(byId("tracking-message"), `Current status: ${shipment.status}`, true);
    showTracking(shipment);
  });

  function showTracking(shipment) {
    byId("tracking-result").innerHTML = shipment.history.map((entry) => `<article><strong>${escapeHtml(entry.status.replaceAll("_", " "))}</strong><div>${new Date(entry.at).toLocaleString(state.language)}</div></article>`).join("");
  }

  function renderProfile() {
    const user = currentUser();
    if (!user) return;
    byId("profile-name").value = user.name;
    byId("profile-email").value = user.email;
    const addresses = state.addresses.filter((address) => address.ownerId === user.id);
    byId("address-list").innerHTML = addresses.length ? addresses.map((address) => `<li><span><strong>${escapeHtml(address.label)}</strong><br>${escapeHtml(address.value)}</span><button class="ghost" data-address-id="${address.id}" type="button">Remove</button></li>`).join("") : '<li class="empty">No saved addresses.</li>';
  }

  byId("profile-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const name = byId("profile-name").value.trim();
    if (!name) return message(byId("profile-message"), "Name is required.");
    currentUser().name = name;
    save();
    message(byId("profile-message"), "Profile updated.", true);
  });

  byId("address-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const label = testId("address-label").value.trim();
    const value = testId("address-value").value.trim();
    if (!label || !value) return;
    state.addresses.push({ id: crypto.randomUUID(), ownerId: state.session.userId, label, value });
    save(); event.target.reset(); renderProfile();
  });

  byId("address-list").addEventListener("click", (event) => {
    const button = event.target.closest("[data-address-id]");
    if (!button) return;
    state.addresses = state.addresses.filter((address) => address.id !== button.dataset.addressId);
    save(); renderProfile();
  });

  function renderIncidentOptions() {
    const shipments = ownShipments();
    byId("incident-shipment").innerHTML = shipments.map((shipment) => `<option value="${shipment.id}">${escapeHtml(shipment.trackingNumber)}</option>`).join("");
  }

  byId("incident-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const shipmentId = byId("incident-shipment").value;
    const description = testId("incident-description").value.trim();
    if (!shipmentId || !description) return message(byId("incident-message"), "Select a shipment and describe the issue.");
    state.incidents.push({ id: crypto.randomUUID(), ownerId: state.session.userId, shipmentId, type: testId("incident-type").value, description, status: "OPEN" });
    save(); event.target.reset(); message(byId("incident-message"), "Your report was submitted.", true);
  });

  updateSessionControls();
  showView(location.hash.slice(1) || (state.session ? "shipments" : "auth"));
})();
