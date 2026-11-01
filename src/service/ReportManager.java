package service;

import cli.ConsoleHelper;
import core.Main;
import db.DatabaseManager;
import java.sql.SQLException;
import model.Road;
import model.RoadDamage;

/**
 * Handles damage report submission and listing.
 */
public class ReportManager {

    public static void submitReport() {
        Main.sc.nextLine(); // Clear the newline left by the previous menu choice.

        System.out.println("\n==========================================");
        System.out.println("        SUBMIT ROAD DAMAGE REPORT");
        System.out.println("==========================================");

        if (Main.roadNetwork.roads.isEmpty()) {
            System.out.println("No roads are registered yet.");
            System.out.println("Ask the Engineering Section to add roads first.");
            return;
        }

        Main.roadNetwork.viewRoads();
        System.out.print("\nEnter Road ID: ");
        String roadId = Main.sc.nextLine().trim();

        Road selectedRoad = Main.roadNetwork.findRoadById(roadId);
        if (selectedRoad == null) {
            System.out.println("Road ID not found. Please select an existing Road ID.");
            return;
        }

        System.out.println("\nRoad details fetched from the stored road network:");
        System.out.println("Road ID       : " + selectedRoad.roadId);
        System.out.println("Starting Point: " + selectedRoad.from);
        System.out.println("Ending Point  : " + selectedRoad.to);
        System.out.println("Travel Time   : " + selectedRoad.travelTime + " minutes");

        System.out.print("\nEnter Damage Type: ");
        String damageType = Main.sc.nextLine().trim();
        if (damageType.isEmpty()) {
            System.out.println("Damage type cannot be empty.");
            return;
        }

        int severity = ConsoleHelper.readRating("Enter Severity (1-3): ");
        int accidentRisk = ConsoleHelper.readRating("Enter Accident Risk (1-3): ");
        int roadImportance = ConsoleHelper.readRating("Enter Road Importance (1-3): ");
        int trafficRank = ConsoleHelper.readRating("Enter Traffic Rank (1-3): ");

        RoadDamage report;
        try {
            report = DatabaseManager.insertDamageReport(
                selectedRoad.roadId, damageType,
                DatabaseManager.ratingLabel(severity),
                DatabaseManager.ratingLabel(accidentRisk),
                DatabaseManager.ratingLabel(roadImportance), trafficRank
            );
            Main.addReportToMemory(report);
        } catch (SQLException | IllegalArgumentException exception) {
            System.out.println("Could not save report: " + exception.getMessage());
            return;
        }

        // AUTOMATICALLY ALLOT MAINTENANCE TEAM AND REQUIRED TOOLS
        model.RepairJob allottedJob = RepairManager.autoAllotTeamAndTools(report);

        System.out.println("\n------------------------------------------");
        System.out.println("Road damage report submitted successfully.");
        System.out.println("Report ID       : " + report.reportId);
        System.out.println("Road ID         : " + report.roadId);
        System.out.println("Location        : " + report.location);
        System.out.println("Starting Point  : " + report.startLocation);
        System.out.println("Ending Point    : " + report.endLocation);
        System.out.println("Road Travel Time: " + report.roadTravelTime + " minutes");
        System.out.println("Damage Type     : " + report.damageType);
        System.out.println("Submitted On    : " + report.getFormattedSubmissionTime());
        System.out.println("Status          : " + report.status);

        if (allottedJob != null && allottedJob.assignedTeam != null) {
            System.out.println("\n--- AUTOMATIC MAINTENANCE ALLOTMENT ---");
            System.out.println("Allotted Team   : Team " + allottedJob.assignedTeam.teamId
                    + " (Base Location: " + allottedJob.assignedTeam.currentLocation + ")");
            System.out.println("Allotted Tools  : " + allottedJob.requiredEquipment);
            System.out.println("Repair Status   : " + report.status);
            System.out.println("------------------------------------------");
        } else {
            System.out.println("\n[Note] All maintenance teams are currently busy. Report is queued for automatic allotment.");
            System.out.println("------------------------------------------");
        }
    }

    public static void viewReports() {
        System.out.println("\n==========================================");
        System.out.println("           ROAD DAMAGE REPORTS");
        System.out.println("==========================================");

        if (Main.reports.isEmpty()) {
            System.out.println("No road damage reports available.");
            return;
        }

        for (RoadDamage report : Main.reports) {
            System.out.println("\n------------------------------------------");
            System.out.println("Report ID       : " + report.reportId);
            System.out.println("Road ID         : " + report.roadId);
            System.out.println("Location        : " + report.location);
            System.out.println("Starting Point  : " + report.startLocation);
            System.out.println("Ending Point    : " + report.endLocation);
            System.out.println("Road Travel Time: " + report.roadTravelTime + " minutes");
            System.out.println("Damage Type     : " + report.damageType);
            System.out.println("Submitted On    : " + report.getFormattedSubmissionTime());
            System.out.println("Severity        : " + report.severity);
            System.out.println("Accident Risk   : " + report.accidentRisk);
            System.out.println("Road Importance : " + report.roadImportance);
            System.out.println("Traffic Rank    : " + report.trafficRank);
            System.out.println("Report Order    : " + report.reportOrder);
            System.out.println("Status          : " + report.status);
            System.out.println("Allotted Team   : " + (report.assignedTeam == null ? "Unassigned" : report.assignedTeam));
            if (report.assignedEquipment != null && !report.assignedEquipment.isEmpty()) {
                System.out.println("Allotted Tools  : " + report.assignedEquipment);
            }
        }

        System.out.println("------------------------------------------");
    }
}
