import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** Serves the persisted road network to the browser frontend. */
public final class RoadApiServer {

    private RoadApiServer() { }

    public static void start(int port) {
        try {
            HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", port), 0
            );
            server.createContext("/api/", RoadApiServer::handleRoads);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("Road API ready at http://localhost:" + port + "/api/roads");
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

            String[] pathParts = path.split("/");
            if (
                "PATCH".equalsIgnoreCase(method) &&
                pathParts.length == 5 &&
                "api".equals(pathParts[1]) &&
                "roads".equals(pathParts[2]) &&
                "status".equals(pathParts[4])
            ) {
                updateRoadStatus(exchange, pathParts[3]);
                return;
            }

            if (
                "PATCH".equalsIgnoreCase(method) &&
                pathParts.length == 5 &&
                "api".equals(pathParts[1]) &&
                "reports".equals(pathParts[2]) &&
                "status".equals(pathParts[4])
            ) {
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
            roadId, damageType, severity, risk, importance, trafficRank
        );
        Main.addReportToMemory(report);
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
            .append(",\"submittedAt\":\"").append(jsonEscape(report.submittedAt.toString()))
            .append("\"}");
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

    /* Case-insensitive lookup so roads added with differently-cased names still plot. */
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

        sendJson(exchange, 201, "{\"damageType\":\"" + jsonEscape(damageType.trim()) + "\",\"equipmentName\":\"" + jsonEscape(equipmentName.trim()) + "\"}");
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

        sendJson(exchange, 200, "{\"damageType\":\"" + jsonEscape(damageType.trim()) + "\",\"equipmentName\":\"" + jsonEscape(equipmentName.trim()) + "\"}");
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
}