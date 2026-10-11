"use strict";

/* Roads and reports are loaded from the Java API; maintenance teams remain demo data. */

const ROAD_API_URL = "http://127.0.0.1:8080/api/roads";
const ROUTE_API_URL = "http://127.0.0.1:8080/api/routes";
const REPORT_API_URL = "http://127.0.0.1:8080/api/reports";
const LOCATION_API_URL = "http://127.0.0.1:8080/api/locations";

const users = {
    ward: {
        username: "ward01",
        password: "ward123",
        name: "Ward Officer",
        initials: "WO"
    },
    engineering: {
        username: "engineer01",
        password: "eng123",
        name: "Engineering Section",
        initials: "ES"
    },
    team: {
        username: "team01",
        password: "team123",
        name: "Maintenance Team T1",
        initials: "T1"
    }
};

let selectedRole = "ward";
let currentUser = null;
let roads = [];
const reports = [];
let roadLoadError = "";
let reportLoadError = "";

const defaultTeams = [
    {
        id: "T1",
        location: "Hospital",
        available: true,
        equipment: ["Pothole Kit", "Road Cutter"]
    },
    {
        id: "T2",
        location: "Market",
        available: false,
        equipment: ["Resurfacing Machine"]
    },
    {
        id: "T3",
        location: "Ground",
        available: true,
        equipment: ["Road Cutter", "Pothole Kit"]
    }
];

function loadTeams() {
    try {
        const savedTeams = JSON.parse(localStorage.getItem("roadnexusTeams"));

        if (
            Array.isArray(savedTeams) &&
            savedTeams.every(function (team) {
                return (
                    team &&
                    typeof team.id === "string" &&
                    typeof team.location === "string" &&
                    typeof team.available === "boolean" &&
                    Array.isArray(team.equipment) &&
                    team.equipment.every(item => typeof item === "string")
                );
            })
        ) {
            return savedTeams;
        }
    } catch (error) {
        // Use the sample teams when browser storage is unavailable or invalid.
    }

    return defaultTeams;
}

const teams = loadTeams();

function saveTeams() {
    try {
        localStorage.setItem("roadnexusTeams", JSON.stringify(teams));
        return true;
    } catch (error) {
        return false;
    }
}

let locations = {};

const pageTitles = {
    dashboard: "Dashboard",
    roads: "Road Network",
    reports: "Damage Reports",
    teams: "Maintenance Teams",
    jobs: "My Repair Jobs",
    routes: "Fastest Route"
};

function getElement(id) {
    return document.getElementById(id);
}

/* Prevent sample text from being interpreted as HTML. */
function escapeHTML(value) {
    return String(value).replace(/[&<>"']/g, function (character) {
        const replacements = {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        };

        return replacements[character];
    });
}

function findRoad(roadId) {
    return roads.find(function (road) {
        return road.id === roadId;
    });
}

/* Create a colored status label. */
function statusBadge(status) {
    const classes = {
        Open: "status-open",
        Closed: "status-closed",
        Pending: "status-pending",
        Completed: "status-completed",
        Available: "status-available",
        Unavailable: "status-unavailable",
        Critical: "status-critical",
        High: "status-high",
        Medium: "status-medium",
        Low: "status-low",
        Active: "status-active",
        Waiting: "status-waiting"
    };

    const className = classes[status] || "status-waiting";

    return `<span class="status ${className}">${escapeHTML(status)}</span>`;
}

/* ---------------------------------
   ROLE SELECTION AND LOGIN
---------------------------------- */

document.querySelectorAll(".role").forEach(function (button) {
    button.addEventListener("click", function () {
        selectedRole = button.dataset.role;

        document.querySelectorAll(".role").forEach(function (roleButton) {
            roleButton.classList.toggle(
                "selected",
                roleButton === button
            );
        });

        getElement("loginError").textContent = "";
    });
});

getElement("loginForm").addEventListener("submit", function (event) {
    event.preventDefault();

    const username = getElement("username").value.trim();
    const password = getElement("password").value;
    const account = users[selectedRole];

    if (
        username !== account.username ||
        password !== account.password
    ) {
        getElement("loginError").textContent =
            "Incorrect demo login. Check the selected role and credentials.";
        return;
    }

    currentUser = {
        ...account,
        role: selectedRole
    };

    startApplication();
});

function startApplication() {
    getElement("loginScreen").classList.add("hidden");
    getElement("application").classList.remove("hidden");

    getElement("sideAvatar").textContent = currentUser.initials;
    getElement("topAvatar").textContent = currentUser.initials;

    getElement("sideName").textContent = currentUser.name;
    getElement("topName").textContent = currentUser.name;

    const roleNames = {
        ward: "Ward Officer",
        engineering: "Engineering Section",
        team: "Maintenance Team"
    };

    getElement("sideRole").textContent =
        roleNames[currentUser.role] + " Access";

    getElement("topRole").textContent =
        roleNames[currentUser.role];

    const welcomeMessages = {
        ward: "Submit road damage reports and track reported issues.",
        engineering: "Monitor roads, coordinate repair teams and plan routes.",
        team: "Review your repair assignments and plan routes to job locations."
    };

    getElement("welcomeTitle").textContent =
        "Welcome, " + currentUser.name;

    getElement("welcomeText").textContent =
        welcomeMessages[currentUser.role];

    /*
     * Show only navigation items and buttons allowed
     * for the currently selected demo role.
     */
    document.querySelectorAll("[data-roles]").forEach(function (element) {
        const permittedRoles = element.dataset.roles.split(",");

        element.classList.toggle(
            "hidden",
            !permittedRoles.includes(currentUser.role)
        );
    });

    showPage("dashboard");
    renderEverything();
    loadRoads();
    loadReports();
}

/* ---------------------------------
   LOGOUT
---------------------------------- */

getElement("logoutButton").addEventListener("click", function () {
    currentUser = null;
    currentRoutePath = [];

    getElement("application").classList.add("hidden");
    getElement("loginScreen").classList.remove("hidden");

    getElement("loginForm").reset();
    getElement("loginError").textContent = "";

    selectedRole = "ward";

    document.querySelectorAll(".role").forEach(function (button) {
        button.classList.toggle(
            "selected",
            button.dataset.role === "ward"
        );
    });
});

/* ---------------------------------
   PAGE NAVIGATION
---------------------------------- */

function showPage(pageName) {
    if (!pageTitles[pageName]) {
        return;
    }

    const navigationButton = document.querySelector(
        `#navigation [data-page="${pageName}"]`
    );

    /*
     * Do not allow a hidden navigation page
     * to be opened through the menu.
     */
    if (
        pageName !== "dashboard" &&
        (!navigationButton || navigationButton.classList.contains("hidden"))
    ) {
        pageName = "dashboard";
    }

    document.querySelectorAll(".page").forEach(function (page) {
        page.classList.remove("active-page");
    });

    getElement(pageName + "Page").classList.add("active-page");

    document.querySelectorAll(".nav-link").forEach(function (button) {
        button.classList.toggle(
            "active",
            button.dataset.page === pageName
        );
    });

    getElement("pageTitle").textContent = pageTitles[pageName];

    if (pageName === "roads") loadRoads();
    if (pageName === "reports") loadReports();
    if (pageName === "teams") renderTeams();
    if (pageName === "jobs") renderJobs();

    if (pageName === "dashboard") {
        if (roadMapInstance) {
            roadMapInstance.invalidateSize();
        }
        renderRoadMap();
    }

    if (pageName === "routes") {
        if (routeMapInstance) {
            routeMapInstance.invalidateSize();
        }
        renderRouteMap();
    }
}

document.querySelectorAll(".nav-link").forEach(function (button) {
    button.addEventListener("click", function () {
        showPage(button.dataset.page);
    });
});

document.querySelectorAll("[data-go]").forEach(function (button) {
    button.addEventListener("click", function () {
        showPage(button.dataset.go);
    });
});

/* ---------------------------------
   DASHBOARD STATISTICS
---------------------------------- */

function renderStatistics() {
    getElement("totalRoads").textContent = roads.length;

    getElement("openRoads").textContent =
        roads.filter(function (road) {
            return !road.closed;
        }).length;

    getElement("pendingReports").textContent =
        reports.filter(function (report) {
            return report.status === "Pending";
        }).length;

    getElement("availableTeams").textContent =
        teams.filter(function (team) {
            return team.available;
        }).length;

    const summary = [
        ["Registered road segments", roads.length],
        ["Open road segments", roads.filter(r => !r.closed).length],
        ["Closed road segments", roads.filter(r => r.closed).length],
        [
            "Available teams",
            teams.filter(t => t.available).length + " / " + teams.length
        ]
    ];

    getElement("networkSummary").innerHTML = summary.map(function (item) {
        return `
            <div class="summary-item">
                <span>${escapeHTML(item[0])}</span>
                <strong>${escapeHTML(item[1])}</strong>
            </div>
        `;
    }).join("");
}

/* ---------------------------------
   ROAD NETWORK MAP (coordinates come from the backend)
---------------------------------- */

/* locations of the last calculated fastest route, used to highlight the map. */
let currentRoutePath = [];

function getLocationNames() {
    const names = Object.keys(locations);
    if (names.length > 0) {
        return names.sort();
    }
    return [...new Set(roads.flatMap(function (road) {
        return [road.from, road.to];
    }))].sort();
}

/* The only source of [lat, lng]: coordinates stored in the database and
 * served by the API. The browser never invents a position. */
function getLocationCoords(name) {
    if (!name || typeof name !== "string") {
        return null;
    }
    const location = locations[name];
    if (location && Number.isFinite(location.lat) && Number.isFinite(location.lng)) {
        return [location.lat, location.lng];
    }
    return null;
}

/* Prefer the coordinates the backend attached to the segment itself,
 * then fall back to the location register served by the API. */
function roadEndpointCoords(road, key) {
    const lat = road[key + "Lat"];
    const lng = road[key + "Lng"];
    if (Number.isFinite(Number(lat)) && Number.isFinite(Number(lng))) {
        return [Number(lat), Number(lng)];
    }
    return getLocationCoords(road[key]);
}

function locationPairKey(from, to) {
    return [from, to].sort().join("→");
}

/* ---------------------------------
   LEAFLET MAP (coordinates from the backend)
---------------------------------- */

/* The map is centred on the municipal area; every marker and line gets its
 * [lat, lng] from the locations stored in the database and served by the
 * API, never from layout math in the browser. */
const NETWORK_CENTER = [13.0827, 80.2707];

let roadMapInstance = null;
let routeMapInstance = null;
let roadMapLayer = null;
let routeMapLayer = null;

function createMap(elementId) {
    const element = getElement(elementId);

    if (!element || typeof L === "undefined") {
        return null;
    }

    const map = L.map(element, {
        center: NETWORK_CENTER,
        zoom: 15
    });

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    return map;
}

function ensureMap(which) {
    if (which === "routes") {
        if (!routeMapInstance) {
            routeMapInstance = createMap("routeMap");
        }
        return routeMapInstance;
    }

    if (!roadMapInstance) {
        roadMapInstance = createMap("roadMap");
    }
    return roadMapInstance;
}

/* Build Leaflet layers: road segments, route highlight and location markers. */
function buildNetworkLayer(routeNames) {
    const layer = L.featureGroup();
    const route = routeNames || [];
    const routeEdges = new Set();
    const routeNodes = new Set(route);

    for (let i = 0; i + 1 < route.length; i++) {
        routeEdges.add(locationPairKey(route[i], route[i + 1]));
    }

    roads.forEach(function (road) {
        const a = roadEndpointCoords(road, "from");
        const b = roadEndpointCoords(road, "to");

        /* A segment can only be drawn when the backend supplied both ends. */
        if (!a || !b) {
            return;
        }

        const latlngs = [a, b];
        const tooltip = `${road.id} · ${road.from} → ${road.to} · ${road.time} min${road.closed ? " · CLOSED" : ""}`;
        const segment = L.polyline(latlngs, {
            color: road.closed ? "#d94d57" : "#397be9",
            weight: road.closed ? 4 : 5,
            opacity: 0.9,
            dashArray: road.closed ? "8 8" : null,
            lineCap: "round"
        });

        segment.bindTooltip(tooltip, { sticky: true });
        layer.addLayer(segment);

        if (routeEdges.has(locationPairKey(road.from, road.to))) {
            layer.addLayer(L.polyline(latlngs, {
                color: "#f5a623",
                weight: 7,
                opacity: 0.9,
                dashArray: "12 9",
                lineCap: "round"
            }));
        }
    });

    getLocationNames().forEach(function (name) {
        const point = getLocationCoords(name);

        if (!point) {
            return;
        }

        const isStart = route.length > 0 && route[0] === name;
        const isEnd = route.length > 0 && route[route.length - 1] === name;
        const onRoute = routeNodes.has(name);

        const accent = isStart ? "#2f9e44"
            : isEnd ? "#1971c2"
            : onRoute ? "#f5a623"
            : "#397be9";

        const marker = L.circleMarker(point, {
            radius: onRoute ? 9 : 6,
            color: accent,
            weight: 3,
            fillColor: isStart || isEnd ? accent : onRoute ? "#fff4e0" : "#ffffff",
            fillOpacity: 1
        });

        marker.bindTooltip(name, { direction: "top", offset: [0, -6] });
        layer.addLayer(marker);
    });

    return layer;
}

function fitMapToNetwork(map, layer) {
    const key = getLocationNames().join("|") + "|roads:" + roads.length;
    const size = map.getSize();

    /* Skip while the panel is hidden (zero size); fit once it is visible. */
    if (map.__fittedKey === key || !size || size.x === 0) {
        return;
    }

    const bounds = layer.getBounds();

    if (bounds && bounds.isValid()) {
        map.fitBounds(bounds, { padding: [30, 30] });
        map.__fittedKey = key;
    }
}

function renderLeafletMap(which) {
    const map = ensureMap(which);

    if (!map) {
        return;
    }

    const previous = which === "routes" ? routeMapLayer : roadMapLayer;

    if (previous) {
        map.removeLayer(previous);
    }

    const layer = buildNetworkLayer(currentRoutePath);
    layer.addTo(map);

    if (which === "routes") {
        routeMapLayer = layer;
    } else {
        roadMapLayer = layer;
    }

    fitMapToNetwork(map, layer);
}

function renderRoadMap() {
    renderLeafletMap("roads");
}

function renderRouteMap() {
    renderLeafletMap("routes");
}

/* ---------------------------------
   ROAD NETWORK TABLE
---------------------------------- */

function renderRoads(searchText = "") {
    const query = searchText.trim().toLowerCase();

    const filteredRoads = roads.filter(function (road) {
        return (
            road.id.toLowerCase().includes(query) ||
            road.from.toLowerCase().includes(query) ||
            road.to.toLowerCase().includes(query)
        );
    });

    if (filteredRoads.length === 0) {
        getElement("roadsTable").innerHTML =
            `<tr><td colspan="6">${roadLoadError
                ? "Road data is unavailable. Check the connection message above."
                : roads.length === 0
                    ? "No roads found in the database. Add roads using the Engineering console."
                    : "No matching roads found."}</td></tr>`;
        return;
    }

    getElement("roadsTable").innerHTML = filteredRoads.map(function (road) {
        const action = currentUser?.role === "engineering"
            ? `<button class="secondary" data-toggle-road="${escapeHTML(road.id)}">
                ${road.closed ? "Reopen" : "Close"}
            </button>`
            : "—";

        return `
            <tr>
                <td class="id-text">${escapeHTML(road.id)}</td>
                <td>${escapeHTML(road.from)}</td>
                <td>${escapeHTML(road.to)}</td>
                <td>${road.time} min</td>
                <td>${statusBadge(road.closed ? "Closed" : "Open")}</td>
                <td>${action}</td>
            </tr>
        `;
    }).join("");
}

getElement("roadSearch").addEventListener("input", function (event) {
    renderRoads(event.target.value);
});

getElement("refreshRoadsButton").addEventListener("click", function () {
    loadRoads();
});

getElement("roadForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    if (currentUser?.role !== "engineering") {
        return;
    }

    const form = event.currentTarget;
    const submitButton = form.querySelector("button[type=submit]");
    const message = getElement("roadFormMessage");
    submitButton.disabled = true;
    message.textContent = "Saving road to the database...";
    message.classList.remove("error");

    try {
        const response = await fetch(ROAD_API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({
                id: getElement("newRoadId").value.trim(),
                from: getElement("newRoadStart").value.trim(),
                to: getElement("newRoadEnd").value.trim(),
                time: getElement("newRoadTime").value
            })
        });
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Road API returned HTTP ${response.status}.`);
        }

        form.reset();
        await loadRoads();
        message.textContent = `${payload.id} added to the road database.`;
    } catch (error) {
        message.textContent = `Could not add road: ${error.message}`;
        message.classList.add("error");
    } finally {
        submitButton.disabled = false;
    }
});

async function updateRoadClosure(road) {
    const status = getElement("roadConnectionStatus");
    const closed = !road.closed;

    if (status) {
        status.textContent = `Saving ${road.id} status to the database...`;
        status.classList.remove("error");
    }

    try {
        const response = await fetch(
            `${ROAD_API_URL}/${encodeURIComponent(road.id)}/status?closed=${closed}`,
            { method: "PATCH" }
        );
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Road API returned HTTP ${response.status}.`);
        }

        road.closed = Boolean(payload.closed);
        if (status) {
            status.textContent = `Connected to database · ${roads.length} road(s) loaded · ${road.id} updated`;
        }
        renderEverything();
        renderRoads(getElement("roadSearch").value);
    } catch (error) {
        if (status) {
            status.textContent = `Could not save road status: ${error.message}`;
            status.classList.add("error");
        }
    }
}

getElement("roadsTable").addEventListener("click", function (event) {
    const button = event.target.closest("[data-toggle-road]");

    if (!button || currentUser?.role !== "engineering") {
        return;
    }

    const selectedRoad = findRoad(button.dataset.toggleRoad);

    if (!selectedRoad) {
        return;
    }

    updateRoadClosure(selectedRoad);
});

/* ---------------------------------
   DAMAGE REPORTS
---------------------------------- */

function renderRecentReports() {
    const recent = [...reports].reverse().slice(0, 5);

    if (recent.length === 0) {
        getElement("recentReports").innerHTML =
            `<tr><td colspan="5">No reports found.</td></tr>`;
        return;
    }

    getElement("recentReports").innerHTML = recent.map(function (report) {
        return `
            <tr>
                <td class="id-text">${escapeHTML(report.id)}</td>
                <td>${escapeHTML(report.roadId)}</td>
                <td>${escapeHTML(report.damage)}</td>
                <td>${statusBadge(report.severity)}</td>
                <td>${statusBadge(report.status)}</td>
            </tr>
        `;
    }).join("");
}

function renderReports() {
    const sortedReports = [...reports].reverse();

    if (sortedReports.length === 0) {
        getElement("reportsTable").innerHTML =
            `<tr><td colspan="8">${reportLoadError
                ? "Saved reports are unavailable. Check the database connection."
                : "No reports submitted yet."}</td></tr>`;
    } else {
        getElement("reportsTable").innerHTML = sortedReports.map(function (report) {
            let action = "View only";

            if (currentUser.role === "engineering") {
                action = `
                    <select class="action-select"
                        data-report-status="${escapeHTML(report.id)}">
                        <option value="Pending"
                            ${report.status === "Pending" ? "selected" : ""}>
                            Pending
                        </option>
                        <option value="Active"
                            ${report.status === "Active" ? "selected" : ""}>
                            Active
                        </option>
                        <option value="Completed"
                            ${report.status === "Completed" ? "selected" : ""}>
                            Completed
                        </option>
                    </select>
                `;
            }

            return `
                <tr>
                    <td class="id-text">${escapeHTML(report.id)}</td>
                    <td>${escapeHTML(report.roadId)}</td>
                    <td>${escapeHTML(report.damage)}</td>
                    <td>${statusBadge(report.severity)}</td>
                    <td>${statusBadge(report.risk)}</td>
                    <td>${statusBadge(report.status)}</td>
                    <td>${report.submittedAt
                        ? escapeHTML(new Date(report.submittedAt).toLocaleString())
                        : "—"}</td>
                    <td>${action}</td>
                </tr>
            `;
        }).join("");
    }

    const roadSelect = getElement("reportRoad");
    const previousValue = roadSelect.value;

    roadSelect.innerHTML = roads.map(function (road) {
        return `
            <option value="${escapeHTML(road.id)}">
                ${escapeHTML(road.id)} — ${escapeHTML(road.from)}
                to ${escapeHTML(road.to)}
            </option>
        `;
    }).join("");

    if (roads.some(road => road.id === previousValue)) {
        roadSelect.value = previousValue;
    }

    getElement("reportFormPanel").classList.toggle(
        "hidden",
        currentUser.role !== "ward"
    );
}

async function saveReportStatus(report, status) {
    const message = getElement("reportStatusMessage");
    message.textContent = `Saving ${report.id} status...`;
    message.classList.remove("error");

    try {
        const response = await fetch(
            `${REPORT_API_URL}/${encodeURIComponent(report.id)}/status?status=${encodeURIComponent(status)}`,
            { method: "PATCH" }
        );
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Report API returned HTTP ${response.status}.`);
        }

        const index = reports.findIndex(item => item.id === report.id);
        if (index !== -1) {
            reports[index] = mapReport(payload);
        }
        message.textContent = `${report.id} status saved.`;
        renderEverything();
    } catch (error) {
        message.textContent = `Could not save report status: ${error.message}`;
        message.classList.add("error");
        renderReports();
    }
}

getElement("reportsTable").addEventListener("change", function (event) {
    const reportId = event.target.dataset.reportStatus;

    if (!reportId || currentUser?.role !== "engineering") {
        return;
    }

    const report = reports.find(function (item) {
        return item.id === reportId;
    });

    if (!report) {
        return;
    }

    event.target.disabled = true;
    saveReportStatus(report, event.target.value);
});

getElement("reportForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    if (currentUser?.role !== "ward") {
        return;
    }

    const form = event.currentTarget;
    const submitButton = form.querySelector("button[type=submit]");
    const message = getElement("reportFormMessage");
    submitButton.disabled = true;
    message.textContent = "Saving report to the database...";

    try {
        const response = await fetch(REPORT_API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({
                roadId: getElement("reportRoad").value,
                damageType: getElement("damageType").value,
                severity: getElement("severity").value,
                risk: getElement("accidentRisk").value,
                importance: getElement("roadImportance").value,
                traffic: getElement("trafficRank").value
            })
        });
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Report API returned HTTP ${response.status}.`);
        }

        reports.push(mapReport(payload));
        form.reset();
        reportLoadError = "";
        message.textContent = `${payload.id} saved to the municipal database.`;
        renderEverything();
    } catch (error) {
        message.textContent = `Could not save report: ${error.message}`;
    } finally {
        submitButton.disabled = false;
    }
});

/* ---------------------------------
   MAINTENANCE TEAMS
---------------------------------- */

function renderTeams() {
    const previousLocation = getElement("newTeamLocation").value;
    const locationOptions = Object.keys(locations).map(function (location) {
        return `<option value="${escapeHTML(location)}">${escapeHTML(location)}</option>`;
    }).join("");

    getElement("newTeamLocation").innerHTML = locationOptions;
    if (Object.keys(locations).includes(previousLocation)) {
        getElement("newTeamLocation").value = previousLocation;
    }

    const equipmentTeam = getElement("equipmentTeam");
    const previousTeam = equipmentTeam.value;
    equipmentTeam.innerHTML = teams.map(function (team) {
        return `<option value="${escapeHTML(team.id)}">Team ${escapeHTML(team.id)}</option>`;
    }).join("");
    if (teams.some(team => team.id === previousTeam)) {
        equipmentTeam.value = previousTeam;
    }

    if (teams.length === 0) {
        getElement("teamsGrid").innerHTML =
            '<p class="empty-teams">No teams registered yet.</p>';
        return;
    }

    getElement("teamsGrid").innerHTML = teams.map(function (team) {
        const availability = team.available ? "Available" : "Unavailable";
        const equipmentHTML = team.equipment.length
            ? team.equipment.map(function (equipment, index) {
                return `
                    <li>
                        <span>${escapeHTML(equipment)}</span>
                        <button class="equipment-remove" type="button"
                            data-remove-team="${escapeHTML(team.id)}"
                            data-equipment-index="${index}"
                            aria-label="Remove ${escapeHTML(equipment)} from team ${escapeHTML(team.id)}">
                            Remove
                        </button>
                    </li>
                `;
            }).join("")
            : "<li class=\"empty-equipment\">No equipment assigned</li>";

        return `
            <article class="team-card">
                ${statusBadge(availability)}
                <h4>Maintenance Team ${escapeHTML(team.id)}</h4>
                <p>
                    <strong>Current location:</strong><br>
                    ${escapeHTML(team.location)}
                </p>
                <strong class="equipment-heading">Equipment</strong>
                <ul class="equipment-list">${equipmentHTML}</ul>
            </article>
        `;
    }).join("");
}

async function loadEquipmentOptions(damageType) {
    const equipmentSelect = getElement("equipmentName");
    const selectedValue = equipmentSelect.value;
    const query = damageType ? `?damageType=${encodeURIComponent(damageType)}` : "";

    equipmentSelect.innerHTML = '<option value="">Select equipment</option>';

    try {
        const response = await fetch(`http://127.0.0.1:8080/api/equipment${query}`, { cache: "no-store" });
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Equipment API returned HTTP ${response.status}.`);
        }

        equipmentSelect.innerHTML = '<option value="">Select equipment</option>' +
            payload.map(function (item) {
                return `<option value="${escapeHTML(item)}">${escapeHTML(item)}</option>`;
            }).join("");

        if (payload.includes(selectedValue)) {
            equipmentSelect.value = selectedValue;
        }
    } catch (error) {
        console.warn("Could not load equipment list:", error);
    }
}

getElement("equipmentDamageType").addEventListener("change", function () {
    loadEquipmentOptions(this.value);
});
loadEquipmentOptions(getElement("equipmentDamageType").value);

getElement("teamRegistrationForm").addEventListener("submit", function (event) {
    event.preventDefault();

    if (currentUser?.role !== "engineering") {
        return;
    }

    const teamId = getElement("newTeamId").value.trim().toUpperCase();
    const message = getElement("teamFormMessage");

    if (teams.some(team => team.id.toUpperCase() === teamId)) {
        message.textContent = `Team ${teamId} is already registered.`;
        message.classList.add("error");
        return;
    }

    teams.push({
        id: teamId,
        location: getElement("newTeamLocation").value,
        available: getElement("newTeamAvailability").value === "true",
        equipment: []
    });

    const saved = saveTeams();
    event.target.reset();
    message.textContent = saved
        ? `Team ${teamId} registered.`
        : `Team ${teamId} registered for this session. Browser storage is unavailable.`;
    message.classList.remove("error");
    renderEverything();
});

getElement("equipmentForm").addEventListener("submit", function (event) {
    event.preventDefault();

    if (currentUser?.role !== "engineering") {
        return;
    }

    const team = teams.find(item => item.id === getElement("equipmentTeam").value);
    const equipmentName = getElement("equipmentName").value.trim();
    const message = getElement("teamFormMessage");

    if (!team || !equipmentName) {
        return;
    }

    if (team.equipment.some(item => item.toLowerCase() === equipmentName.toLowerCase())) {
        message.textContent = `${equipmentName} is already assigned to team ${team.id}.`;
        message.classList.add("error");
        return;
    }

    team.equipment.push(equipmentName);
    const saved = saveTeams();
    event.target.reset();
    message.textContent = saved
        ? `${equipmentName} added to team ${team.id}.`
        : `${equipmentName} added for this session. Browser storage is unavailable.`;
    message.classList.remove("error");
    renderEverything();
});

getElement("teamsGrid").addEventListener("click", function (event) {
    const button = event.target.closest("[data-remove-team]");

    if (!button || currentUser?.role !== "engineering") {
        return;
    }

    const team = teams.find(item => item.id === button.dataset.removeTeam);
    const equipmentIndex = Number(button.dataset.equipmentIndex);

    if (!team || !Number.isInteger(equipmentIndex) || !team.equipment[equipmentIndex]) {
        return;
    }

    const [removedEquipment] = team.equipment.splice(equipmentIndex, 1);
    const saved = saveTeams();
    const message = getElement("teamFormMessage");
    message.textContent = saved
        ? `${removedEquipment} removed from team ${team.id}.`
        : `${removedEquipment} removed for this session. Browser storage is unavailable.`;
    message.classList.remove("error");
    renderEverything();
});

/* ---------------------------------
   MAINTENANCE TEAM JOBS
---------------------------------- */

function renderJobs() {
    /*
     * T1 is used as the demo login's assigned team.
     * A real backend must determine the team from the
     * authenticated account, not trust browser data.
     */
    const assignedReports = reports.filter(function (report) {
        return report.assignedTeam === "T1";
    });

    if (assignedReports.length === 0) {
        getElement("jobsTable").innerHTML =
            `<tr><td colspan="6">No assigned repair jobs.</td></tr>`;
        return;
    }

    getElement("jobsTable").innerHTML = assignedReports.map(function (report) {
        let action = "—";

        if (report.status !== "Completed") {
            action = `
                <button class="secondary"
                    data-complete-report="${escapeHTML(report.id)}">
                    Mark completed
                </button>
            `;
        }

        return `
            <tr>
                <td>J-${escapeHTML(report.id)}</td>
                <td>${escapeHTML(report.id)}</td>
                <td>${escapeHTML(report.roadId)}</td>
                <td>${escapeHTML(report.damage)}</td>
                <td>${statusBadge(report.status)}</td>
                <td>${action}</td>
            </tr>
        `;
    }).join("");
}

getElement("jobsTable").addEventListener("click", function (event) {
    const button = event.target.closest("[data-complete-report]");

    if (!button || currentUser?.role !== "team") {
        return;
    }

    const report = reports.find(function (item) {
        return item.id === button.dataset.completeReport;
    });

    if (report) {
        saveReportStatus(report, "Completed");
    }
});

/* ---------------------------------
   FASTEST ROUTE FINDER
---------------------------------- */

/*
 * Fill the start and destination dropdowns.
 */
function populateRouteDropdowns() {
    const names = Object.keys(locations);

    const startSelect = getElement("routeStart");
    const endSelect = getElement("routeEnd");

    const oldStart = startSelect.value;
    const oldEnd = endSelect.value;

    const options = names.map(function (name) {
        return `<option value="${escapeHTML(name)}">${escapeHTML(name)}</option>`;
    }).join("");

    startSelect.innerHTML = options;
    endSelect.innerHTML = options;

    startSelect.value = names.includes(oldStart)
        ? oldStart
        : names[0] || "";

    endSelect.value = names.includes(oldEnd)
        ? oldEnd
        : names[names.length - 1] || "";
}

getElement("routeForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    if (
        !currentUser ||
        !["engineering", "team"].includes(currentUser.role)
    ) {
        return;
    }

    const start = getElement("routeStart").value;
    const destination = getElement("routeEnd").value;
    const resultBox = getElement("routeResult");

    if (locationsCount() === 0) {
        resultBox.textContent = roadLoadError
            ? "Routes are unavailable because the road database could not be loaded."
            : "No road network is currently stored in the database.";
        return;
    }

    resultBox.textContent = "Calculating route...";
    try {
        const query = new URLSearchParams({ start: start, destination: destination });
        const response = await fetch(`${ROUTE_API_URL}?${query.toString()}`, {
            cache: "no-store"
        });
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Route API returned HTTP ${response.status}.`);
        }

        currentRoutePath = String(payload.path || "")
            .split(" -> ")
            .map(function (name) { return name.trim(); })
            .filter(Boolean);

        resultBox.innerHTML = `
            <strong>Fastest reachable route found</strong>
            <p>${escapeHTML(payload.path)}</p>
            <p><strong>Estimated travel time:</strong> ${Number(payload.time)} minutes</p>
            <p>Closed roads are excluded from this calculation.</p>
        `;
        renderRouteMap();
        renderRoadMap();
    } catch (error) {
        currentRoutePath = [];
        renderRouteMap();
        renderRoadMap();
        resultBox.textContent = `Could not calculate route: ${error.message}`;
    }
});

function locationsCount() {
    return Object.keys(locations).length;
}

/* ---------------------------------
   REFRESH ALL DEMO VIEWS
---------------------------------- */

function renderEverything() {
    renderStatistics();
    renderRoadMap();
    renderRouteMap();
    renderRecentReports();
    renderRoads();
    renderReports();
    renderTeams();
    populateRouteDropdowns();
}

function mapReport(report) {
    if (
        !report ||
        typeof report.id !== "string" ||
        typeof report.roadId !== "string" ||
        typeof report.damage !== "string" ||
        typeof report.status !== "string"
    ) {
        throw new Error("The Report API returned an invalid report record.");
    }

    return {
        id: report.id,
        roadId: report.roadId,
        damage: report.damage,
        severity: report.severity,
        risk: report.risk,
        importance: report.importance,
        traffic: Number(report.traffic),
        status: report.status,
        assignedTeam: "T1",
        submittedAt: report.submittedAt
    };
}

async function loadReports() {
    const message = getElement("reportStatusMessage");
    if (message && currentUser) {
        message.textContent = "Loading saved reports...";
        message.classList.remove("error");
    }

    try {
        const response = await fetch(REPORT_API_URL, { cache: "no-store" });
        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.error || `Report API returned HTTP ${response.status}.`);
        }
        if (!Array.isArray(payload)) {
            throw new Error("The Report API returned an invalid report list.");
        }

        const savedReports = payload.map(mapReport);
        reports.splice(0, reports.length, ...savedReports);
        reportLoadError = "";
        if (message && currentUser) {
            message.textContent = `${reports.length} saved report(s) loaded.`;
        }
    } catch (error) {
        reportLoadError = error.message;
        if (message && currentUser) {
            message.textContent = `Cannot load saved reports: ${error.message}`;
            message.classList.add("error");
        }
    }

    renderEverything();
}

async function loadRoads() {
    const status = getElement("roadConnectionStatus");
    const badge = getElement("appDataBadge");

    if (status) {
        status.textContent = "Loading roads from the database...";
        status.classList.remove("error");
    }
    if (badge) {
        badge.textContent = "ROAD DB: CONNECTING";
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 8000);
    try {
        /* Coordinates for the map, markers and route pickers come from the
         * backend location register, never from the browser. */
        await loadLocations();

        const response = await fetch(ROAD_API_URL, {
            cache: "no-store",
            signal: controller.signal
        });
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Road API returned HTTP ${response.status}.`);
        }
        if (!Array.isArray(payload)) {
            throw new Error("The Road API returned an invalid road list.");
        }

        roads = payload.map(function (road) {
            if (
                typeof road.id !== "string" ||
                typeof road.from !== "string" ||
                typeof road.to !== "string" ||
                !Number.isFinite(Number(road.time))
            ) {
                throw new Error("The Road API returned an invalid road record.");
            }

            return {
                id: road.id,
                from: road.from,
                to: road.to,
                time: Number(road.time),
                closed: Boolean(road.closed),
                fromLat: road.fromLat,
                fromLng: road.fromLng,
                toLat: road.toLat,
                toLng: road.toLng
            };
        });

        roadLoadError = "";
        if (status) {
            status.textContent = `Connected to database · ${roads.length} road(s) loaded`;
            status.classList.remove("error");
        }
    } catch (error) {
        roads = [];
        locations = {};
        roadLoadError = error.name === "AbortError"
            ? "The Road API did not respond within 8 seconds."
            : error.message;
        if (status) {
            status.textContent = `Cannot load roads: ${roadLoadError} Start the Java application with the SQLite JDBC driver.`;
            status.classList.add("error");
        }
    } finally {
        clearTimeout(timeoutId);
    }

    if (badge) {
        badge.textContent = roadLoadError ? "ROAD DB: OFFLINE" : "ROAD DB: CONNECTED";
        badge.classList.toggle("database-offline", Boolean(roadLoadError));
    }

    renderEverything();
}

/* Pull the location register from the backend: name → { lat, lng }.
 * These coordinates are the only positions the map is allowed to use. */
async function loadLocations() {
    try {
        const response = await fetch(LOCATION_API_URL, { cache: "no-store" });
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Location API returned HTTP ${response.status}.`);
        }
        if (!Array.isArray(payload)) {
            throw new Error("The Location API returned an invalid location list.");
        }

        const byName = {};
        payload.forEach(function (location) {
            const lat = Number(location.latitude);
            const lng = Number(location.longitude);
            if (
                typeof location.name === "string" &&
                Number.isFinite(lat) &&
                Number.isFinite(lng)
            ) {
                byName[location.name] = { lat: lat, lng: lng };
            }
        });
        locations = byName;
    } catch (error) {
        /* Keep whatever is already in the register if the API is down;
         * road segments that carry their own coordinates still render. */
        locations = Object.keys(locations).length > 0 ? locations : {};
    }
}