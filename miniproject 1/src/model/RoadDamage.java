package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RoadDamage {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");

    public String reportId;
    public String roadId;
    public String location;
    public String startLocation;
    public String endLocation;
    public int roadTravelTime;
    public String damageType;
    public LocalDateTime submittedAt;

    public int severity;
    public int accidentRisk;
    public int roadImportance;
    public int trafficRank;
    public int reportOrder;
    public String status;
    public String assignedTeam;
    public String assignedEquipment;

    public RoadDamage(String reportId, String roadId, String location,
                      String startLocation, String endLocation, int roadTravelTime,
                      String damageType, int severity, int accidentRisk,
                      int roadImportance, int trafficRank, int reportOrder) {

        this(reportId, roadId, location, startLocation, endLocation, roadTravelTime,
            damageType, severity, accidentRisk, roadImportance, trafficRank,
            reportOrder, LocalDateTime.now(), "PENDING", "Unassigned", "");
    }

    public RoadDamage(String reportId, String roadId, String location,
                      String startLocation, String endLocation, int roadTravelTime,
                      String damageType, int severity, int accidentRisk,
                      int roadImportance, int trafficRank, int reportOrder,
                      LocalDateTime submittedAt, String status) {

        this(reportId, roadId, location, startLocation, endLocation, roadTravelTime,
            damageType, severity, accidentRisk, roadImportance, trafficRank,
            reportOrder, submittedAt, status, "Unassigned", "");
    }

    public RoadDamage(String reportId, String roadId, String location,
                      String startLocation, String endLocation, int roadTravelTime,
                      String damageType, int severity, int accidentRisk,
                      int roadImportance, int trafficRank, int reportOrder,
                      LocalDateTime submittedAt, String status,
                      String assignedTeam, String assignedEquipment) {

        this.reportId = reportId;
        this.roadId = roadId;
        this.location = location;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.roadTravelTime = roadTravelTime;
        this.damageType = damageType;
        this.severity = severity;
        this.accidentRisk = accidentRisk;
        this.roadImportance = roadImportance;
        this.trafficRank = trafficRank;
        this.reportOrder = reportOrder;
        this.submittedAt = submittedAt;
        this.status = status;
        this.assignedTeam = (assignedTeam == null || assignedTeam.trim().isEmpty()) ? "Unassigned" : assignedTeam;
        this.assignedEquipment = (assignedEquipment == null) ? "" : assignedEquipment;
    }

    public String getFormattedSubmissionTime() {
        return submittedAt.format(DISPLAY_FORMAT);
    }

    public void updateDamage(int severity, int accidentRisk,
                             int roadImportance, int trafficRank) {
        this.severity = severity;
        this.accidentRisk = accidentRisk;
        this.roadImportance = roadImportance;
        this.trafficRank = trafficRank;
    }
}
