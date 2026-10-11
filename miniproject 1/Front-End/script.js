"use strict";

/* Road records are loaded from the Java API; reports and teams remain demo data. */

const ROAD_API_URL = "http://127.0.0.1:8080/api/roads";

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
let nextReportNumber = 4;
let roads = [];
let roadLoadError = "";

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

const reports = [
    {
        id: "DR001",
        roadId: "R2",
        damage: "Pothole",
        severity: "High",
        risk: "High",
        importance: "High",
        traffic: 1,
        status: "Pending",
        assignedTeam: "T1"
    },
    {
        id: "DR002",
        roadId: "R3",
        damage: "Road Crack",
        severity: "Medium",
        risk: "Medium",
        importance: "Medium",
        traffic: 2,
        status: "Pending",
        assignedTeam: "T1"
    },
    {
        id: "DR003",
        roadId: "R5",
        damage: "Surface Damage",
        severity: "Low",
        risk: "Low",
        importance: "Low",
        traffic: 3,
        status: "Completed",
        assignedTeam: "T1"
    }
];

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
}

/* ---------------------------------
   LOGOUT
---------------------------------- */

getElement("logoutButton").addEventListener("click", function () {
    currentUser = null;

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
    if (pageName === "reports") renderReports();
    if (pageName === "teams") renderTeams();
    if (pageName === "jobs") renderJobs();
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
   DRAW THE SAMPLE ROAD NETWORK
---------------------------------- */

function renderRoadMap() {
    if (roads.length === 0) {
        getElement("roadMap").innerHTML = `
            <text x="350" y="205" text-anchor="middle" class="map-label">
                ${roadLoadError ? "Road database unavailable" : "No roads in the database"}
            </text>
        `;
        return;
    }

    const edgeHTML = roads.map(function (road) {
        const start = locations[road.from];
        const end = locations[road.to];

        if (!start || !end) {
            return "";
        }

        const middleX = (start.x + end.x) / 2;
        const middleY = (start.y + end.y) / 2;

        const roadClass = road.closed ? "road-edge closed" : "road-edge";
        const labelColor = road.closed ? "#c33e48" : "#346bd0";

        return `
            <g>
                <line class="${roadClass}"
                    x1="${start.x}" y1="${start.y}"
                    x2="${end.x}" y2="${end.y}" />

                <rect x="${middleX - 20}" y="${middleY - 13}"
                    width="40" height="23" rx="6"
                    fill="white" stroke="#dce5ef" />

                <text x="${middleX}" y="${middleY + 3}"
                    text-anchor="middle" font-size="10"
                    font-weight="700" fill="${labelColor}">
                    ${escapeHTML(road.id)}
                </text>
            </g>
        `;
    }).join("");

    const nodeHTML = Object.entries(locations).map(function (entry) {
        const name = entry[0];
        const point = entry[1];

        return `
            <g>
                <circle class="map-node"
                    cx="${point.x}" cy="${point.y}" r="8" />

                <text class="map-label"
                    x="${point.x}" y="${point.y - 16}"
                    text-anchor="middle">
                    ${escapeHTML(name)}
                </text>
            </g>
        `;
    }).join("");

    getElement("roadMap").innerHTML = edgeHTML + nodeHTML;
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
            `<tr><td colspan="7">No reports submitted yet.</td></tr>`;
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

/* Engineering changes a report's demo status. */
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

    report.status = event.target.value;

    renderEverything();
    renderReports();
});

/* Ward Officer submits a new demo report. */
getElement("reportForm").addEventListener("submit", function (event) {
    event.preventDefault();

    if (currentUser?.role !== "ward") {
        return;
    }

    const newReport = {
        id: "DR" + String(nextReportNumber).padStart(3, "0"),
        roadId: getElement("reportRoad").value,
        damage: getElement("damageType").value,
        severity: getElement("severity").value,
        risk: getElement("accidentRisk").value,
        importance: getElement("roadImportance").value,
        traffic: Number(getElement("trafficRank").value),
        status: "Pending",
        assignedTeam: "T1"
    };

    nextReportNumber++;
    reports.push(newReport);

    event.target.reset();

    renderEverything();
    renderReports();

    alert(
        "Demo report submitted successfully. " +
        "This report is not saved in your Java backend or database."
    );
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
                    Mark completed (demo)
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
        report.status = "Completed";
        renderEverything();
        renderJobs();
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

/*
 * Dijkstra's algorithm:
 * 1. Start at the selected location.
 * 2. Choose the unvisited location with the smallest known time.
 * 3. Ignore closed roads.
 * 4. Update travel times for connected locations.
 * 5. Reconstruct the fastest path.
 */
function findFastestRoute(start, destination) {
    const distance = {};
    const previous = {};
    const unvisited = new Set(Object.keys(locations));

    for (const location of unvisited) {
        distance[location] = Infinity;
        previous[location] = null;
    }

    if (!(start in distance) || !(destination in distance)) {
        return null;
    }

    distance[start] = 0;

    while (unvisited.size > 0) {
        let current = null;
        let smallestDistance = Infinity;

        for (const location of unvisited) {
            if (distance[location] < smallestDistance) {
                smallestDistance = distance[location];
                current = location;
            }
        }

        if (current === null || smallestDistance === Infinity) {
            break;
        }

        if (current === destination) {
            break;
        }

        unvisited.delete(current);

        for (const road of roads) {
            // Closed roads cannot be used.
            if (road.closed) {
                continue;
            }

            let nextLocation = null;

            if (road.from === current) {
                nextLocation = road.to;
            } else if (road.to === current) {
                nextLocation = road.from;
            }

            if (
                nextLocation === null ||
                !unvisited.has(nextLocation)
            ) {
                continue;
            }

            const newDistance = distance[current] + road.time;

            if (newDistance < distance[nextLocation]) {
                distance[nextLocation] = newDistance;

                previous[nextLocation] = {
                    location: current,
                    roadId: road.id
                };
            }
        }

        unvisited.delete(current);
    }

    if (distance[destination] === Infinity) {
        return null;
    }

    const path = [];
    const roadIds = [];
    let current = destination;

    while (current !== null) {
        path.unshift(current);

        const previousStep = previous[current];

        if (previousStep) {
            roadIds.unshift(previousStep.roadId);
            current = previousStep.location;
        } else {
            current = null;
        }
    }

    return {
        path: path,
        roadIds: roadIds,
        time: distance[destination]
    };
}

getElement("routeForm").addEventListener("submit", function (event) {
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

    if (start === destination) {
        resultBox.textContent =
            "Starting location and destination are the same. Travel time: 0 minutes.";
        return;
    }

    const result = findFastestRoute(start, destination);

    if (result === null) {
        resultBox.innerHTML = `
            <strong>No reachable route found.</strong>
            <p>Try another destination or check the road closures.</p>
        `;
        return;
    }

    const pathHTML = result.path.map(function (location, index) {
        const arrow = index === 0
            ? ""
            : '<span class="route-arrow">→</span>';

        return `
            ${arrow}
            <span class="route-stop">${escapeHTML(location)}</span>
        `;
    }).join("");

    resultBox.innerHTML = `
        <strong>Fastest reachable route found</strong>

        <div class="route-path">
            ${pathHTML}
        </div>

        <p>
            <strong>Road segments:</strong>
            ${result.roadIds.map(escapeHTML).join(" → ")}
        </p>

        <p>
            <strong>Estimated travel time:</strong>
            ${result.time} minutes
        </p>

        <p>Closed roads are excluded from this calculation.</p>
    `;
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
    renderRecentReports();
    renderRoads();
    renderReports();
    renderTeams();
    populateRouteDropdowns();
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
                closed: Boolean(road.closed)
            };
        });

        roadLoadError = "";
        updateLocationsFromRoads();
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

function updateLocationsFromRoads() {
    const locationNames = [...new Set(roads.flatMap(function (road) {
        return [road.from, road.to];
    }))];
    const centerX = 350;
    const centerY = 205;
    const radiusX = Math.min(270, 65 + locationNames.length * 25);
    const radiusY = Math.min(145, 40 + locationNames.length * 14);

    locations = Object.fromEntries(locationNames.map(function (name, index) {
        const angle = (2 * Math.PI * index / Math.max(locationNames.length, 1)) - Math.PI / 2;
        return [name, {
            x: centerX + Math.cos(angle) * radiusX,
            y: centerY + Math.sin(angle) * radiusY
        }];
    }));
}