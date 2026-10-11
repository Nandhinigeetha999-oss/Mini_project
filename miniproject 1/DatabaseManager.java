import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;

/** Handles permanent storage of the road network and damage reports in SQLite. */
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
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS damage_reports (" +
                "report_id TEXT PRIMARY KEY COLLATE NOCASE, " +
                "road_id TEXT NOT NULL, " +
                "location TEXT NOT NULL, " +
                "start_location TEXT NOT NULL, " +
                "end_location TEXT NOT NULL, " +
                "road_travel_time INTEGER NOT NULL, " +
                "damage_type TEXT NOT NULL, " +
                "severity INTEGER NOT NULL, " +
                "accident_risk INTEGER NOT NULL, " +
                "road_importance INTEGER NOT NULL, " +
                "traffic_rank INTEGER NOT NULL, " +
                "report_order INTEGER NOT NULL UNIQUE, " +
                "status TEXT NOT NULL DEFAULT 'PENDING', " +
                "submitted_at TEXT NOT NULL)"
            );
            seedRoads(connection);
        }
    }

    private static void seedRoads(Connection connection) throws SQLException {
        String[][] roads = {
            {"R001", "Municipal Office", "Market Junction", "4"},
            {"R002", "Market Junction", "Government School", "3"},
            {"R003", "Government School", "City Hospital", "5"},
            {"R004", "City Hospital", "Bus Stand", "4"},
            {"R005", "Bus Stand", "Railway Station", "6"},
            {"R006", "Market Junction", "Main Park", "4"},
            {"R007", "Main Park", "Sports Ground", "3"},
            {"R008", "Sports Ground", "Water Tank", "5"},
            {"R009", "Water Tank", "Industrial Estate", "6"},
            {"R010", "Industrial Estate", "Bypass Junction", "5"},
            {"R011", "Municipal Office", "Post Office", "3"},
            {"R012", "Post Office", "Library", "2"},
            {"R013", "Library", "Government School", "3"},
            {"R014", "Post Office", "Bus Stand", "5"},
            {"R015", "City Hospital", "Fire Station", "3"},
            {"R016", "Fire Station", "Police Station", "2"},
            {"R017", "Police Station", "Bypass Junction", "6"},
            {"R018", "Railway Station", "Bypass Junction", "7"},
            {"R019", "Water Tank", "Fire Station", "7"}
        };
        String sql = "INSERT OR IGNORE INTO roads " +
            "(road_id, start_location, end_location, travel_time, is_closed) " +
            "VALUES (?, ?, ?, ?, 0)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (String[] road : roads) {
                preparedStatement.setString(1, road[0]);
                preparedStatement.setString(2, road[1]);
                preparedStatement.setString(3, road[2]);
                preparedStatement.setInt(4, Integer.parseInt(road[3]));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
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

    public static synchronized RoadDamage insertDamageReport(
            String roadId, String damageType, String severity, String accidentRisk,
            String roadImportance, int trafficRank) throws SQLException {
        if (roadId == null || roadId.isBlank() || damageType == null || damageType.isBlank()) {
            throw new IllegalArgumentException("Road and damage type are required.");
        }
        if (trafficRank < 1 || trafficRank > 4) {
            throw new IllegalArgumentException("Traffic rank must be between 1 and 4.");
        }

        int severityRating = ratingFromLabel(severity);
        int riskRating = ratingFromLabel(accidentRisk);
        int importanceRating = ratingFromLabel(roadImportance);

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            String startLocation;
            String endLocation;
            int roadTravelTime;
            try (PreparedStatement roadStatement = connection.prepareStatement(
                    "SELECT start_location, end_location, travel_time FROM roads " +
                    "WHERE road_id = ? COLLATE NOCASE")) {
                roadStatement.setString(1, roadId.trim());
                try (ResultSet roadResult = roadStatement.executeQuery()) {
                    if (!roadResult.next()) {
                        throw new IllegalArgumentException("Selected road does not exist.");
                    }
                    startLocation = roadResult.getString("start_location");
                    endLocation = roadResult.getString("end_location");
                    roadTravelTime = roadResult.getInt("travel_time");
                }
            }

            int reportOrder;
            try (Statement orderStatement = connection.createStatement();
                 ResultSet orderResult = orderStatement.executeQuery(
                     "SELECT COALESCE(MAX(report_order), 0) + 1 FROM damage_reports")) {
                reportOrder = orderResult.next() ? orderResult.getInt(1) : 1;
            }

            RoadDamage report = new RoadDamage(
                String.format("DR%03d", reportOrder), roadId.trim(),
                startLocation + " - " + endLocation, startLocation, endLocation,
                roadTravelTime, damageType.trim(), severityRating, riskRating,
                importanceRating, trafficRank, reportOrder
            );
            String sql = "INSERT INTO damage_reports (" +
                "report_id, road_id, location, start_location, end_location, road_travel_time, " +
                "damage_type, severity, accident_risk, road_importance, traffic_rank, " +
                "report_order, status, submitted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, report.reportId);
                statement.setString(2, report.roadId);
                statement.setString(3, report.location);
                statement.setString(4, report.startLocation);
                statement.setString(5, report.endLocation);
                statement.setInt(6, report.roadTravelTime);
                statement.setString(7, report.damageType);
                statement.setInt(8, report.severity);
                statement.setInt(9, report.accidentRisk);
                statement.setInt(10, report.roadImportance);
                statement.setInt(11, report.trafficRank);
                statement.setInt(12, report.reportOrder);
                statement.setString(13, report.status);
                statement.setString(14, report.submittedAt.toString());
                statement.executeUpdate();
            }
            connection.commit();
            return report;
        }
    }

    public static ArrayList<RoadDamage> loadDamageReports() throws SQLException {
        ArrayList<RoadDamage> reports = new ArrayList<>();
        String sql = "SELECT report_id, road_id, location, start_location, end_location, " +
            "road_travel_time, damage_type, severity, accident_risk, road_importance, " +
            "traffic_rank, report_order, status, submitted_at " +
            "FROM damage_reports ORDER BY report_order";

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                reports.add(new RoadDamage(
                    result.getString("report_id"), result.getString("road_id"),
                    result.getString("location"), result.getString("start_location"),
                    result.getString("end_location"), result.getInt("road_travel_time"),
                    result.getString("damage_type"), result.getInt("severity"),
                    result.getInt("accident_risk"), result.getInt("road_importance"),
                    result.getInt("traffic_rank"), result.getInt("report_order"),
                    LocalDateTime.parse(result.getString("submitted_at")),
                    result.getString("status")
                ));
            }
        }
        return reports;
    }

    public static boolean updateDamageReportStatus(String reportId, String status)
            throws SQLException {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!normalizedStatus.equals("PENDING") && !normalizedStatus.equals("ACTIVE") &&
            !normalizedStatus.equals("COMPLETED")) {
            throw new IllegalArgumentException("Status must be Pending, Active, or Completed.");
        }

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE damage_reports SET status = ? WHERE report_id = ? COLLATE NOCASE")) {
            statement.setString(1, normalizedStatus);
            statement.setString(2, reportId == null ? "" : reportId.trim());
            return statement.executeUpdate() > 0;
        }
    }

    public static int ratingFromLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Severity, risk, and importance are required.");
        }
        switch (label.trim().toLowerCase(Locale.ROOT)) {
            case "low": return 1;
            case "medium": return 2;
            case "high": return 3;
            case "critical": return 4;
            default: throw new IllegalArgumentException("Unknown rating: " + label);
        }
    }

    public static String ratingLabel(int rating) {
        switch (rating) {
            case 1: return "Low";
            case 2: return "Medium";
            case 3: return "High";
            case 4: return "Critical";
            default: return "Unknown";
        }
    }
}
