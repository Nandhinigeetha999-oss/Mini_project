import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RoadDamage {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a");

    String reportId;
    String roadId;
    String location;
    String startLocation;
    String endLocation;
    int roadTravelTime;
    String damageType;
    LocalDateTime submittedAt;

    int severity;
    int accidentRisk;
    int roadImportance;
    int trafficRank;
    int reportOrder;
    String status;

    RoadDamage(String reportId, String roadId, String location,
               String startLocation, String endLocation, int roadTravelTime,
               String damageType, int severity, int accidentRisk,
               int roadImportance, int trafficRank, int reportOrder) {

        this(reportId, roadId, location, startLocation, endLocation, roadTravelTime,
            damageType, severity, accidentRisk, roadImportance, trafficRank,
            reportOrder, LocalDateTime.now(), "PENDING");
        }

        RoadDamage(String reportId, String roadId, String location,
               String startLocation, String endLocation, int roadTravelTime,
               String damageType, int severity, int accidentRisk,
               int roadImportance, int trafficRank, int reportOrder,
               LocalDateTime submittedAt, String status) {

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
    }

    String getFormattedSubmissionTime() {
        return submittedAt.format(DISPLAY_FORMAT);
    }

    void updateDamage(int severity, int accidentRisk,
                      int roadImportance, int trafficRank) {
        this.severity = severity;
        this.accidentRisk = accidentRisk;
        this.roadImportance = roadImportance;
        this.trafficRank = trafficRank;
    }
}
