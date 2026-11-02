"use strict";

/* Roads and reports are loaded from the Java API; maintenance teams remain demo data. */

const API_BASE = (window.location.protocol === "file:" || !window.location.origin || window.location.origin === "null")
    ? "http://127.0.0.1:8080"
    : window.location.origin;

const ROAD_API_URL = `${API_BASE}/api/roads`;
const ROUTE_API_URL = `${API_BASE}/api/routes`;
const REPORT_API_URL = `${API_BASE}/api/reports`;
const LOCATION_API_URL = `${API_BASE}/api/locations`;
const COLORING_API_URL = `${API_BASE}/api/coloring`;






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

const DEFAULT_LOCATIONS = {
    "Municipal Office": { lat: 13.075529, lng: 80.258186 },
    "Market Junction": { lat: 13.083090, lng: 80.258186 },
    "Government School": { lat: 13.081383, lng: 80.263886 },
    "City Hospital": { lat: 13.081139, lng: 80.271257 },
    "Bus Stand": { lat: 13.075529, lng: 80.270529 },
    "Railway Station": { lat: 13.075529, lng: 80.278714 },
    "Main Park": { lat: 13.089871, lng: 80.259043 },
    "Sports Ground": { lat: 13.089871, lng: 80.266800 },
    "Water Tank": { lat: 13.089871, lng: 80.275414 },
    "Industrial Estate": { lat: 13.089871, lng: 80.283214 },
    "Bypass Junction": { lat: 13.082798, lng: 80.282486 },
    "Post Office": { lat: 13.075529, lng: 80.261829 },
    "Library": { lat: 13.075529, lng: 80.264271 },
    "Fire Station": { lat: 13.085529, lng: 80.277429 },
    "Police Station": { lat: 13.085920, lng: 80.283214 }
};

const DEFAULT_ROADS = [
    { id: "R001", from: "Municipal Office", to: "Market Junction", time: 4, closed: false, fromLat: 13.075529, fromLng: 80.258186, toLat: 13.083090, toLng: 80.258186 },
    { id: "R002", from: "Market Junction", to: "Government School", time: 3, closed: false, fromLat: 13.083090, fromLng: 80.258186, toLat: 13.081383, toLng: 80.263886 },
    { id: "R003", from: "Government School", to: "City Hospital", time: 5, closed: false, fromLat: 13.081383, fromLng: 80.263886, toLat: 13.081139, toLng: 80.271257 },
    { id: "R004", from: "City Hospital", to: "Bus Stand", time: 4, closed: false, fromLat: 13.081139, fromLng: 80.271257, toLat: 13.075529, toLng: 80.270529 },
    { id: "R005", from: "Bus Stand", to: "Railway Station", time: 6, closed: false, fromLat: 13.075529, fromLng: 80.270529, toLat: 13.075529, toLng: 80.278714 },
    { id: "R006", from: "Market Junction", to: "Main Park", time: 4, closed: false, fromLat: 13.083090, fromLng: 80.258186, toLat: 13.089871, toLng: 80.259043 },
    { id: "R007", from: "Main Park", to: "Sports Ground", time: 3, closed: false, fromLat: 13.089871, fromLng: 80.259043, toLat: 13.089871, toLng: 80.266800 },
    { id: "R008", from: "Sports Ground", to: "Water Tank", time: 5, closed: false, fromLat: 13.089871, fromLng: 80.266800, toLat: 13.089871, toLng: 80.275414 },
    { id: "R009", from: "Water Tank", to: "Industrial Estate", time: 6, closed: false, fromLat: 13.089871, fromLng: 80.275414, toLat: 13.089871, toLng: 80.283214 },
    { id: "R010", from: "Industrial Estate", to: "Bypass Junction", time: 5, closed: false, fromLat: 13.089871, fromLng: 80.283214, toLat: 13.082798, toLng: 80.282486 },
    { id: "R011", from: "Municipal Office", to: "Post Office", time: 3, closed: false, fromLat: 13.075529, fromLng: 80.258186, toLat: 13.075529, toLng: 80.261829 },
    { id: "R012", from: "Post Office", to: "Library", time: 2, closed: false, fromLat: 13.075529, fromLng: 80.261829, toLat: 13.075529, toLng: 80.264271 },
    { id: "R013", from: "Library", to: "Government School", time: 3, closed: false, fromLat: 13.075529, fromLng: 80.264271, toLat: 13.081383, toLng: 80.263886 },
    { id: "R014", from: "Post Office", to: "Bus Stand", time: 5, closed: false, fromLat: 13.075529, fromLng: 80.261829, toLat: 13.075529, toLng: 80.270529 },
    { id: "R015", from: "City Hospital", to: "Fire Station", time: 3, closed: false, fromLat: 13.081139, fromLng: 80.271257, toLat: 13.085529, toLng: 80.277429 },
    { id: "R016", from: "Fire Station", to: "Police Station", time: 2, closed: false, fromLat: 13.085529, fromLng: 80.277429, toLat: 13.085920, toLng: 80.283214 },
    { id: "R017", from: "Police Station", to: "Bypass Junction", time: 6, closed: false, fromLat: 13.085920, fromLng: 80.283214, toLat: 13.082798, toLng: 80.282486 },
    { id: "R018", from: "Railway Station", to: "Bypass Junction", time: 7, closed: false, fromLat: 13.075529, fromLng: 80.278714, toLat: 13.082798, toLng: 80.282486 },
    { id: "R019", from: "Water Tank", to: "Fire Station", time: 7, closed: false, fromLat: 13.089871, fromLng: 80.275414, toLat: 13.085529, toLng: 80.277429 }
];

const DEFAULT_PHASING_DATA = {
    chromaticNumber: 4,
    totalRoads: 19,
    phases: [
        { phase: 1, name: "Phase 1", colorHex: "#10b981", roadCount: 6, roads: DEFAULT_ROADS.filter(r => ["R002", "R004", "R007", "R010", "R011", "R019"].includes(r.id)) },
        { phase: 2, name: "Phase 2", colorHex: "#3b82f6", roadCount: 6, roads: DEFAULT_ROADS.filter(r => ["R001", "R003", "R008", "R014", "R016", "R018"].includes(r.id)) },
        { phase: 3, name: "Phase 3", colorHex: "#f59e0b", roadCount: 6, roads: DEFAULT_ROADS.filter(r => ["R005", "R006", "R009", "R012", "R015", "R017"].includes(r.id)) },
        { phase: 4, name: "Phase 4", colorHex: "#8b5cf6", roadCount: 1, roads: DEFAULT_ROADS.filter(r => ["R013"].includes(r.id)) }
    ],
    roadColors: {
        R001: { phase: 2, colorHex: "#3b82f6", conflicts: ["R002", "R006", "R011"] },
        R002: { phase: 1, colorHex: "#10b981", conflicts: ["R001", "R003", "R006", "R013"] },
        R003: { phase: 2, colorHex: "#3b82f6", conflicts: ["R002", "R004", "R013", "R015"] },
        R004: { phase: 1, colorHex: "#10b981", conflicts: ["R003", "R005", "R014", "R015"] },
        R005: { phase: 3, colorHex: "#f59e0b", conflicts: ["R004", "R014", "R018"] },
        R006: { phase: 3, colorHex: "#f59e0b", conflicts: ["R001", "R002", "R007"] },
        R007: { phase: 1, colorHex: "#10b981", conflicts: ["R006", "R008"] },
        R008: { phase: 2, colorHex: "#3b82f6", conflicts: ["R007", "R009", "R019"] },
        R009: { phase: 3, colorHex: "#f59e0b", conflicts: ["R008", "R010", "R019"] },
        R010: { phase: 1, colorHex: "#10b981", conflicts: ["R009", "R017", "R018"] },
        R011: { phase: 1, colorHex: "#10b981", conflicts: ["R001", "R012", "R014"] },
        R012: { phase: 3, colorHex: "#f59e0b", conflicts: ["R011", "R013", "R014"] },
        R013: { phase: 4, colorHex: "#8b5cf6", conflicts: ["R002", "R003", "R012"] },
        R014: { phase: 2, colorHex: "#3b82f6", conflicts: ["R004", "R005", "R011", "R012"] },
        R015: { phase: 3, colorHex: "#f59e0b", conflicts: ["R003", "R004", "R016", "R019"] },
        R016: { phase: 2, colorHex: "#3b82f6", conflicts: ["R015", "R017", "R019"] },
        R017: { phase: 3, colorHex: "#f59e0b", conflicts: ["R010", "R016", "R018"] },
        R018: { phase: 2, colorHex: "#3b82f6", conflicts: ["R005", "R010", "R017"] },
        R019: { phase: 1, colorHex: "#10b981", conflicts: ["R008", "R009", "R015", "R016"] }
    }
};

const DEFAULT_REPORTS = [
    {
        reportId: "DR001",
        roadId: "R004",
        damageType: "Pothole",
        location: "City Hospital",
        startLocation: "City Hospital",
        endLocation: "Bus Stand",
        roadTravelTime: 4,
        severity: 3,
        accidentRisk: 3,
        roadImportance: 3,
        trafficRank: 2,
        priorityScore: 3.5,
        status: "Active",
        assignedTeam: "T1",
        assignedEquipment: "Pothole Kit",
        submittedAt: "2026-11-01 10:30"
    },
    {
        reportId: "DR002",
        roadId: "R001",
        damageType: "Road Crack",
        location: "Municipal Office",
        startLocation: "Municipal Office",
        endLocation: "Market Junction",
        roadTravelTime: 4,
        severity: 2,
        accidentRisk: 2,
        roadImportance: 3,
        trafficRank: 3,
        priorityScore: 2.8,
        status: "Pending",
        assignedTeam: "Unassigned",
        assignedEquipment: "Road Cutter",
        submittedAt: "2026-11-01 11:15"
    }
];

function calculateDijkstraClientSide(startNode, destNode) {
    const adj = {};
    roads.forEach(r => {
        if (r.closed) return;
        if (!adj[r.from]) adj[r.from] = [];
        if (!adj[r.to]) adj[r.to] = [];
        adj[r.from].push({ node: r.to, time: r.time });
        adj[r.to].push({ node: r.from, time: r.time });
    });

    const dist = {};
    const prev = {};
    const unvisited = new Set(Object.keys(DEFAULT_LOCATIONS));

    unvisited.forEach(n => dist[n] = Infinity);
    dist[startNode] = 0;

    while (unvisited.size > 0) {
        let curr = null;
        let minDist = Infinity;
        unvisited.forEach(n => {
            if (dist[n] < minDist) {
                minDist = dist[n];
                curr = n;
            }
        });
        if (!curr || minDist === Infinity || curr === destNode) break;
        unvisited.delete(curr);

        (adj[curr] || []).forEach(edge => {
            if (unvisited.has(edge.node)) {
                const alt = dist[curr] + edge.time;
                if (alt < dist[edge.node]) {
                    dist[edge.node] = alt;
                    prev[edge.node] = curr;
                }
            }
        });
    }

    if (dist[destNode] === Infinity || dist[destNode] === undefined) return null;
    const path = [];
    let u = destNode;
    while (u) {
        path.unshift(u);
        u = prev[u];
    }
    return { path: path.join(" -> "), time: dist[destNode] };
}

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

let teams = loadTeams();

async function fetchTeamsFromApi() {
    try {
        const response = await fetch(`${API_BASE}/api/teams`, { cache: "no-store" });
        if (response.ok) {
            const payload = await response.json();
            if (Array.isArray(payload) && payload.length > 0) {
                teams = payload.map(function (team) {
                    return {
                        id: team.id,
                        location: team.location,
                        available: Boolean(team.available),
                        activeJob: team.activeJob || "",
                        equipment: Array.isArray(team.equipment) ? team.equipment : []
                    };
                });
                saveTeams();
                renderTeams();
                renderJobs();
            }
        }
    } catch (error) {
        // Fall back to local teams
    }
}

async function triggerAutoAllot() {
    const message = getElement("teamFormMessage");
    if (message) {
        message.textContent = "Auto-allotting teams and tools to pending repairs...";
        message.classList.remove("error");
    }

    try {
        const response = await fetch(`${API_BASE}/api/teams/auto-allot`, {
            method: "POST"
        });
        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.error || `HTTP ${response.status}`);
        }
        await fetchTeamsFromApi();
        await loadReports();
        if (message) {
            message.textContent = payload.message || "Auto-allotment completed! Teams and tools assigned.";
            if (payload.allottedCount === 0 && payload.pendingCount > 0) {
                message.classList.add("error");
            } else {
                message.classList.remove("error");
            }
        }
        renderEverything();
    } catch (error) {
        // Client-side / Demo Mode Fallback:
        const pendingReports = reports.filter(function (r) {
            return r.status !== "Completed" && 
                (r.status === "Pending" || !r.assignedTeam || r.assignedTeam.toLowerCase().includes("unassigned"));
        });

        if (pendingReports.length === 0) {
            if (message) {
                message.textContent = "No pending repairs waiting for allotment. All tasks are active or completed.";
                message.classList.remove("error");
            }
            return;
        }

        const availableTeams = teams.filter(function (t) {
            return t.available && (!t.activeJob || t.activeJob === "");
        });

        if (availableTeams.length === 0) {
            if (message) {
                message.textContent = `All maintenance teams are currently busy. ${pendingReports.length} pending repair(s) queued for next available team.`;
                message.classList.add("error");
            }
            return;
        }

        let allottedCount = 0;
        const details = [];
        for (const rep of pendingReports) {
            const team = availableTeams.shift();
            if (!team) break;

            rep.assignedTeam = team.id;
            rep.status = "Active";
            const reqTool = rep.damage && rep.damage.toLowerCase().includes("crack") ? "Road Cutter"
                : rep.damage && rep.damage.toLowerCase().includes("resurfacing") ? "Resurfacing Machine"
                : "Pothole Kit";
            rep.assignedEquipment = reqTool;
            if (!team.equipment.includes(reqTool)) {
                team.equipment.push(reqTool);
            }
            team.available = false;
            team.activeJob = "J-" + rep.id;
            allottedCount++;
            details.push(`Team ${team.id} -> ${rep.id} (${rep.damage})`);
        }

        saveTeams();
        if (message) {
            message.textContent = `[Demo Mode] Auto-allotted ${allottedCount} team(s): ${details.join(", ")}`;
            message.classList.remove("error");
        }
        renderEverything();
    }
}

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
    routes: "Fastest Route",
    phasing: "Conflict-Free Maintenance Phasing"
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
    fetchTeamsFromApi();
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
    if (pageName === "reports") { loadReports(); fetchTeamsFromApi(); }
    if (pageName === "teams") { fetchTeamsFromApi(); renderTeams(); }
    if (pageName === "jobs") { fetchTeamsFromApi(); renderJobs(); }

    if (pageName === "dashboard") {
        if (roadMapInstance) {
            roadMapInstance.invalidateSize();
        }
        renderRoadMap();
    }

    if (pageName === "phasing") {
        if (phasingMapInstance) {
            phasingMapInstance.invalidateSize();
        }
        loadPhasing();
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
        road.closed = closed;
        if (status) {
            status.textContent = "[Demo Mode] " + road.id + " marked as " + (closed ? "closed" : "open") + " locally.";
            status.classList.remove("error");
        }
        renderEverything();
        renderRoads(getElement("roadSearch").value);
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

            const isUnassigned = !report.assignedTeam || report.assignedTeam.toLowerCase().includes("unassigned");
            const teamBadge = isUnassigned
                ? `<span class="status status-pending">Unassigned (Queued)</span>`
                : `<span class="status status-active">Team ${escapeHTML(report.assignedTeam.replace(/^Team\s+/i, ""))}</span>`;
            const toolsDisplay = report.assignedEquipment
                ? `<span style="font-size: 0.85em; color: #334155;">${escapeHTML(report.assignedEquipment)}</span>`
                : `<span style="font-size: 0.85em; color: #94a3b8;">—</span>`;

            return `
                <tr>
                    <td class="id-text">${escapeHTML(report.id)}</td>
                    <td>${escapeHTML(report.roadId)}</td>
                    <td>${escapeHTML(report.damage)}</td>
                    <td>${statusBadge(report.severity)}</td>
                    <td>${statusBadge(report.risk)}</td>
                    <td>${statusBadge(report.status)}</td>
                    <td>${teamBadge}</td>
                    <td>${toolsDisplay}</td>
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
        await fetchTeamsFromApi();
        await loadReports();
        renderEverything();
    } catch (error) {
        report.status = status;
        message.textContent = "[Demo Mode] " + report.id + " status marked as " + status + " locally.";
        message.classList.remove("error");
        renderEverything();
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
        const teamInfo = payload.assignedTeam && payload.assignedTeam !== "Unassigned"
            ? ` Auto-allotted to Team ${payload.assignedTeam} with tools: [${payload.assignedEquipment || 'Standard Tools'}].`
            : " Placed in pending queue (no teams currently available).";
        message.textContent = `${payload.id} saved to the municipal database.${teamInfo}`;
        await fetchTeamsFromApi();
        renderEverything();
    } catch (error) {
        const roadId = getElement("reportRoad").value;
        const damageType = getElement("damageType").value;
        const newRep = {
            id: "D" + String(reports.length + 1).padStart(3, "0"),
            roadId: roadId,
            damageType: damageType,
            severity: getElement("severity").value,
            risk: getElement("accidentRisk").value,
            importance: getElement("roadImportance").value,
            trafficRank: Number(getElement("trafficRank").value) || 1,
            score: (Number(getElement("severity").value) || 1) * 3,
            status: "ACTIVE",
            assignedTeam: "T1",
            assignedEquipment: "Pothole Kit",
            repairPlan: "Standard repair for " + damageType
        };
        reports.push(newRep);
        form.reset();
        message.textContent = "[Demo Mode] " + newRep.id + " recorded locally and assigned to Team T1.";
        message.classList.remove("error");
        renderEverything();
    } finally {
        submitButton.disabled = false;
    }
});

/* ---------------------------------
   MAINTENANCE TEAMS
---------------------------------- */

function getPendingReports() {
    return reports.filter(function (r) {
        if (!r) return false;
        const status = (r.status || "").toLowerCase();
        const team = (r.assignedTeam || "").toLowerCase();
        if (status === "completed") return false;
        return status === "pending" || !r.assignedTeam || team.includes("unassigned") || team === "";
    });
}

function renderTeams() {
    const newTeamLocationSelect = getElement("newTeamLocation");
    const previousLocation = newTeamLocationSelect.value;
    const registerButton = getElement("teamRegistrationForm")?.querySelector("button[type='submit']");
    const pendingReports = getPendingReports();

    if (pendingReports.length === 0) {
        newTeamLocationSelect.innerHTML = '<option value="" disabled selected>No road damage is found</option>';
        newTeamLocationSelect.disabled = true;
        if (registerButton) {
            registerButton.disabled = true;
            registerButton.title = "No road damage is found";
        }
    } else {
        newTeamLocationSelect.disabled = false;
        if (registerButton) {
            registerButton.disabled = false;
            registerButton.title = "";
        }

        const pendingOptions = [];
        const seenValues = new Set();

        pendingReports.forEach(function (r) {
            const road = Array.isArray(roads) ? roads.find(function (rd) { return rd.id === r.roadId; }) : null;
            const fromLoc = r.from || (road ? road.from : "");
            const toLoc = r.to || (road ? road.to : "");
            const loc = r.location || (fromLoc && toLoc ? `${fromLoc} - ${toLoc}` : fromLoc || toLoc);

            const candidates = [];
            if (loc) {
                candidates.push({
                    value: loc,
                    label: `${loc} — [Pending: ${r.id} (${r.damage || "Damage"})]`
                });
            }
            if (fromLoc && fromLoc !== loc) {
                candidates.push({
                    value: fromLoc,
                    label: `${fromLoc} — [Pending: ${r.id} (${r.damage || "Damage"})]`
                });
            }
            if (toLoc && toLoc !== loc && toLoc !== fromLoc) {
                candidates.push({
                    value: toLoc,
                    label: `${toLoc} — [Pending: ${r.id} (${r.damage || "Damage"})]`
                });
            }

            candidates.forEach(function (c) {
                if (c.value && !seenValues.has(c.value)) {
                    seenValues.add(c.value);
                    pendingOptions.push(c);
                }
            });
        });

        if (pendingOptions.length === 0) {
            newTeamLocationSelect.innerHTML = '<option value="" disabled selected>No road damage is found</option>';
            newTeamLocationSelect.disabled = true;
            if (registerButton) {
                registerButton.disabled = true;
                registerButton.title = "No road damage is found";
            }
        } else {
            newTeamLocationSelect.innerHTML = pendingOptions.map(function (opt) {
                return `<option value="${escapeHTML(opt.value)}">${escapeHTML(opt.label)}</option>`;
            }).join("");

            if (seenValues.has(previousLocation)) {
                newTeamLocationSelect.value = previousLocation;
            }
        }
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
                ${(function() {
                    const assignedReport = reports.find(function (r) {
                        if (!r || !r.assignedTeam || r.status === "Completed") return false;
                        const cleanTeam = r.assignedTeam.trim().replace(/^Team\s+/i, "").toUpperCase();
                        return cleanTeam === team.id.toUpperCase();
                    });
                    const activeJobText = team.activeJob || (assignedReport ? ("J-" + assignedReport.id) : "");
                    return activeJobText
                        ? '<p class="team-assignment" style="margin:6px 0 10px; font-size:13px;"><strong>Active Job:</strong> <span class="badge blue" style="background:#eff6ff; color:#1d4ed8; padding:3px 8px; border-radius:4px; font-weight:700;">' + escapeHTML(activeJobText) + '</span></p>'
                        : '<p class="team-assignment text-muted" style="margin:6px 0 10px; font-size:13px; color:#64748b;"><small>No active job (Ready for dispatch)</small></p>';
                })()}
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
        const response = await fetch(`${API_BASE}/api/equipment${query}`, { cache: "no-store" });
        const payload = await response.json();

        if (!response.ok) {
            throw new Error(payload.error || `Equipment API returned HTTP ${response.status}.`);
        }

        payload.forEach(function (item) {
            const option = document.createElement("option");
            option.value = item;
            option.textContent = item;
            equipmentSelect.appendChild(option);
        });

        if (payload.includes(selectedValue)) {
            equipmentSelect.value = selectedValue;
        }
    } catch (error) {
        console.warn("Could not load equipment list:", error);
    }
}

getElement("equipmentDamageType").addEventListener("change", function () {
    getElement("equipmentName").value = "";
    loadEquipmentOptions(this.value);
});
loadEquipmentOptions(getElement("equipmentDamageType").value);

getElement("teamRegistrationForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    if (currentUser?.role !== "engineering") {
        return;
    }

    const pendingReports = getPendingReports();
    const message = getElement("teamFormMessage");

    if (pendingReports.length === 0) {
        message.textContent = "No road damage is found.";
        message.classList.add("error");
        return;
    }

    const teamId = getElement("newTeamId").value.trim().toUpperCase();
    const location = getElement("newTeamLocation").value;
    const available = getElement("newTeamAvailability").value === "true";

    if (!location) {
        message.textContent = "No road damage is found.";
        message.classList.add("error");
        return;
    }

    if (teams.some(team => team.id.toUpperCase() === teamId)) {
        message.textContent = `Team ${teamId} is already registered.`;
        message.classList.add("error");
        return;
    }

    message.textContent = `Registering Team ${teamId}...`;
    message.classList.remove("error");

    try {
        const response = await fetch(`${API_BASE}/api/teams`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({
                id: teamId,
                location: location,
                available: String(available)
            })
        });

        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.error || `HTTP ${response.status}`);
        }

        event.target.reset();
        await fetchTeamsFromApi();
        await loadReports();

        const createdTeam = teams.find(function (t) { return t.id.toUpperCase() === teamId.toUpperCase(); });
        const activeJob = (createdTeam && createdTeam.activeJob) || (function () {
            const rep = reports.find(function (r) {
                if (!r || !r.assignedTeam || r.status === "Completed") return false;
                return r.assignedTeam.trim().replace(/^Team\s+/i, "").toUpperCase() === teamId.toUpperCase();
            });
            return rep ? ("J-" + rep.id) : "";
        })();
        const assignedInfo = activeJob
            ? ` Auto-allotted to Repair ${activeJob}!`
            : (available ? " Ready for dispatch." : " Marked unavailable.");

        message.textContent = `Team ${teamId} registered and saved in database.${assignedInfo}`;
        message.classList.remove("error");
        renderEverything();
    } catch (error) {
        const newTeamObj = {
            id: teamId,
            location: location,
            available: available,
            equipment: [],
            activeJob: ""
        };
        teams.push(newTeamObj);
        event.target.reset();

        let allottedNotice = "";
        if (available) {
            const pendingRep = reports.find(r => r.status !== "Completed" && (!r.assignedTeam || r.assignedTeam === "Unassigned" || r.assignedTeam.includes("Unassigned")));
            if (pendingRep) {
                pendingRep.assignedTeam = teamId;
                pendingRep.status = "Active";
                newTeamObj.available = false;
                newTeamObj.activeJob = "J-" + pendingRep.id;
                if (!newTeamObj.equipment.includes("Pothole Kit")) newTeamObj.equipment.push("Pothole Kit");
                allottedNotice = ` Auto-allotted to pending repair J-${pendingRep.id}!`;
            }
        }
        saveTeams();
        message.textContent = `[Demo Mode] Team ${teamId} registered locally.${allottedNotice}`;
        message.classList.remove("error");
        renderEverything();
    }
});

getElement("equipmentForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    if (currentUser?.role !== "engineering") {
        return;
    }

    const team = teams.find(item => item.id === getElement("equipmentTeam").value);
    const damageType = getElement("equipmentDamageType").value;
    const equipmentName = getElement("equipmentName").value.trim();
    const message = getElement("teamFormMessage");

    if (!team || !equipmentName || !damageType) {
        return;
    }

    if (team.equipment.some(item => item.toLowerCase() === equipmentName.toLowerCase())) {
        message.textContent = `${equipmentName} is already assigned to team ${team.id}.`;
        message.classList.add("error");
        return;
    }

    try {
        await fetch(`${API_BASE}/api/equipment`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({ damageType, equipmentName })
        });

        const response = await fetch(`${API_BASE}/api/teams/equipment`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: new URLSearchParams({ teamId: team.id, equipmentName: equipmentName })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        await fetchTeamsFromApi();
        event.target.reset();
        loadEquipmentOptions(damageType);
        message.textContent = `${equipmentName} added to team ${team.id} and saved to database.`;
        message.classList.remove("error");
        renderEverything();
    } catch (error) {
        message.textContent = `Could not save equipment: ${error.message}`;
        message.classList.add("error");
    }
});

getElement("teamsGrid").addEventListener("click", async function (event) {
    const button = event.target.closest("[data-remove-team]");

    if (!button || currentUser?.role !== "engineering") {
        return;
    }

    const team = teams.find(item => item.id === button.dataset.removeTeam);
    const equipmentIndex = Number(button.dataset.equipmentIndex);

    if (!team || !Number.isInteger(equipmentIndex) || !team.equipment[equipmentIndex]) {
        return;
    }

    const removedEquipment = team.equipment[equipmentIndex];
    const message = getElement("teamFormMessage");

    try {
        const response = await fetch(
            `${API_BASE}/api/teams/equipment?teamId=${encodeURIComponent(team.id)}&equipment=${encodeURIComponent(removedEquipment)}`,
            { method: "DELETE" }
        );
        if (response.ok) {
            await fetchTeamsFromApi();
            if (message) {
                message.textContent = `${removedEquipment} removed from team ${team.id} and updated in database.`;
                message.classList.remove("error");
            }
            renderEverything();
            return;
        }
    } catch (ignored) { }

    team.equipment.splice(equipmentIndex, 1);
    const saved = saveTeams();
    if (message) {
        message.textContent = saved
            ? `${removedEquipment} removed from team ${team.id}.`
            : `${removedEquipment} removed for this session. Browser storage is unavailable.`;
        message.classList.remove("error");
    }
    renderEverything();
});

/* ---------------------------------
   MAINTENANCE TEAM JOBS
---------------------------------- */

function renderJobs() {
    const userRole = currentUser?.role;
    const userTeam = currentUser?.initials || "T1";
    const assignedReports = reports.filter(function (report) {
        if (userRole === "engineering") {
            return report.assignedTeam && report.assignedTeam !== "Unassigned";
        }
        return report.assignedTeam === userTeam || (!report.assignedTeam && userTeam === "T1");
    });

    if (assignedReports.length === 0) {
        getElement("jobsTable").innerHTML =
            `<tr><td colspan="7">No assigned repair jobs.</td></tr>`;
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
                <td><small style="color: #334155;">${escapeHTML(report.assignedEquipment || 'Standard Kit')}</small></td>
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
        const local = calculateDijkstraClientSide(start, destination);
        if (local) {
            currentRoutePath = String(local.path || "").split(" -> ").map(s => s.trim()).filter(Boolean);
            resultBox.innerHTML = `
                <strong>Fastest reachable route found</strong>
                <p>${escapeHTML(local.path)}</p>
                <p><strong>Estimated travel time:</strong> ${Number(local.time)} minutes</p>
                <p>Closed roads are excluded from this calculation.</p>
            `;
            renderRouteMap();
            renderRoadMap();
            return;
        }
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
        location: report.location || report.from || "",
        from: report.from || "",
        to: report.to || "",
        damage: report.damage,
        severity: report.severity,
        risk: report.risk,
        importance: report.importance,
        traffic: Number(report.traffic),
        status: report.status,
        assignedTeam: report.assignedTeam || "Unassigned",
        assignedEquipment: report.assignedEquipment || "",
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
        if (reports.length === 0) {
            reports.splice(0, reports.length, ...DEFAULT_REPORTS.map(mapReport));
        }
        reportLoadError = "";
        if (message && currentUser) {
            message.textContent = "Demo Mode (Static Preview) · " + reports.length + " report(s) loaded.";
            message.classList.remove("error");
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
        await fetchTeamsFromApi();

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
        locations = Object.keys(locations).length > 0 ? locations : DEFAULT_LOCATIONS;
        roads = DEFAULT_ROADS.map(r => ({ ...r }));
        roadLoadError = "";
        if (status) {
            status.textContent = "Demo Mode (Static Preview) · " + roads.length + " roads loaded";
            status.classList.remove("error");
        }
        if (badge) {
            badge.textContent = "ROAD DB: DEMO MODE";
            badge.className = "status-badge demo-badge database-demo";
        }
    } finally {
        clearTimeout(timeoutId);
    }

    if (badge && !badge.textContent.includes("DEMO")) {
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
        locations = Object.keys(locations).length > 0 ? locations : DEFAULT_LOCATIONS;
    }
}

const autoAllotBtn = getElement("autoAllotButton");
if (autoAllotBtn) {
    autoAllotBtn.addEventListener("click", triggerAutoAllot);
}


/* =========================================================
   CONFLICT-FREE MAINTENANCE PHASING (GRAPH COLORING)
   ========================================================= */

let phasingMapInstance = null;
let phasingMapLayer = null;
let currentPhasingData = null;

async function loadPhasing() {
    try {
        const response = await fetch(COLORING_API_URL);
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        const data = await response.json();
        currentPhasingData = data;
        renderPhasing(data);
    } catch (err) {
        currentPhasingData = DEFAULT_PHASING_DATA;
        renderPhasing(DEFAULT_PHASING_DATA);
    }
}

function renderPhasing(data) {
    if (!data) return;

    // 1. Metric Stat Cards
    const metricsContainer = getElement("phasingMetrics");
    if (metricsContainer) {
        const totalPhases = data.chromaticNumber || 0;
        const totalRoads = data.totalRoads || 0;
        const maxConcurrent = data.phases && data.phases.length > 0
            ? Math.max(...data.phases.map(function (p) { return p.roadCount || 0; }))
            : 0;
        const parallelSpeedup = totalPhases > 0 ? (totalRoads / totalPhases).toFixed(1) : "1.0";

        metricsContainer.innerHTML = `
            <div class="stat-card">
                <div><span>CHROMATIC NUMBER (&chi;)</span><i class="stat-icon blue">🎨</i></div>
                <h2>${totalPhases} <small style="font-size: 14px; font-weight: normal; color: #64748b;">Phases</small></h2>
                <small>Optimal conflict-free time slots</small>
            </div>
            <div class="stat-card">
                <div><span>TOTAL ROADS ANALYZED</span><i class="stat-icon blue">🛣</i></div>
                <h2>${totalRoads}</h2>
                <small>Intersection conflict graph</small>
            </div>
            <div class="stat-card">
                <div><span>MAX CONCURRENT REPAIRS</span><i class="stat-icon blue">⚡</i></div>
                <h2>${maxConcurrent} <small style="font-size: 14px; font-weight: normal; color: #64748b;">Roads</small></h2>
                <small>Parallel jobs without gridlock</small>
            </div>
            <div class="stat-card">
                <div><span>PARALLEL EFFICIENCY</span><i class="stat-icon blue">📈</i></div>
                <h2>${parallelSpeedup}x</h2>
                <small>Average repairs per phase</small>
            </div>
        `;
    }

    // 2. Phase Cards
    const cardsContainer = getElement("phasingCards");
    if (cardsContainer) {
        cardsContainer.innerHTML = (data.phases || []).map(function (phase) {
            const roadRows = (phase.roads || []).map(function (road) {
                const conflicts = (data.roadColors && data.roadColors[road.id] && data.roadColors[road.id].conflicts) || [];
                const conflictHint = conflicts.length > 0 ? `Conflicts: ${conflicts.join(", ")}` : "No conflicts";

                return `
                    <li class="phase-road-item" title="${escapeHTML(conflictHint)}">
                        <div>
                            <span class="phase-road-id">${escapeHTML(road.id)}</span>
                            <span style="margin-left: 8px; color: #334155; font-weight: 500;">
                                ${escapeHTML(road.from)} &harr; ${escapeHTML(road.to)}
                            </span>
                        </div>
                        <span style="font-size: 12px; color: #64748b; font-weight: 600;">
                            ${road.time} min
                        </span>
                    </li>
                `;
            }).join("");

            return `
                <div class="phase-card" style="--phase-color: ${phase.colorHex};">
                    <div class="phase-card-header">
                        <span class="phase-pill" style="background: ${phase.colorHex};">
                            ${escapeHTML(phase.name)}
                        </span>
                        <span style="font-size: 13px; font-weight: 600; color: #475569;">
                            ${phase.roadCount} Concurrent ${phase.roadCount === 1 ? "Road" : "Roads"}
                        </span>
                    </div>
                    <ul class="phase-road-list">
                        ${roadRows}
                    </ul>
                </div>
            `;
        }).join("");
    }

    // 3. Leaflet Phase Map
    renderPhasingMap(data);
}

function renderPhasingMap(data) {
    const mapElement = getElement("phasingMap");
    if (!mapElement || typeof L === "undefined") return;

    if (!phasingMapInstance) {
        phasingMapInstance = createMap("phasingMap");
    }

    if (!phasingMapInstance) return;

    phasingMapInstance.invalidateSize();

    if (phasingMapLayer) {
        phasingMapInstance.removeLayer(phasingMapLayer);
    }

    phasingMapLayer = L.featureGroup();
    const roadColors = data.roadColors || {};

    (data.phases || []).forEach(function (phase) {
        (phase.roads || []).forEach(function (road) {
            const a = roadEndpointCoords(road, "from");
            const b = roadEndpointCoords(road, "to");
            if (!a || !b) return;

            const color = phase.colorHex || "#10b981";
            const segment = L.polyline([a, b], {
                color: color,
                weight: 6,
                opacity: 0.95,
                lineCap: "round"
            });

            const conflicts = (roadColors[road.id] && roadColors[road.id].conflicts) || [];
            segment.bindTooltip(`
                <div style="font-family: inherit; font-size: 12px;">
                    <strong style="color:${color};">${escapeHTML(road.id)} · ${escapeHTML(phase.name)}</strong><br>
                    <span>${escapeHTML(road.from)} &harr; ${escapeHTML(road.to)} (${road.time} min)</span><br>
                    <small style="color: #64748b;">Adjacent Conflicts: ${escapeHTML(conflicts.join(", ") || "None")}</small>
                </div>
            `, { sticky: true });

            phasingMapLayer.addLayer(segment);
        });
    });

    getLocationNames().forEach(function (name) {
        const point = getLocationCoords(name);
        if (!point) return;

        const marker = L.circleMarker(point, {
            radius: 6,
            color: "#0f172a",
            weight: 2,
            fillColor: "#ffffff",
            fillOpacity: 1
        });
        marker.bindTooltip(`<b>${escapeHTML(name)}</b><br><small>Junction / Intersection</small>`, { sticky: true });
        phasingMapLayer.addLayer(marker);
    });

    phasingMapLayer.addTo(phasingMapInstance);
}

const refreshPhasingBtn = getElement("refreshPhasingButton");
if (refreshPhasingBtn) {
    refreshPhasingBtn.addEventListener("click", loadPhasing);
}
