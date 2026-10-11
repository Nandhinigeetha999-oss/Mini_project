import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

/** Serves the persisted road network to the browser frontend. */
public final class RoadApiServer {

    private RoadApiServer() { }

    public static void start(int port) {
        try {
            HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", port), 0
            );
            server.createContext("/api/roads", RoadApiServer::handleRoads);
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

            sendJson(exchange, 404, "{\"error\":\"API endpoint not found.\"}");
        } catch (SQLException exception) {
            sendJson(exchange, 500, "{\"error\":\"Database error: " + jsonEscape(exception.getMessage()) + "\"}");
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, "{\"error\":\"" + jsonEscape(exception.getMessage()) + "\"}");
        }
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

    private static void sendRoads(HttpExchange exchange, ArrayList<Road> roads) throws IOException {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < roads.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            appendRoad(json, roads.get(index));
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private static void sendRoad(HttpExchange exchange, int status, Road road) throws IOException {
        StringBuilder json = new StringBuilder();
        appendRoad(json, road);
        sendJson(exchange, status, json.toString());
    }

    private static void appendRoad(StringBuilder json, Road road) {
        json.append("{\"id\":\"").append(jsonEscape(road.roadId))
            .append("\",\"from\":\"").append(jsonEscape(road.from))
            .append("\",\"to\":\"").append(jsonEscape(road.to))
            .append("\",\"time\":").append(road.travelTime)
            .append(",\"closed\":").append(road.closed).append('}');
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, PATCH, OPTIONS");
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