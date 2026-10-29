package core;

import api.RoadApiServer;
import cli.ConsoleHelper;
import cli.EngineeringSection;
import cli.WardOfficerSection;
import db.DatabaseManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import model.MaintenanceTeam;
import model.RepairJob;
import model.RoadDamage;
import routing.RoadNetwork;
import service.RepairManager;
import service.ReportManager;
import service.TeamManager;

public class Main {

    public static Scanner sc = new Scanner(System.in);

    // Stores all road damage reports
    public static ArrayList<RoadDamage> reports = new ArrayList<>();

    // Stores all maintenance teams
    public static ArrayList<MaintenanceTeam> teams = new ArrayList<>();

    // Stores road damage reports using Report ID
    public static HashMap<String, RoadDamage> reportMap = new HashMap<>();

    // Stores maintenance teams using Team ID
    public static HashMap<String, MaintenanceTeam> teamMap = new HashMap<>();

    // Road network
    public static RoadNetwork roadNetwork = new RoadNetwork();

    // Stores repair jobs
    public static ArrayList<RepairJob> repairJobs = new ArrayList<>();

    // Automatically gives each report an order
    public static int reportOrder = 1;

    static {
        try {
            DatabaseManager.initializeDatabase();
            for (RoadDamage report : DatabaseManager.loadDamageReports()) {
                addReportToMemory(report);
            }
            for (MaintenanceTeam team : DatabaseManager.loadMaintenanceTeams()) {
                teams.add(team);
                teamMap.put(team.teamId, team);
            }
        } catch (SQLException exception) {
            System.err.println("Could not load database records: " + exception.getMessage());
        }

        if (teams.isEmpty()) {
            initializeDefaultTeams();
        }

        // Reconstruct active repair jobs from persisted active reports
        for (RoadDamage report : reports) {
            if ("ACTIVE".equalsIgnoreCase(report.status) && report.assignedTeam != null && !"Unassigned".equalsIgnoreCase(report.assignedTeam)) {
                MaintenanceTeam team = teamMap.get(report.assignedTeam);
                if (team != null) {
                    RepairJob job = new RepairJob(report);
                    job.assignTeam(team);
                    job.requiredEquipment = report.assignedEquipment;
                    repairJobs.add(job);
                }
            }
        }
    }

    public static void initializeDefaultTeams() {
        registerTeamInternal("T1", "City Hospital", "Pothole Kit", "Road Cutter");
        registerTeamInternal("T2", "Market Junction", "Resurfacing Machine");
        registerTeamInternal("T3", "Sports Ground", "Road Cutter", "Pothole Kit");
        registerTeamInternal("T4", "Bus Stand", "General Equipment", "Pothole Kit");
    }

    private static void registerTeamInternal(String teamId, String location, String... equipment) {
        MaintenanceTeam team = new MaintenanceTeam(teamId, location);
        for (String eq : equipment) {
            team.addEquipment(eq);
        }
        teams.add(team);
        teamMap.put(teamId, team);
        try {
            DatabaseManager.saveOrUpdateMaintenanceTeam(team);
        } catch (SQLException ignored) { }
    }

    // ==========================================
    // MAIN METHOD
    // ==========================================

    public static void main(String[] args) {
        RoadApiServer.start(8080);
           
        while (true) {

            System.out.println("\n==========================================");
            System.out.println(" SMART MUNICIPAL ROAD MAINTENANCE SYSTEM");
            System.out.println("          SENTHAMIZH NAGAR");
            System.out.println("==========================================");

            System.out.println("1. Engineering Section");
            System.out.println("2. Ward Officer");
            System.out.println("3. Exit");

            System.out.print("Enter your choice: ");
            int choice = ConsoleHelper.getMenuChoice();
            
            switch (choice) {

                case 1:
                    engineeringMenu();
                    break;

                case 2:
                    wardOfficerMenu();
                    break;

                case 3:
                    System.out.println("\nApplication closed.");
                    sc.close();
                    return;

                default:
                    System.out.println("\nInvalid choice.");
            }
        }
    }

    public static synchronized void addReportToMemory(RoadDamage report) {
        if (!reportMap.containsKey(report.reportId)) {
            reports.add(report);
            reportMap.put(report.reportId, report);
        }
        reportOrder = Math.max(reportOrder, report.reportOrder + 1);
    }

    // ==========================================
    // DELEGATE METHODS (PRESERVES 100% COMPATIBILITY)
    // ==========================================

    public static void engineeringMenu() {
        EngineeringSection.showMenu();
    }

    public static void wardOfficerMenu() {
        WardOfficerSection.showMenu();
    }

    public static void submitReport() {
        ReportManager.submitReport();
    }

    public static void viewReports() {
        ReportManager.viewReports();
    }

    public static int readRating(String prompt) {
        return ConsoleHelper.readRating(prompt);
    }

    public static int getMenuChoice() {
        return ConsoleHelper.getMenuChoice();
    }

    public static void registerMaintenanceTeam() {
        TeamManager.registerMaintenanceTeam();
    }

    public static void addEquipmentToTeam() {
        TeamManager.addEquipmentToTeam();
    }

    public static void removeEquipmentFromTeam() {
        TeamManager.removeEquipmentFromTeam();
    }

    public static void removeEquipment() {
        TeamManager.removeEquipment();
    }

    public static MaintenanceTeam findTeam(String teamId) {
        return TeamManager.findTeam(teamId);
    }

    public static void viewTeams() {
        TeamManager.viewTeams();
    }

    public static void setTeamAvailability() {
        TeamManager.setTeamAvailability();
    }

    public static void addRoad() {
        EngineeringSection.addRoad();
    }

    public static void markRoadClosure() {
        EngineeringSection.markRoadClosure();
    }

    public static void processPendingRepairs() {
        RepairManager.processPendingRepairs();
    }

    public static MaintenanceTeam findCompatibleTeam(RoadDamage report) {
        return RepairManager.findCompatibleTeam(report);
    }

    public static void updateRepairCompletion() {
        RepairManager.updateRepairCompletion();
    }

    public static void updateDamageReport() {
        RepairManager.updateDamageReport();
    }

    public static void viewRepairJobs() {
        RepairManager.viewRepairJobs();
    }

    public static void testDijkstra() {
        EngineeringSection.testDijkstra();
    }
}
