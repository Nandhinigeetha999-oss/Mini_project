package api;

import core.Main;
import db.DatabaseManager;
import model.Location;
import model.MaintenanceTeam;
import model.RepairJob;
import model.Road;
import model.RoadDamage;
import routing.Dijkstra;
import service.RepairManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** Serves the persisted road network to the browser frontend. */
public final class RoadApiServer {

    private RoadApiServer() {
    }

    public static void start(int port) {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) { }
        }
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/api/", RoadApiServer::handleRoads);
            server.createContext("/", RoadApiServer::handleStatic);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("Road API ready at http://localhost:" + port + "/api/roads");
            System.out.println("Frontend UI ready at http://localhost:" + port + "/");
        } catch (IOException exception) {
            System.err.println("Could not start the Road API on port " + port + ": " + exception.getMessage());
        }
    }

    private static void handleRoads(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getRawPath();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        try {
            if ("GET".equalsIgnoreCase(method) && "/api/database".equals(path)) {
                sendDatabaseTables(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/roads".equals(path)) {
                sendRoads(exchange, DatabaseManager.loadRoads());
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/equipment".equals(path)) {
                sendEquipment(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/reports".equals(path)) {
                sendReports(exchange, DatabaseManager.loadDamageReports());
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/locations".equals(path)) {
                sendLocations(exchange, DatabaseManager.loadLocations());
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/locations".equals(path)) {
                createLocation(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/equipment".equals(path)) {
                createEquipment(exchange);
                return;
            }

            if ("DELETE".equalsIgnoreCase(method) && "/api/equipment".equals(path)) {
                deleteEquipment(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/roads".equals(path)) {
                createRoad(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/reports".equals(path)) {
                createReport(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/routes".equals(path)) {
                sendRoute(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/teams".equals(path)) {
                sendTeams(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/teams".equals(path)) {
                createTeam(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/teams/auto-allot".equals(path)) {
                RepairManager.reconcileTeamAssignments();
                sendTeams(exchange);
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/api/teams/equipment".equals(path)) {
                addTeamEquipment(exchange);
                return;
            }

            if ("DELETE".equalsIgnoreCase(method) && "/api/teams/equipment".equals(path)) {
                removeTeamEquipment(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/jobs".equals(path)) {
                sendJobs(exchange);
                return;
            }

            if ("GET".equalsIgnoreCase(method) && "/api/coloring".equals(path)) {
                sendColoring(exchange);
                return;
            }

            String[] pathParts = path.split("/");
            if ("PATCH".equalsIgnoreCase(method) &&
                    pathParts.length == 5 &&
                    "api".equals(pathParts[1]) &&
                    "roads".equals(pathParts[2]) &&
                    "status".equals(pathParts[4])) {
                updateRoadStatus(exchange, pathParts[3]);
                return;
            }

            if ("PATCH".equalsIgnoreCase(method) &&
                    pathParts.length == 5 &&
                    "api".equals(pathParts[1]) &&
                    "reports".equals(pathParts[2]) &&
                    "status".equals(pathParts[4])) {
                updateReportStatus(exchange, pathParts[3]);
                return;
            }

            sendJson(exchange, 404, "{\"error\":\"API endpoint not found.\"}");
        } catch (SQLException exception) {
            sendJson(exchange, 500, "{\"error\":\"Database error: " + jsonEscape(exception.getMessage()) + "\"}");
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, "{\"error\":\"" + jsonEscape(exception.getMessage()) + "\"}");
        }
    }

    private static void createReport(HttpExchange exchange) throws IOException, SQLException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String roadId = queryValue(body, "roadId");
        String damageType = queryValue(body, "damageType");
        String severity = queryValue(body, "severity");
        String risk = queryValue(body, "risk");
        String importance = queryValue(body, "importance");
        String trafficValue = queryValue(body, "traffic");
        if (trafficValue == null) {
            throw new IllegalArgumentException("Traffic rank is required.");
        }

        int trafficRank;
        try {
            trafficRank = Integer.parseInt(trafficValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Traffic rank must be a whole number.");
        }

        RoadDamage report = DatabaseManager.insertDamageReport(
                roadId, damageType, severity, risk, importance, trafficRank);
        Main.addReportToMemory(report);

        // Automatically allot maintenance team and tools
        RepairManager.autoAllotTeamAndTools(report);

        sendReport(exchange, 201, report);
    }

    private static void updateReportStatus(HttpExchange exchange, String encodedReportId)
            throws IOException, SQLException {
        String reportId = URLDecoder.decode(encodedReportId, "UTF-8");
        String status = queryValue(exchange.getRequestURI().getRawQuery(), "status");
        if (!DatabaseManager.updateDamageReportStatus(reportId, status)) {
            sendJson(exchange, 404, "{\"error\":\"Report not found.\"}");
            return;
        }

        if (status != null && status.equalsIgnoreCase("COMPLETED")) {
            RepairManager.completeRepair(reportId);
        }

        RoadDamage report = Main.reportMap.get(reportId);
        if (report != null) {
            report.status = status.toUpperCase(Locale.ROOT);
            sendReport(exchange, 200, report);
            return;
        }

        for (RoadDamage savedReport : DatabaseManager.loadDamageReports()) {
            if (savedReport.reportId.equalsIgnoreCase(reportId)) {
                sendReport(exchange, 200, savedReport);
                return;
            }
        }
        sendJson(exchange, 404, "{\"error\":\"Report not found.\"}");
    }

    private static void createRoad(HttpExchange exchange) throws IOException, SQLException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String roadId = queryValue(body, "id");
        String from = queryValue(body, "from");
        String to = queryValue(body, "to");
        String travelTimeValue = queryValue(body, "time");

        if (roadId == null || roadId.isBlank() || from == null || from.isBlank() ||
                to == null || to.isBlank() || travelTimeValue == null) {
            throw new IllegalArgumentException("Road ID, start, destination, and travel time are required.");
        }

        int travelTime;
        try {
            travelTime = Integer.parseInt(travelTimeValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Travel time must be a positive whole number.");
        }
        if (travelTime <= 0) {
            throw new IllegalArgumentException("Travel time must be a positive whole number.");
        }

        if (!DatabaseManager.insertRoad(roadId, from, to, travelTime)) {
            sendJson(exchange, 409, "{\"error\":\"That road ID already exists.\"}");
            return;
        }

        Road road = new Road(roadId.trim(), from.trim(), to.trim(), travelTime);
        Main.roadNetwork.addRoadToMemory(road);
        sendRoad(exchange, 201, road);
    }

    private static void sendRoute(HttpExchange exchange) throws IOException, SQLException {
        String query = exchange.getRequestURI().getRawQuery();
        String start = queryValue(query, "start");
        String destination = queryValue(query, "destination");

        if (start == null || destination == null || start.isBlank() || destination.isBlank()) {
            throw new IllegalArgumentException("Provide both start and destination locations.");
        }
        if (!Main.roadNetwork.graph.containsKey(start) ||
                !Main.roadNetwork.graph.containsKey(destination)) {
            throw new IllegalArgumentException("Select locations from the current road network.");
        }

        int travelTime = Dijkstra.findShortestDistance(Main.roadNetwork, start, destination);
        if (travelTime < 0) {
            sendJson(exchange, 404, "{\"error\":\"No open route connects those locations.\"}");
            return;
        }

        String route = Dijkstra.findShortestRoute(Main.roadNetwork, start, destination);
        LinkedHashMap<String, Location> locations = DatabaseManager.loadLocations();

        StringBuilder json = new StringBuilder();
        json.append("{\"path\":\"").append(jsonEscape(route))
                .append("\",\"time\":").append(travelTime)
                .append(",\"points\":[");
        String[] stops = route == null ? new String[0] : route.split(" -> ");
        int index = 0;
        for (String stop : stops) {
            Location location = lookupLocation(locations, stop.trim());
            if (location == null) {
                continue;
            }
            if (index++ > 0) {
                json.append(',');
            }
            appendLocation(json, location);
        }
        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private static void updateRoadStatus(HttpExchange exchange, String encodedRoadId)
            throws IOException, SQLException {
        String roadId = URLDecoder.decode(encodedRoadId, "UTF-8");
        String closedValue = queryValue(exchange.getRequestURI().getRawQuery(), "closed");

        if (!"true".equalsIgnoreCase(closedValue) && !"false".equalsIgnoreCase(closedValue)) {
            throw new IllegalArgumentException("Provide closed=true or closed=false.");
        }

        boolean closed = Boolean.parseBoolean(closedValue);
        if (!DatabaseManager.updateRoadClosed(roadId, closed)) {
            sendJson(exchange, 404, "{\"error\":\"Road not found.\"}");
            return;
        }

        Road inMemoryRoad = Main.roadNetwork.findRoadById(roadId);
        if (inMemoryRoad != null) {
            if (closed) {
                inMemoryRoad.closeRoad();
            } else {
                inMemoryRoad.openRoad();
            }
        }

        for (Road road : DatabaseManager.loadRoads()) {
            if (road.roadId.equalsIgnoreCase(roadId)) {
                sendRoad(exchange, 200, road);
                return;
            }
        }

        sendJson(exchange, 404, "{\"error\":\"Road not found.\"}");
    }

    private static String queryValue(String query, String name) throws IOException {
        if (query == null) {
            return null;
        }

        for (String parameter : query.split("&")) {
            String[] pair = parameter.split("=", 2);
            if (pair.length == 2 && name.equals(URLDecoder.decode(pair[0], "UTF-8"))) {
                return URLDecoder.decode(pair[1], "UTF-8");
            }
        }
        return null;
    }

    private static void sendRoads(HttpExchange exchange, ArrayList<Road> roads)
            throws IOException, SQLException {
        LinkedHashMap<String, Location> locations = DatabaseManager.loadLocations();
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < roads.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            appendRoad(json, roads.get(index), locations);
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private static void sendEquipment(HttpExchange exchange) throws IOException, SQLException {
        String damageType = queryValue(exchange.getRequestURI().getRawQuery(), "damageType");
        ArrayList<String> equipment = DatabaseManager.loadEquipmentCatalog(damageType);
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < equipment.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append('"').append(jsonEscape(equipment.get(index))).append('"');
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private static void sendDatabaseTables(HttpExchange exchange) throws IOException, SQLException {
        StringBuilder json = new StringBuilder();
        appendJsonValue(json, DatabaseManager.loadDatabaseTables());
        sendJson(exchange, 200, json.toString());
    }

    private static void appendJsonValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof String) {
            json.append('"').append(jsonEscape((String) value)).append('"');
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else if (value instanceof Map<?, ?>) {
            json.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append('"').append(jsonEscape(String.valueOf(entry.getKey()))).append("\":");
                appendJsonValue(json, entry.getValue());
            }
            json.append('}');
        } else if (value instanceof Iterable<?>) {
            json.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                appendJsonValue(json, item);
            }
            json.append(']');
        } else {
            json.append('"').append(jsonEscape(String.valueOf(value))).append('"');
        }
    }

    private static void sendReports(HttpExchange exchange, ArrayList<RoadDamage> reports)
            throws IOException {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < reports.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            appendReport(json, reports.get(index));
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private static void sendReport(HttpExchange exchange, int status, RoadDamage report)
            throws IOException {
        StringBuilder json = new StringBuilder();
        appendReport(json, report);
        sendJson(exchange, status, json.toString());
    }

    private static void appendReport(StringBuilder json, RoadDamage report) {
        json.append("{\"id\":\"").append(jsonEscape(report.reportId))
                .append("\",\"roadId\":\"").append(jsonEscape(report.roadId))
                .append("\",\"location\":\"").append(jsonEscape(report.location))
                .append("\",\"from\":\"").append(jsonEscape(report.startLocation))
                .append("\",\"to\":\"").append(jsonEscape(report.endLocation))
                .append("\",\"time\":").append(report.roadTravelTime)
                .append(",\"damage\":\"").append(jsonEscape(report.damageType))
                .append("\",\"severity\":\"").append(DatabaseManager.ratingLabel(report.severity))
                .append("\",\"risk\":\"").append(DatabaseManager.ratingLabel(report.accidentRisk))
                .append("\",\"importance\":\"").append(DatabaseManager.ratingLabel(report.roadImportance))
                .append("\",\"traffic\":").append(report.trafficRank)
                .append(",\"status\":\"").append(displayStatus(report.status))
                .append("\",\"reportOrder\":").append(report.reportOrder)
                .append(",\"assignedTeam\":\"").append(jsonEscape(report.assignedTeam == null ? "Unassigned" : report.assignedTeam))
                .append("\",\"assignedEquipment\":\"").append(jsonEscape(report.assignedEquipment == null ? "" : report.assignedEquipment))
                .append("\",\"submittedAt\":\"").append(jsonEscape(report.submittedAt.toString()))
                .append("\"}");
    }

    private static void sendTeams(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        int index = 0;
        for (MaintenanceTeam team : Main.teams) {
            if (index > 0) json.append(',');

            String activeJobId = "";
            for (RepairJob job : Main.repairJobs) {
                if (job.assignedTeam != null && job.assignedTeam.teamId.equalsIgnoreCase(team.teamId) && "ACTIVE".equalsIgnoreCase(job.status)) {
                    activeJobId = "J-" + job.damage.reportId;
                    break;
                }
            }
            if (activeJobId.isEmpty()) {
                for (RoadDamage report : Main.reports) {
                    if (report.assignedTeam != null && report.assignedTeam.equalsIgnoreCase(team.teamId) && "ACTIVE".equalsIgnoreCase(report.status)) {
                        activeJobId = "J-" + report.reportId;
                        break;
                    }
                }
            }

            boolean isAvailable = activeJobId.isEmpty() && team.available;

            json.append("{\"id\":\"").append(jsonEscape(team.teamId))
                .append("\",\"location\":\"").append(jsonEscape(team.currentLocation))
                .append("\",\"available\":").append(isAvailable)
                .append(",\"activeJob\":\"").append(jsonEscape(activeJobId))
                .append("\",\"equipment\":[");
            int eqIndex = 0;
            for (String eq : team.equipment) {
                if (eqIndex > 0) json.append(',');
                json.append('"').append(jsonEscape(eq)).append('"');
                eqIndex++;
            }
            json.append("]}");
            index++;
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private static void sendJobs(HttpExchange exchange) throws IOException {
        StringBuilder json = new StringBuilder("[");
        int index = 0;
        for (RepairJob job : Main.repairJobs) {
            if (index > 0) json.append(',');
            json.append("{\"id\":\"J-").append(jsonEscape(job.damage.reportId))
                .append("\",\"reportId\":\"").append(jsonEscape(job.damage.reportId))
                .append("\",\"roadId\":\"").append(jsonEscape(job.damage.roadId))
                .append("\",\"location\":\"").append(jsonEscape(job.damage.location))
                .append("\",\"damage\":\"").append(jsonEscape(job.damage.damageType))
                .append("\",\"team\":\"").append(jsonEscape(job.assignedTeam != null ? job.assignedTeam.teamId : "NONE"))
                .append("\",\"equipment\":\"").append(jsonEscape(job.requiredEquipment))
                .append("\",\"status\":\"").append(jsonEscape(job.status))
                .append("\"}");
            index++;
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private static String displayStatus(String status) {
        if (status == null || status.isEmpty()) {
            return "Pending";
        }
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }

    private static void sendRoad(HttpExchange exchange, int status, Road road)
            throws IOException, SQLException {
        LinkedHashMap<String, Location> locations = DatabaseManager.loadLocations();
        StringBuilder json = new StringBuilder();
        appendRoad(json, road, locations);
        sendJson(exchange, status, json.toString());
    }

    private static void appendRoad(StringBuilder json, Road road,
            Map<String, Location> locations) {
        Location fromLocation = lookupLocation(locations, road.from);
        Location toLocation = lookupLocation(locations, road.to);

        json.append("{\"id\":\"").append(jsonEscape(road.roadId))
                .append("\",\"from\":\"").append(jsonEscape(road.from))
                .append("\",\"to\":\"").append(jsonEscape(road.to))
                .append("\",\"time\":").append(road.travelTime)
                .append(",\"closed\":").append(road.closed);
        appendCoordinate(json, "from", fromLocation);
        appendCoordinate(json, "to", toLocation);
        json.append('}');
    }

    private static void appendCoordinate(StringBuilder json, String prefix, Location location) {
        if (location == null) {
            json.append(",\"").append(prefix).append("Lat\":null")
                    .append(",\"").append(prefix).append("Lng\":null");
        } else {
            json.append(",\"").append(prefix).append("Lat\":").append(location.latitude)
                    .append(",\"").append(prefix).append("Lng\":").append(location.longitude);
        }
    }

    /*
     * Case-insensitive lookup so roads added with differently-cased names still
     * plot.
     */
    private static Location lookupLocation(Map<String, Location> locations, String name) {
        if (name == null) {
            return null;
        }
        Location exact = locations.get(name);
        if (exact != null) {
            return exact;
        }
        for (Location location : locations.values()) {
            if (location.name.equalsIgnoreCase(name)) {
                return location;
            }
        }
        return null;
    }

    private static void sendLocations(HttpExchange exchange,
            LinkedHashMap<String, Location> locations) throws IOException {
        StringBuilder json = new StringBuilder("[");
        int index = 0;
        for (Location location : locations.values()) {
            if (index++ > 0) {
                json.append(',');
            }
            appendLocation(json, location);
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private static void appendLocation(StringBuilder json, Location location) {
        json.append("{\"name\":\"").append(jsonEscape(location.name))
                .append("\",\"latitude\":").append(location.latitude)
                .append(",\"longitude\":").append(location.longitude).append('}');
    }

    private static void createEquipment(HttpExchange exchange) throws IOException, SQLException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String damageType = queryValue(body, "damageType");
        String equipmentName = queryValue(body, "equipmentName");

        if (damageType == null || damageType.isBlank() || equipmentName == null || equipmentName.isBlank()) {
            throw new IllegalArgumentException("Damage type and equipment name are required.");
        }

        boolean inserted = DatabaseManager.insertEquipment(damageType, equipmentName);
        if (!inserted) {
            sendJson(exchange, 409, "{\"error\":\"This equipment name already exists for that damage type.\"}");
            return;
        }

        sendJson(exchange, 201, "{\"damageType\":\"" + jsonEscape(damageType.trim()) + "\",\"equipmentName\":\""
                + jsonEscape(equipmentName.trim()) + "\"}");
    }

    private static void deleteEquipment(HttpExchange exchange) throws IOException, SQLException {
        String query = exchange.getRequestURI().getRawQuery();
        String damageType = queryValue(query, "damageType");
        String equipmentName = queryValue(query, "equipmentName");

        if (damageType == null || damageType.isBlank() || equipmentName == null || equipmentName.isBlank()) {
            throw new IllegalArgumentException("Damage type and equipment name are required.");
        }

        boolean removed = DatabaseManager.deleteEquipment(damageType, equipmentName);
        if (!removed) {
            sendJson(exchange, 404, "{\"error\":\"Equipment not found.\"}");
            return;
        }

        sendJson(exchange, 200, "{\"damageType\":\"" + jsonEscape(damageType.trim()) + "\",\"equipmentName\":\""
                + jsonEscape(equipmentName.trim()) + "\"}");
    }

    private static void createLocation(HttpExchange exchange) throws IOException, SQLException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String name = queryValue(body, "name");
        String latitudeValue = queryValue(body, "lat");
        String longitudeValue = queryValue(body, "lng");

        if (name == null || name.isBlank() || latitudeValue == null || longitudeValue == null) {
            throw new IllegalArgumentException("Location name, latitude, and longitude are required.");
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(latitudeValue.trim());
            longitude = Double.parseDouble(longitudeValue.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Latitude and longitude must be numbers.");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordinates are out of range.");
        }

        if (!DatabaseManager.insertLocation(name, latitude, longitude)) {
            sendJson(exchange, 409, "{\"error\":\"That location already exists.\"}");
            return;
        }

        sendJson(exchange, 201, "{\"name\":\"" + jsonEscape(name.trim()) +
                "\",\"latitude\":" + latitude +
                ",\"longitude\":" + longitude + "}");
    }

    private static String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"' || character == '\\') {
                escaped.append('\\').append(character);
            } else if (character < 0x20) {
                escaped.append(String.format("\\u%04x", (int) character));
            } else {
                escaped.append(character);
            }
        }
        return escaped.toString();
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static void sendColoring(HttpExchange exchange) throws IOException, SQLException {
        ArrayList<Road> roads = DatabaseManager.loadRoads();
        LinkedHashMap<String, Location> locations = DatabaseManager.loadLocations();
        routing.GraphColoring.ScheduleResult schedule = routing.GraphColoring.scheduleRoadRepairs(roads);
        routing.GraphColoring.LocationColoringResult locationColors = routing.GraphColoring.colorLocations(roads);

        StringBuilder json = new StringBuilder("{");
        json.append("\"chromaticNumber\":").append(schedule.chromaticNumber);
        json.append(",\"totalRoads\":").append(roads.size());

        json.append(",\"phases\":[");
        int phaseIdx = 0;
        for (Map.Entry<Integer, List<Road>> entry : schedule.phaseGroups.entrySet()) {
            if (phaseIdx > 0) json.append(',');
            int phase = entry.getKey();
            List<Road> phaseRoads = entry.getValue();
            String color = routing.GraphColoring.getColorHex(phase);

            json.append("{\"phase\":").append(phase)
                .append(",\"name\":\"Phase ").append(phase).append("\"")
                .append(",\"colorHex\":\"").append(color).append("\"")
                .append(",\"roadCount\":").append(phaseRoads.size())
                .append(",\"roads\":[");

            for (int r = 0; r < phaseRoads.size(); r++) {
                if (r > 0) json.append(',');
                appendRoad(json, phaseRoads.get(r), locations);
            }
            json.append("]}");
            phaseIdx++;
        }
        json.append(']');

        json.append(",\"roadColors\":{");
        int roadIdx = 0;
        for (Road road : roads) {
            if (roadIdx > 0) json.append(',');
            int phase = schedule.roadToPhase.getOrDefault(road.roadId, 1);
            String color = schedule.roadToColorHex.getOrDefault(road.roadId, "#10b981");
            List<String> conflicts = schedule.conflictAdjacency.getOrDefault(road.roadId, Collections.emptyList());

            json.append("\"").append(jsonEscape(road.roadId)).append("\":{")
                .append("\"phase\":").append(phase)
                .append(",\"colorHex\":\"").append(color).append("\"")
                .append(",\"conflicts\":[");
            for (int c = 0; c < conflicts.size(); c++) {
                if (c > 0) json.append(',');
                json.append('"').append(jsonEscape(conflicts.get(c))).append('"');
            }
            json.append("]}");
            roadIdx++;
        }
        json.append('}');

        json.append(",\"locationChromaticNumber\":").append(locationColors.chromaticNumber);
        json.append(",\"locationZones\":[");
        int locIdx = 0;
        for (Map.Entry<String, Integer> entry : locationColors.locationToZone.entrySet()) {
            if (locIdx > 0) json.append(',');
            json.append("{\"location\":\"").append(jsonEscape(entry.getKey()))
                .append("\",\"zone\":").append(entry.getValue())
                .append(",\"colorHex\":\"").append(locationColors.locationToColorHex.get(entry.getKey())).append("\"}");
            locIdx++;
        }
        json.append(']');

        json.append('}');
        sendJson(exchange, 200, json.toString());
    }

    private static void createTeam(HttpExchange exchange) throws IOException, SQLException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String teamId = queryValue(body, "id");
        if (teamId == null || teamId.isBlank()) {
            teamId = queryValue(body, "teamId");
        }
        String location = queryValue(body, "location");
        String availableValue = queryValue(body, "available");
        String equipment = queryValue(body, "equipment");

        if (teamId == null || teamId.isBlank() || location == null || location.isBlank()) {
            throw new IllegalArgumentException("Team ID and current location are required.");
        }

        teamId = teamId.trim().toUpperCase(Locale.ROOT);
        location = location.trim();
        boolean available = !"false".equalsIgnoreCase(availableValue);

        if (Main.teamMap.containsKey(teamId)) {
            sendJson(exchange, 409, "{\"error\":\"Team " + jsonEscape(teamId) + " is already registered.\"}");
            return;
        }

        MaintenanceTeam team = new MaintenanceTeam(teamId, location);
        team.available = available;
        if (equipment != null && !equipment.isBlank()) {
            for (String eq : equipment.split(",")) {
                String trimmed = eq.trim();
                if (!trimmed.isEmpty()) {
                    team.addEquipment(trimmed);
                }
            }
        }

        Main.teams.add(team);
        Main.teamMap.put(teamId, team);
        DatabaseManager.saveOrUpdateMaintenanceTeam(team);

        // Immediate automated dispatch: If team is available, allot any waiting pending repairs
        if (team.available) {
            RepairManager.autoAllotAllPendingRepairs();
        }

        String activeJob = "";
        for (RepairJob job : Main.repairJobs) {
            if (job.assignedTeam != null && job.assignedTeam.teamId.equalsIgnoreCase(team.teamId) && "ACTIVE".equalsIgnoreCase(job.status)) {
                activeJob = "J-" + job.damage.reportId;
                break;
            }
        }
        if (activeJob.isEmpty()) {
            for (RoadDamage report : Main.reports) {
                if (report.assignedTeam != null && report.assignedTeam.equalsIgnoreCase(team.teamId) && "ACTIVE".equalsIgnoreCase(report.status)) {
                    activeJob = "J-" + report.reportId;
                    break;
                }
            }
        }

        StringBuilder json = new StringBuilder("{");
        json.append("\"id\":\"").append(jsonEscape(team.teamId)).append("\",");
        json.append("\"location\":\"").append(jsonEscape(team.currentLocation)).append("\",");
        json.append("\"available\":").append(activeJob.isEmpty() && team.available).append(",");
        json.append("\"activeJob\":\"").append(jsonEscape(activeJob)).append("\",");
        json.append("\"equipment\":[");
        int eqIdx = 0;
        for (String eq : team.equipment) {
            if (eqIdx > 0) json.append(',');
            json.append('"').append(jsonEscape(eq)).append('"');
            eqIdx++;
        }
        json.append("]}");

        sendJson(exchange, 201, json.toString());
    }

    private static void addTeamEquipment(HttpExchange exchange) throws IOException, SQLException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String teamId = queryValue(body, "teamId");
        if (teamId == null || teamId.isBlank()) teamId = queryValue(body, "id");
        String equipmentName = queryValue(body, "equipmentName");
        if (equipmentName == null || equipmentName.isBlank()) equipmentName = queryValue(body, "equipment");

        if (teamId == null || teamId.isBlank() || equipmentName == null || equipmentName.isBlank()) {
            throw new IllegalArgumentException("Team ID and equipment name are required.");
        }

        teamId = teamId.trim().toUpperCase(Locale.ROOT);
        equipmentName = equipmentName.trim();

        MaintenanceTeam team = Main.teamMap.get(teamId);
        if (team == null) {
            sendJson(exchange, 404, "{\"error\":\"Team " + jsonEscape(teamId) + " not found.\"}");
            return;
        }

        team.addEquipment(equipmentName);
        DatabaseManager.saveOrUpdateMaintenanceTeam(team);
        sendJson(exchange, 200, "{\"success\":true,\"teamId\":\"" + jsonEscape(teamId) + "\",\"equipment\":\"" + jsonEscape(equipmentName) + "\"}");
    }

    private static void removeTeamEquipment(HttpExchange exchange) throws IOException, SQLException {
        String query = exchange.getRequestURI().getRawQuery();
        String teamId = queryValue(query, "teamId");
        if (teamId == null || teamId.isBlank()) teamId = queryValue(query, "id");
        String equipmentName = queryValue(query, "equipment");
        if (equipmentName == null || equipmentName.isBlank()) equipmentName = queryValue(query, "equipmentName");

        if (teamId == null || teamId.isBlank() || equipmentName == null || equipmentName.isBlank()) {
            throw new IllegalArgumentException("Team ID and equipment name are required.");
        }

        teamId = teamId.trim().toUpperCase(Locale.ROOT);
        equipmentName = equipmentName.trim();

        MaintenanceTeam team = Main.teamMap.get(teamId);
        if (team == null) {
            sendJson(exchange, 404, "{\"error\":\"Team " + jsonEscape(teamId) + " not found.\"}");
            return;
        }

        team.removeEquipment(equipmentName);
        DatabaseManager.saveOrUpdateMaintenanceTeam(team);
        sendJson(exchange, 200, "{\"success\":true,\"teamId\":\"" + jsonEscape(teamId) + "\",\"equipment\":\"" + jsonEscape(equipmentName) + "\"}");
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
            return;
        }

        String rawPath = exchange.getRequestURI().getPath();
        if (rawPath == null || rawPath.isEmpty() || "/".equals(rawPath)) {
            rawPath = "/index.html";
        }
        if (rawPath.startsWith("/Front-End/")) {
            rawPath = rawPath.substring("/Front-End".length());
            if (rawPath.isEmpty() || "/".equals(rawPath)) {
                rawPath = "/index.html";
            }
        }

        rawPath = rawPath.replace('\\', '/');
        if (rawPath.contains("..")) {
            sendJson(exchange, 403, "{\"error\":\"Access denied.\"}");
            return;
        }

        String relativePath = rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
        java.io.File file = new java.io.File("Front-End", relativePath);
        if (!file.exists() || file.isDirectory()) {
            file = new java.io.File(relativePath);
        }

        if (!file.exists() || file.isDirectory()) {
            sendJson(exchange, 404, "{\"error\":\"Not found.\"}");
            return;
        }

        String contentType = getContentType(file.getName());
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(method) ? -1 : bytes.length);
        if (!"HEAD".equalsIgnoreCase(method)) {
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
        exchange.close();
    }

    private static String getContentType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css")) return "text/css; charset=utf-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (lower.endsWith(".json")) return "application/json; charset=utf-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}

