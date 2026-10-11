import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
            statement.executeUpdate(
                "DELETE FROM damage_reports WHERE rowid NOT IN (" +
                "SELECT MIN(rowid) FROM damage_reports GROUP BY road_id, start_location, " +
                "end_location, damage_type, severity, accident_risk, road_importance, traffic_rank)"
            );
            statement.executeUpdate(
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_damage_reports_duplicate_check " +
                "ON damage_reports (road_id, start_location, end_location, damage_type, " +
                "severity, accident_risk, road_importance, traffic_rank)"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS equipment_catalog (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "damage_type TEXT NOT NULL, " +
                "equipment_name TEXT NOT NULL, " +
                "UNIQUE(damage_type, equipment_name))"
            );
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS locations (" +
                "name TEXT PRIMARY KEY COLLATE NOCASE, " +
                "latitude REAL NOT NULL, " +
                "longitude REAL NOT NULL)"
            );
            seedRoads(connection);
            seedLocations(connection);
            seedEquipmentCatalog(connection);
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

    /**
     * Coordinates for every named municipal place. The map only ever plots the
     * latitudes/longitudes stored here and served by the API, so the browser
     * never has to invent a position for a location.
     */
    private static void seedLocations(Connection connection) throws SQLException {
        Object[][] locations = {
            {"Municipal Office", 13.075529, 80.258186},
            {"Market Junction", 13.083090, 80.258186},
            {"Government School", 13.081383, 80.263886},
            {"City Hospital", 13.081139, 80.271257},
            {"Bus Stand", 13.075529, 80.270529},
            {"Railway Station", 13.075529, 80.278714},
            {"Main Park", 13.089871, 80.259043},
            {"Sports Ground", 13.089871, 80.266800},
            {"Water Tank", 13.089871, 80.275414},
            {"Industrial Estate", 13.089871, 80.283214},
            {"Bypass Junction", 13.082798, 80.282486},
            {"Post Office", 13.075529, 80.261829},
            {"Library", 13.075529, 80.264271},
            {"Fire Station", 13.085529, 80.277429},
            {"Police Station", 13.085920, 80.283214}
        };
        String sql = "INSERT OR IGNORE INTO locations (name, latitude, longitude) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (Object[] location : locations) {
                preparedStatement.setString(1, (String) location[0]);
                preparedStatement.setDouble(2, (Double) location[1]);
                preparedStatement.setDouble(3, (Double) location[2]);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private static void seedEquipmentCatalog(Connection connection) throws SQLException {
        Object[][] equipment = {
            {"Pothole", "Pothole Patch Kit"},
            {"Pothole", "Asphalt Tamper"},
            {"Pothole", "Road Roller"},
            {"Pothole", "Cold Mix Recycler"},
            {"Pothole", "Compaction Plate"},
            {"Pothole", "Surface Leveler"},
            {"Road Crack", "Road Cutter"},
            {"Road Crack", "Crack Sealing Machine"},
            {"Road Crack", "Jackhammer"},
            {"Road Crack", "Sealing Gun"},
            {"Road Crack", "Surface Cleaner"},
            {"Road Crack", "Joint Filler"},
            {"Resurfacing", "Resurfacing Machine"},
            {"Resurfacing", "Milling Machine"},
            {"Resurfacing", "Asphalt Finisher"},
            {"Resurfacing", "Bitumen Sprayer"},
            {"Resurfacing", "Road Sweeper"},
            {"Resurfacing", "Curb Cutter"},
            {"General", "Traffic Cone"},
            {"General", "Safety Sign"},
            {"General", "Inspection Ladder"},
            {"General", "Traffic Barricade"},
            {"General", "Portable Light Tower"},
            {"General", "Safety Barrier"}
        };
        String sql = "INSERT OR IGNORE INTO equipment_catalog (damage_type, equipment_name) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (Object[] item : equipment) {
                preparedStatement.setString(1, (String) item[0]);
                preparedStatement.setString(2, (String) item[1]);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    public static ArrayList<String> loadEquipmentCatalog(String damageType) throws SQLException {
        ArrayList<String> equipment = new ArrayList<>();
        String sql = "SELECT equipment_name FROM equipment_catalog " +
            "WHERE (? IS NULL OR ? = '' OR damage_type = ? COLLATE NOCASE) " +
            "ORDER BY equipment_name";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String normalizedType = damageType == null ? "" : damageType.trim();
            statement.setString(1, normalizedType);
            statement.setString(2, normalizedType);
            statement.setString(3, normalizedType);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    equipment.add(result.getString("equipment_name"));
                }
            }
        }
        return equipment;
    }

    public static boolean insertEquipment(String damageType, String equipmentName) throws SQLException {
        if (damageType == null || damageType.isBlank() || equipmentName == null || equipmentName.isBlank()) {
            throw new IllegalArgumentException("Damage type and equipment name are required.");
        }

        String sql = "INSERT OR IGNORE INTO equipment_catalog (damage_type, equipment_name) VALUES (?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, damageType.trim());
            statement.setString(2, equipmentName.trim());
            return statement.executeUpdate() > 0;
        }
    }

    public static boolean deleteEquipment(String damageType, String equipmentName) throws SQLException {
        if (damageType == null || damageType.isBlank() || equipmentName == null || equipmentName.isBlank()) {
            throw new IllegalArgumentException("Damage type and equipment name are required.");
        }

        String sql = "DELETE FROM equipment_catalog WHERE damage_type = ? COLLATE NOCASE AND equipment_name = ? COLLATE NOCASE";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, damageType.trim());
            statement.setString(2, equipmentName.trim());
            return statement.executeUpdate() > 0;
        }
    }

    public static LinkedHashMap<String, Location> loadLocations() throws SQLException {
        LinkedHashMap<String, Location> locations = new LinkedHashMap<>();
        String sql = "SELECT name, latitude, longitude FROM locations ORDER BY name";

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                locations.put(result.getString("name"), new Location(
                    result.getString("name"),
                    result.getDouble("latitude"),
                    result.getDouble("longitude")
                ));
            }
        }
        return locations;
    }

    public static Location findLocation(String name) throws SQLException {
        if (name == null || name.isBlank()) {
            return null;
        }
        String sql = "SELECT name, latitude, longitude FROM locations WHERE name = ? COLLATE NOCASE";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new Location(
                        result.getString("name"),
                        result.getDouble("latitude"),
                        result.getDouble("longitude")
                    );
                }
            }
        }
        return null;
    }

    public static boolean insertLocation(String name, double latitude, double longitude)
            throws SQLException {
        String sql = "INSERT INTO locations (name, latitude, longitude) VALUES (?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            statement.setDouble(2, latitude);
            statement.setDouble(3, longitude);
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 19) {
                return false;
            }
            throw exception;
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

            String duplicateCheckSql = "SELECT 1 FROM damage_reports WHERE road_id = ? COLLATE NOCASE " +
                "AND start_location = ? AND end_location = ? AND damage_type = ? COLLATE NOCASE " +
                "AND severity = ? AND accident_risk = ? AND road_importance = ? AND traffic_rank = ? LIMIT 1";
            try (PreparedStatement duplicateCheck = connection.prepareStatement(duplicateCheckSql)) {
                duplicateCheck.setString(1, roadId.trim());
                duplicateCheck.setString(2, startLocation);
                duplicateCheck.setString(3, endLocation);
                duplicateCheck.setString(4, damageType.trim());
                duplicateCheck.setInt(5, severityRating);
                duplicateCheck.setInt(6, riskRating);
                duplicateCheck.setInt(7, importanceRating);
                duplicateCheck.setInt(8, trafficRank);
                try (ResultSet duplicateResult = duplicateCheck.executeQuery()) {
                    if (duplicateResult.next()) {
                        connection.rollback();
                        throw new IllegalArgumentException("This exact road damage report has already been submitted.");
                    }
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
