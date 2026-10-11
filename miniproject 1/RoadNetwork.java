import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class RoadNetwork {

    ArrayList<Road> roads;
    HashMap<String, ArrayList<GraphEdge>> graph;

    RoadNetwork() {
        roads = new ArrayList<>();
        graph = new HashMap<>();

        try {
            DatabaseManager.initializeDatabase();
            ArrayList<Road> savedRoads = DatabaseManager.loadRoads();
            for (Road road : savedRoads) {
                addRoadToMemory(road);
            }
            System.out.println("Loaded " + roads.size() + " road(s) from the database.");
        } catch (SQLException exception) {
            System.out.println("Database error while loading roads: " + exception.getMessage());
            System.out.println("Check that the SQLite JDBC driver is added to the project.");
        }
    }

    // Saves the road first, then adds it to the in-memory graph used by Dijkstra.
    boolean addRoad(String roadId, String from, String to, int travelTime) {
        if (roadId == null || roadId.trim().isEmpty() ||
            from == null || from.trim().isEmpty() ||
            to == null || to.trim().isEmpty()) {
            System.out.println("Road ID and both locations are required.");
            return false;
        }
        if (travelTime <= 0) {
            System.out.println("Travel time must be greater than zero.");
            return false;
        }

        try {
            boolean inserted = DatabaseManager.insertRoad(roadId, from, to, travelTime);
            if (!inserted) {
                System.out.println("Road ID already exists. Please use a unique Road ID.");
                return false;
            }
            addRoadToMemory(new Road(roadId.trim(), from.trim(), to.trim(), travelTime));
            return true;
        } catch (SQLException exception) {
            System.out.println("Could not save road to database: " + exception.getMessage());
            return false;
        }
    }

    private void addRoadToMemory(Road road) {
        roads.add(road);
        graph.putIfAbsent(road.from, new ArrayList<>());
        graph.putIfAbsent(road.to, new ArrayList<>());
        graph.get(road.from).add(new GraphEdge(road.to, road.travelTime));
        graph.get(road.to).add(new GraphEdge(road.from, road.travelTime));
    }

    Road findRoadById(String roadId) {
        if (roadId == null) return null;
        for (Road road : roads) {
            if (road.roadId.equalsIgnoreCase(roadId.trim())) {
                return road;
            }
        }
        return null;
    }

    void viewRoads() {
        System.out.println("\n================================");
        System.out.println("          ROAD NETWORK");
        System.out.println("================================");

        if (roads.isEmpty()) {
            System.out.println("No roads added yet. Use Engineering Section > Add Road first.");
            return;
        }

        for (Road road : roads) {
            System.out.println("\nRoad ID      : " + road.roadId);
            System.out.println("From         : " + road.from);
            System.out.println("To           : " + road.to);
            System.out.println("Travel Time  : " + road.travelTime + " minutes");
            System.out.println("Status       : " + (road.closed ? "CLOSED" : "OPEN"));
        }
    }

    void markRoadClosed(String roadId) {
        Road road = findRoadById(roadId);
        if (road == null) {
            System.out.println("Road not found.");
            return;
        }
        try {
            if (DatabaseManager.updateRoadClosed(road.roadId, true)) {
                road.closeRoad();
                System.out.println("Road " + road.roadId + " marked as CLOSED.");
            } else {
                System.out.println("Road was not found in the database.");
            }
        } catch (SQLException exception) {
            System.out.println("Could not update road status: " + exception.getMessage());
        }
    }

    void markRoadOpen(String roadId) {
        Road road = findRoadById(roadId);
        if (road == null) {
            System.out.println("Road not found.");
            return;
        }
        try {
            if (DatabaseManager.updateRoadClosed(road.roadId, false)) {
                road.openRoad();
                System.out.println("Road " + road.roadId + " marked as OPEN.");
            } else {
                System.out.println("Road was not found in the database.");
            }
        } catch (SQLException exception) {
            System.out.println("Could not update road status: " + exception.getMessage());
        }
    }
}
