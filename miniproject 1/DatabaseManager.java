import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/** Handles permanent storage of the municipal road network in SQLite. */
public class DatabaseManager {

    private static final String DATABASE_URL = "jdbc:sqlite:smart_road_maintenance.db";

    private DatabaseManager() { }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL);
    }

    public static void initializeDatabase() throws SQLException {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS roads (" +
                "road_id TEXT PRIMARY KEY COLLATE NOCASE, " +
                "start_location TEXT NOT NULL, " +
                "end_location TEXT NOT NULL, " +
                "travel_time INTEGER NOT NULL CHECK (travel_time > 0), " +
                "is_closed INTEGER NOT NULL DEFAULT 0)"
            );
        }
    }

    public static ArrayList<Road> loadRoads() throws SQLException {
        ArrayList<Road> loadedRoads = new ArrayList<>();
        String sql = "SELECT road_id, start_location, end_location, travel_time, is_closed FROM roads ORDER BY road_id";

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                Road road = new Road(
                    result.getString("road_id"),
                    result.getString("start_location"),
                    result.getString("end_location"),
                    result.getInt("travel_time")
                );
                if (result.getInt("is_closed") == 1) {
                    road.closeRoad();
                }
                loadedRoads.add(road);
            }
        }
        return loadedRoads;
    }

    public static boolean insertRoad(String roadId, String from, String to, int travelTime)
            throws SQLException {
        String sql = "INSERT INTO roads (road_id, start_location, end_location, travel_time, is_closed) VALUES (?, ?, ?, ?, 0)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roadId.trim());
            statement.setString(2, from.trim());
            statement.setString(3, to.trim());
            statement.setInt(4, travelTime);
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            // SQLite error code 19 is a constraint violation (e.g. duplicate Road ID).
            if (exception.getErrorCode() == 19) {
                return false;
            }
            throw exception;
        }
    }

    public static boolean updateRoadClosed(String roadId, boolean closed) throws SQLException {
        String sql = "UPDATE roads SET is_closed = ? WHERE road_id = ? COLLATE NOCASE";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, closed ? 1 : 0);
            statement.setString(2, roadId.trim());
            return statement.executeUpdate() > 0;
        }
    }
}
